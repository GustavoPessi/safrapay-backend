package com.safrapay.backend.chat;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.safrapay.backend.chat.dto.ChatMensagemRequest;
import com.safrapay.backend.chat.dto.ChatMensagemResponse;
import com.safrapay.backend.chat.dto.ChatMensagemResponse.LancamentoProposto;
import com.safrapay.backend.security.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Assistente de IA dentro do app: texto, foto de nota (etapa 2) e arquivo
 * (etapa 3) viram propostas de lançamento; perguntas viram CONSULTA (contrato
 * v2, 01/10/2026 — "a IA entende, o app calcula").
 *
 * Recebe a conversa + o contexto do usuário (sem valores financeiros), pede ao
 * Claude que identifique despesas/receitas ou a pergunta e devolve tudo
 * estruturado. Não grava nada no banco: quem grava é o app, depois que o
 * produtor confirma; e quem calcula as respostas das perguntas é o app, no Room.
 *
 * Tudo que vem da IA é revalidado aqui (uuid tem que existir no contexto,
 * categoria tem que estar na lista, data válida, valor positivo) e os
 * "camposFaltando" são recalculados no servidor — o app pode confiar neles.
 */
@Service
public class ChatService {

    private static final Logger log = LoggerFactory.getLogger(ChatService.class);

    static final String PROPOSTA = "PROPOSTA";
    static final String PRECISA_INFORMACAO = "PRECISA_INFORMACAO";
    static final String SEM_LANCAMENTO = "SEM_LANCAMENTO";
    /** Contrato v2: pergunta do produtor; o app calcula o resultado a partir de "consulta". */
    static final String CONSULTA = "CONSULTA";
    static final java.util.Set<String> TIPOS_CONSULTA = java.util.Set.of(
            "GASTOS", "RECEITAS", "SALDO", "LANCAMENTOS", "SAFRAS", "AREA", "METAS", "MAQUINAS");
    static final java.util.Set<String> SITUACOES = java.util.Set.of("ATIVAS", "ENCERRADAS", "TODAS");
    static final java.util.Set<String> AGRUPAMENTOS = java.util.Set.of("CATEGORIA", "SAFRA", "MES", "MAQUINA");
    static final int MAX_SUGESTOES = 5;
    static final int MAX_SUGESTAO = 60;

    private static final int MAX_CARACTERES_MENSAGEM = 2000;
    private static final int MAX_MENSAGENS_HISTORICO = 20;
    private static final int MAX_ITENS_CONTEXTO = 300;
    private static final int MAX_DESCRICAO = 255;
    /** Limite da foto depois de decodificada (a API aceita até ~5 MB por imagem). */
    static final int MAX_BYTES_IMAGEM = 5 * 1024 * 1024;
    /** Limite do arquivo (planilha/CSV/texto) antes de ler. */
    static final int MAX_BYTES_ARQUIVO = 2 * 1024 * 1024;
    /** Máximo de lançamentos devolvidos numa resposta (arquivo grande). */
    static final int MAX_LANCAMENTOS = 200;
    private static final String[] DIAS_SEMANA = {
        "segunda-feira", "terça-feira", "quarta-feira", "quinta-feira", "sexta-feira", "sábado", "domingo"
    };

    private final AnthropicClient ia;
    private final LimiteDiarioIa limite;
    private final Map<String, Object> ferramenta;
    private final int maxTokensArquivo;

    public ChatService(AnthropicClient ia, LimiteDiarioIa limite, ObjectMapper mapper,
                       @org.springframework.beans.factory.annotation.Value("${ia.max-tokens-arquivo:8192}") int maxTokensArquivo) {
        this.ia = ia;
        this.limite = limite;
        this.maxTokensArquivo = maxTokensArquivo;
        try {
            this.ferramenta = mapper.readValue(FERRAMENTA_JSON, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            throw new IllegalStateException("Definição da ferramenta de IA inválida", e);
        }
    }

    public ChatMensagemResponse processar(ChatMensagemRequest req) {
        if (!ia.configurado()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Assistente de IA não configurado no servidor");
        }
        boolean temImagem = req.getImagem() != null && req.getImagem().getBase64() != null && !req.getImagem().getBase64().isBlank();
        boolean temArquivo = req.getArquivo() != null && req.getArquivo().getBase64() != null && !req.getArquivo().getBase64().isBlank();
        if (temImagem && temArquivo) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Envie foto ou arquivo, não os dois na mesma mensagem");
        }
        List<Map<String, Object>> mensagens = montarMensagens(req);
        anexarImagem(mensagens, req.getImagem());
        LeitorArquivo.Conteudo arquivo = anexarArquivo(mensagens, req.getArquivo());
        LocalDate hoje = parseData(req.getHoje());
        if (hoje == null) hoje = LocalDate.now(ZoneId.of("America/Sao_Paulo"));
        ChatMensagemRequest.Contexto ctx = req.getContexto() != null ? req.getContexto() : new ChatMensagemRequest.Contexto();

        Long usuarioId = SecurityUtils.usuarioLogado().getId();
        int restantes = limite.consumir(usuarioId);
        if (restantes < 0) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Limite diário de mensagens do assistente atingido");
        }

        JsonNode saida;
        try {
            saida = arquivo == null
                    ? ia.chamarComFerramenta(promptSistema(hoje, ctx), mensagens, ferramenta)
                    : ia.chamarComFerramenta(promptSistema(hoje, ctx), mensagens, ferramenta, maxTokensArquivo);
        } catch (RestClientResponseException e) {
            limite.devolver(usuarioId);
            log.warn("API de IA respondeu {}: {}", e.getStatusCode().value(), resumir(e.getResponseBodyAsString()));
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Falha no serviço de IA");
        } catch (RestClientException | IllegalStateException e) {
            limite.devolver(usuarioId);
            log.warn("Falha ao chamar a API de IA: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Falha no serviço de IA");
        }

        ChatMensagemResponse resp = validar(saida, ctx, hoje);
        resp.setMensagensRestantesHoje(restantes);
        if (arquivo != null && arquivo.truncado) {
            resp.getAvisos().add("Arquivo grande: só as primeiras " + arquivo.linhasLidas
                    + " linhas foram lidas. Divida o arquivo para lançar o restante.");
        }
        return resp;
    }

    // ------------------------------------------------------------------
    // Entrada
    // ------------------------------------------------------------------

    /**
     * Converte o histórico do app pro formato da API: começa com "user",
     * papéis alternados (mensagens seguidas do mesmo papel são juntadas) e a
     * última é obrigatoriamente do produtor.
     */
    List<Map<String, Object>> montarMensagens(ChatMensagemRequest req) {
        List<ChatMensagemRequest.Mensagem> historico = req.getMensagens();
        boolean temImagem = (req.getImagem() != null && req.getImagem().getBase64() != null
                && !req.getImagem().getBase64().isBlank())
                || (req.getArquivo() != null && req.getArquivo().getBase64() != null
                && !req.getArquivo().getBase64().isBlank()); // qualquer anexo
        if ((historico == null || historico.isEmpty()) && !temImagem) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nenhuma mensagem enviada");
        }
        if (historico == null) historico = new ArrayList<>();
        if (historico.size() > MAX_MENSAGENS_HISTORICO) {
            historico = historico.subList(historico.size() - MAX_MENSAGENS_HISTORICO, historico.size());
        }

        List<Map<String, Object>> saida = new ArrayList<>();
        for (ChatMensagemRequest.Mensagem m : historico) {
            if (m == null || m.getTexto() == null || m.getTexto().isBlank()) continue;
            String papel = "assistente".equalsIgnoreCase(m.getPapel()) ? "assistant" : "user";
            String texto = m.getTexto().trim();
            if (texto.length() > MAX_CARACTERES_MENSAGEM) texto = texto.substring(0, MAX_CARACTERES_MENSAGEM);
            if (saida.isEmpty() && "assistant".equals(papel)) continue; // a API exige começar pelo usuário
            Map<String, Object> ultima = saida.isEmpty() ? null : saida.get(saida.size() - 1);
            if (ultima != null && papel.equals(ultima.get("role"))) {
                ultima.put("content", ultima.get("content") + "\n" + texto);
            } else {
                Map<String, Object> nova = new HashMap<>();
                nova.put("role", papel);
                nova.put("content", texto);
                saida.add(nova);
            }
        }
        if (temImagem && (saida.isEmpty() || !"user".equals(saida.get(saida.size() - 1).get("role")))) {
            // só foto, sem texto: cria a mensagem do produtor que vai carregar a imagem
            Map<String, Object> nova = new HashMap<>();
            nova.put("role", "user");
            nova.put("content", req.getArquivo() != null ? "O produtor enviou este arquivo." : "O produtor enviou esta foto.");
            saida.add(nova);
        }
        if (saida.isEmpty() || !"user".equals(saida.get(saida.size() - 1).get("role"))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A última mensagem precisa ser do produtor");
        }
        return saida;
    }

    /**
     * Valida a foto (tamanho e tipo pelos bytes, sem confiar no que o app diz) e
     * transforma a última mensagem do produtor em [imagem, texto] no formato da API.
     */
    void anexarImagem(List<Map<String, Object>> mensagens, ChatMensagemRequest.Imagem imagem) {
        if (imagem == null || imagem.getBase64() == null || imagem.getBase64().isBlank()) return;
        String b64 = imagem.getBase64().trim();
        int virgula = b64.indexOf(',');
        if (b64.startsWith("data:") && virgula > 0) b64 = b64.substring(virgula + 1);
        byte[] bytes;
        try {
            bytes = java.util.Base64.getMimeDecoder().decode(b64);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Imagem inválida (base64)");
        }
        if (bytes.length == 0) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Imagem vazia");
        if (bytes.length > MAX_BYTES_IMAGEM) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "Imagem maior que 5 MB");
        }
        String tipo = detectarTipo(bytes);
        if (tipo == null) {
            throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Formato de imagem não suportado (use JPEG, PNG ou WebP)");
        }

        Map<String, Object> ultima = mensagens.get(mensagens.size() - 1);
        Map<String, Object> fonte = new LinkedHashMap<>();
        fonte.put("type", "base64");
        fonte.put("media_type", tipo);
        fonte.put("data", java.util.Base64.getEncoder().encodeToString(bytes));
        Map<String, Object> blocoImagem = new LinkedHashMap<>();
        blocoImagem.put("type", "image");
        blocoImagem.put("source", fonte);
        Map<String, Object> blocoTexto = new LinkedHashMap<>();
        blocoTexto.put("type", "text");
        blocoTexto.put("text", String.valueOf(ultima.get("content")));
        ultima.put("content", List.of(blocoImagem, blocoTexto));
    }

    /**
     * Lê o arquivo (planilha/CSV/texto) e cola o conteúdo, como texto, na última
     * mensagem do produtor. Devolve o que foi lido (ou null se não houver arquivo).
     */
    LeitorArquivo.Conteudo anexarArquivo(List<Map<String, Object>> mensagens, ChatMensagemRequest.Arquivo arquivo) {
        if (arquivo == null || arquivo.getBase64() == null || arquivo.getBase64().isBlank()) return null;
        String b64 = arquivo.getBase64().trim();
        int virgula = b64.indexOf(',');
        if (b64.startsWith("data:") && virgula > 0) b64 = b64.substring(virgula + 1);
        byte[] bytes;
        try {
            bytes = java.util.Base64.getMimeDecoder().decode(b64);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Arquivo inválido (base64)");
        }
        if (bytes.length == 0) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Arquivo vazio");
        if (bytes.length > MAX_BYTES_ARQUIVO) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "Arquivo maior que 2 MB");
        }
        LeitorArquivo.Conteudo conteudo;
        try {
            conteudo = LeitorArquivo.ler(arquivo.getNome(), bytes);
        } catch (LeitorArquivo.FormatoNaoSuportado e) {
            throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, e.getMessage());
        } catch (java.io.IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Não foi possível ler o arquivo");
        }
        if (conteudo.texto.isBlank()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O arquivo não tem conteúdo");

        String nome = arquivo.getNome() == null ? "arquivo" : arquivo.getNome().replaceAll("[\\r\\n\"]", " ").trim();
        Map<String, Object> ultima = mensagens.get(mensagens.size() - 1);
        ultima.put("content", ultima.get("content") + "\n\nARQUIVO ANEXADO \"" + nome + "\" (" + conteudo.formato + ", "
                + conteudo.linhasLidas + " linhas" + (conteudo.truncado ? ", cortado no limite de leitura" : "") + "):\n"
                + "<<<INICIO DO ARQUIVO>>>\n" + conteudo.texto + "<<<FIM DO ARQUIVO>>>");
        return conteudo;
    }

    /** Identifica o formato pelos primeiros bytes (assinatura do arquivo). */
    static String detectarTipo(byte[] b) {
        if (b.length >= 3 && (b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF) return "image/jpeg";
        if (b.length >= 8 && (b[0] & 0xFF) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G') return "image/png";
        if (b.length >= 12 && b[0] == 'R' && b[1] == 'I' && b[2] == 'F' && b[3] == 'F'
                && b[8] == 'W' && b[9] == 'E' && b[10] == 'B' && b[11] == 'P') return "image/webp";
        return null;
    }

    String promptSistema(LocalDate hoje, ChatMensagemRequest.Contexto ctx) {
        List<ChatMensagemRequest.Safra> safras = new ArrayList<>();
        for (ChatMensagemRequest.Safra s : limitar(ctx.getSafras())) if (s != null && s.getUuid() != null) safras.add(s);
        int ativas = 0;
        for (ChatMensagemRequest.Safra s : safras) if (Boolean.TRUE.equals(s.getAtiva())) ativas++;
        int encerradas = safras.size() - ativas;

        LocalDate segunda = hoje.minusDays(hoje.getDayOfWeek().getValue() - 1L);
        LocalDate mesPassado = hoje.minusMonths(1).withDayOfMonth(1);

        StringBuilder sb = new StringBuilder();
        sb.append("Você é o assistente do SafraPay, um app de gestão financeira rural usado por produtores no Brasil.\n")
          .append("Você faz duas coisas: (1) REGISTRAR despesas e receitas que o produtor conta na conversa e ")
          .append("(2) entender PERGUNTAS sobre os dados dele (gastos, receitas, saldo, lançamentos, safras, área, metas, máquinas). ")
          .append("Responda sempre pela ferramenta responder_produtor. Você não grava nada e NÃO calcula nada: ")
          .append("o app mostra a proposta para o produtor confirmar, e é o app que calcula a resposta das perguntas. ")
          .append("Você não conhece nenhum valor financeiro do produtor — nunca invente números, uuids, safras ou categorias.\n\n");

        // ---- Datas
        sb.append("DATAS — hoje é ").append(hoje).append(" (").append(DIAS_SEMANA[hoje.getDayOfWeek().getValue() - 1]).append("). Referências já calculadas:\n")
          .append("- ontem = ").append(hoje.minusDays(1)).append("; anteontem = ").append(hoje.minusDays(2)).append('\n')
          .append("- esta semana = ").append(segunda).append(" a ").append(hoje).append('\n')
          .append("- semana passada (segunda a domingo) = ").append(segunda.minusWeeks(1)).append(" a ").append(segunda.minusDays(1)).append('\n')
          .append("- este mês = ").append(hoje.withDayOfMonth(1)).append(" a ").append(hoje).append('\n')
          .append("- mês passado = ").append(mesPassado).append(" a ").append(mesPassado.withDayOfMonth(mesPassado.lengthOfMonth())).append('\n')
          .append("- este ano = ").append(hoje.withDayOfYear(1)).append(" a ").append(hoje).append('\n')
          .append("Converta datas para AAAA-MM-DD. Dia/mês sem ano (\"09/08\", \"nove de agosto\", \"dia 15\") = ano atual (").append(hoje.getYear()).append("). ")
          .append("Só em PERGUNTA: se essa data cair depois de hoje, use o ano anterior. ")
          .append("\"em agosto\" = o mês inteiro (dia 1 ao último dia); \"de 01/08 a 15/08\" = esse intervalo. ")
          .append("Ao LANÇAR sem data, use hoje — NUNCA pergunte a data.\n\n");

        // ---- Lançamentos
        sb.append("REGISTRAR LANÇAMENTOS:\n")
          .append("- Valores em reais. Entenda formas como \"200 conto\", \"2 mil\", \"1,5 mil\", \"R$ 1.234,56\". O valor é sempre positivo.\n")
          .append("- Despesa: a categoria é obrigatória e deve ser exatamente um nome desta lista: ")
          .append(listaCategorias(ctx)).append(". ")
          .append("Escolha a mais adequada (ex.: diesel/gasolina → Combustível; adubo, defensivo, calcário, ureia → Insumos; semente → Sementes; ")
          .append("peça, conserto, manutenção de máquina → Máquinas; imposto, ITR, taxa → Impostos; cartório, licença → Documentos), ")
          .append("sempre respeitando os nomes que existem na lista.\n")
          .append("- Receita: não tem categoria (use null).\n")
          .append("- Máquina: só preencha maquinaUuid se o produtor citar uma máquina da lista; senão null. Receita nunca tem máquina.\n")
          .append("- Descrição: curta e clara, com a primeira letra maiúscula (ex.: \"Diesel S10\", \"Venda de 400 sacas de soja\").\n")
          .append("- Uma mensagem pode ter mais de um lançamento: devolva todos.\n")
          .append("- Sempre que o produtor estiver registrando algo, inclua o lançamento em lancamentos MESMO INCOMPLETO, ")
          .append("com null nos campos que não souber (ex.: safraUuid null quando precisar perguntar a safra). ")
          .append("Nunca devolva lancamentos vazio quando houver um lançamento em andamento na conversa.\n")
          .append("- Se o produtor corrigir algo (ex.: \"não, foi 250\"), devolva a proposta inteira de novo, já corrigida.\n")
          .append("- Valor muito fora do comum para o tipo de gasto (ex.: diesel de R$ 50.000): mantenha PROPOSTA, mas destaque o valor na resposta ")
          .append("pedindo para o produtor conferir.\n")
          .append("- FOTO: se vier uma foto de nota fiscal, cupom, recibo ou comprovante, leia o valor TOTAL (não os itens um a um, ")
          .append("a menos que o produtor peça), a data de emissão (use a data do documento, convertida para AAAA-MM-DD), o fornecedor e o que foi comprado; ")
          .append("monte uma despesa com descrição curta (ex.: \"Adubo NPK - Agropecuária Silva\") e escolha a categoria pelos itens. ")
          .append("Nota de venda da produção (o produtor vendendo) é receita. Se a foto estiver ilegível ou cortada, peça outra foto. ")
          .append("Se a foto não for um documento com valor (ex.: máquina quebrada), use SEM_LANCAMENTO e explique que, para lançar, precisa do valor.\n")
          .append("- ARQUIVO: se vier um arquivo anexado (planilha, CSV ou bloco de notas), cada linha/registro que for um lançamento ")
          .append("individual vira um item em lancamentos. Ignore cabeçalhos, totais, subtotais, saldos e linhas de resumo. ")
          .append("Use as colunas do arquivo: tipo/sinal (valor negativo, \"saída\" ou \"débito\" = despesa; \"entrada\" ou \"crédito\" = receita), ")
          .append("data (converta para AAAA-MM-DD; se a linha não tiver data, deixe null — em arquivo NÃO use a data de hoje), ")
          .append("descrição, categoria, cultura/safra/talhão. Se o produtor disser no texto algo que vale para todos (ex.: \"tudo da soja\"), aplique a todos. ")
          .append("No máximo " + MAX_LANCAMENTOS + " lançamentos. Na resposta NÃO liste item por item: resuma quantos lançamentos encontrou, ")
          .append("quantas despesas e receitas e o total de cada, e diga o que falta (ex.: \"12 sem safra\") — o app mostra a lista para revisão.\n\n");

        // ---- Safra ao lançar
        sb.append("SAFRA AO LANÇAR (o produtor tem hoje ").append(ativas).append(" safra(s) ativa(s) e ")
          .append(encerradas).append(" encerrada(s)):\n")
          .append("- Use sempre o uuid de uma safra da lista abaixo. Se o produtor citar cultura, talhão, propriedade ou ano, escolha a safra correspondente ")
          .append("(pode ser encerrada, se ele indicar); se ainda sobrar mais de uma candidata, pergunte só entre elas.\n")
          .append("- Não citou e há exatamente 1 safra ativa → use essa e AVISE na resposta, ex.: ")
          .append("\"Lancei na safra Soja 2025/2026 • Talhão A, que é a única em aberto. Se for outra, me diga.\"\n")
          .append("- Não citou e há 2 ou mais ativas → safraUuid null, status PRECISA_INFORMACAO, resposta com uma lista numerada das safras ativas ")
          .append("(use os rótulos) perguntando em qual lançar, e sugestoes com o rótulo de cada uma. ")
          .append("Depois entenda respostas curtas como \"1\", \"a primeira\", \"a da soja\", \"talhão A\" como escolha dessa lista.\n")
          .append("- safraCitada (em cada lançamento): true SÓ se o produtor indicou a safra nesta conversa (citou cultura, talhão, ano ou propriedade, ")
          .append("ou escolheu da lista); false se foi você que escolheu. Com 2 ou mais safras ativas, nunca escolha sozinho.\n")
          .append("- Nenhuma ativa, mas há encerradas → safraUuid null, PRECISA_INFORMACAO: liste as encerradas e pergunte se é em uma delas ")
          .append("ou se ele precisa abrir uma safra nova (no app: Início → \"+ Nova safra\").\n")
          .append("- Nenhuma safra cadastrada → explique que antes é preciso cadastrar uma safra no app (Início → \"+ Nova safra\").\n\n");

        // ---- Perguntas
        sb.append("PERGUNTAS (status CONSULTA):\n")
          .append("- Quando o produtor PERGUNTAR algo sobre os dados dele, use status CONSULTA, lancamentos vazio e preencha consulta com os filtros. ")
          .append("Tipos de consulta: GASTOS (despesas); RECEITAS; SALDO (receitas menos despesas: \"lucro\", \"resultado\", \"quanto sobrou\"); ")
          .append("LANCAMENTOS (\"últimos lançamentos\", \"o que eu lancei\"); SAFRAS (lista de safras); AREA (área de propriedades, talhões ou de uma cultura); ")
          .append("METAS; MAQUINAS (gasto por máquina e manutenções/anotações pendentes).\n")
          .append("- Preencha só os filtros que o produtor disse; o resto fica null. safraUuid, propriedadeUuid, talhaoUuid e maquinaUuid só com uuids das listas abaixo ")
          .append("(\"com o trator\" → maquinaUuid do trator). categoria só com um nome da lista de categorias. ")
          .append("Cultura citada (\"na soja\", \"do milho\", \"área de milho\") vai em cultura (ex.: \"Soja\"), NÃO em safraUuid — ")
          .append("mesmo que só exista uma safra dessa cultura. safraUuid só quando ele falar de uma safra específica ")
          .append("(\"safra 2025/2026\", \"safra do talhão A\") ou escolher da lista. ")
          .append("situacao só para SAFRAS: ATIVAS (padrão), ENCERRADAS ou TODAS. ")
          .append("agrupar só se ele pedir detalhamento: CATEGORIA, SAFRA, MES ou MAQUINA.\n")
          .append("- Período: preencha dataInicio/dataFim só se ele disser um dia ou período (um dia só = mesma data nos dois). ")
          .append("Se não disser, deixe as duas null: o app usa a safra/cultura/talhão inteiros quando houver esse filtro, senão o mês atual — ")
          .append("nesse último caso a resposta deve deixar claro que é \"neste mês\".\n")
          .append("- Na CONSULTA a resposta é SÓ uma frase curta de introdução, SEM nenhum valor (ex.: \"Seus gastos em 09/08/2026:\", ")
          .append("\"Estas são suas safras em aberto:\", \"Seus gastos com combustível neste mês:\"). O app completa com o resultado calculado.\n")
          .append("- Lançamento e pergunta na mesma mensagem: trate só o lançamento agora e coloque a pergunta em sugestoes ")
          .append("(ex.: \"Quanto gastei este mês?\") para ele tocar em seguida.\n")
          .append("- Pergunta que esses tipos não respondem (preço de mercado, previsão do tempo, recomendação) → SEM_LANCAMENTO.\n\n");

        // ---- Usabilidade
        sb.append("CONVERSA:\n")
          .append("- Faltando dado, pergunte UMA coisa por vez, de forma objetiva, com sugestoes quando houver opções fechadas.\n")
          .append("- sugestoes: até 5 respostas rápidas de até 60 caracteres, que o app mostra como botões; tocar envia o texto como mensagem do produtor, ")
          .append("então escreva do ponto de vista dele (ex.: rótulos de safra, \"Quanto gastei este mês?\"). ")
          .append("Em PROPOSTA não sugira \"confirmar\"/\"sim\" — o app já tem o botão de confirmar no card. Pode ficar vazio.\n")
          .append("- Fora do escopo (conselho agronômico, financeiro ou jurídico, clima): SEM_LANCAMENTO, diga em uma frase que ainda não faz isso ")
          .append("e ofereça sugestoes do que faz.\n")
          .append("- \"O que você faz?\", ajuda ou saudação: SEM_LANCAMENTO, explique curto que você lança gastos e receitas (por texto, foto de nota ou planilha) ")
          .append("e responde perguntas sobre gastos, receitas, saldo, safras, área, metas e máquinas, com sugestoes como ")
          .append("\"Lançar um gasto\", \"Quanto gastei este mês?\", \"Quais safras estão ativas?\".\n")
          .append("- status: PROPOSTA quando todos os lançamentos estiverem completos; PRECISA_INFORMACAO quando faltar algo num lançamento ")
          .append("(ou para escolher a safra); CONSULTA para perguntas; SEM_LANCAMENTO para o resto.\n")
          .append("- resposta: português do Brasil, simples e curta, como numa conversa de WhatsApp. Em PROPOSTA, resuma o que entendeu ")
          .append("e peça confirmação (ex.: \"Entendi: despesa de R$ 200,00 com Combustível em 30/09, safra Soja 2025/2026. Confirma?\").\n\n");

        // ---- Contexto
        sb.append("Safras do produtor (uuid | rótulo | situação | início a fim | propriedade):\n");
        Map<String, String> nomesPropriedades = new HashMap<>();
        for (ChatMensagemRequest.Propriedade p : limitar(ctx.getPropriedades())) {
            if (p != null && p.getUuid() != null) nomesPropriedades.put(p.getUuid(), nn(p.getNome()));
        }
        if (safras.isEmpty()) sb.append("(nenhuma safra cadastrada)\n");
        for (ChatMensagemRequest.Safra s : safras) {
            sb.append(s.getUuid()).append(" | ").append(rotuloSafra(s)).append(" | ")
              .append(Boolean.TRUE.equals(s.getAtiva()) ? "ativa" : "encerrada").append(" | ")
              .append(s.getDataInicio() == null ? "?" : s.getDataInicio()).append(" a ")
              .append(s.getDataFim() == null ? "em andamento" : s.getDataFim()).append(" | ")
              .append(nomesPropriedades.getOrDefault(nn(s.getPropriedadeUuid()), "")).append('\n');
        }
        sb.append("\nPropriedades (uuid | nome | área total em ha):\n");
        boolean algumaPropriedade = false;
        for (ChatMensagemRequest.Propriedade p : limitar(ctx.getPropriedades())) {
            if (p == null || p.getUuid() == null) continue;
            algumaPropriedade = true;
            sb.append(p.getUuid()).append(" | ").append(nn(p.getNome())).append(" | ").append(area(p.getAreaTotal())).append('\n');
        }
        if (!algumaPropriedade) sb.append("(nenhuma)\n");
        sb.append("\nTalhões (uuid | nome | propriedade | cultura | área em ha):\n");
        boolean algumTalhao = false;
        for (ChatMensagemRequest.Talhao t : limitar(ctx.getTalhoes())) {
            if (t == null || t.getUuid() == null) continue;
            algumTalhao = true;
            sb.append(t.getUuid()).append(" | ").append(nn(t.getNome())).append(" | ")
              .append(nomesPropriedades.getOrDefault(nn(t.getPropriedadeUuid()), "")).append(" | ")
              .append(nn(t.getCultura())).append(" | ").append(area(t.getAreaHa())).append('\n');
        }
        if (!algumTalhao) sb.append("(nenhum)\n");
        sb.append("\nMáquinas do produtor (uuid | nome):\n");
        List<ChatMensagemRequest.Maquina> maquinas = limitar(ctx.getMaquinas());
        boolean algumaMaquina = false;
        for (ChatMensagemRequest.Maquina m : maquinas) {
            if (m == null || m.getUuid() == null) continue;
            algumaMaquina = true;
            sb.append(m.getUuid()).append(" | ").append(nn(m.getNome())).append('\n');
        }
        if (!algumaMaquina) sb.append("(nenhuma)\n");
        return sb.toString();
    }

    /**
     * Rótulo curto da safra ("Soja 2025/2026 • Talhão A"), usado na lista numerada
     * e nos botões de sugestão. Sem os campos v2, cai na descrição que o app manda.
     */
    static String rotuloSafra(ChatMensagemRequest.Safra s) {
        String cultura = vazioParaNull(s.getCultura());
        String rotulo;
        if (cultura != null) {
            StringBuilder r = new StringBuilder(cultura);
            String ano = vazioParaNull(s.getAnoSafra());
            if (ano != null) r.append(' ').append(ano);
            String talhao = vazioParaNull(s.getTalhao());
            if (talhao != null) r.append(" • ").append(talhao);
            rotulo = r.toString();
        } else {
            rotulo = vazioParaNull(s.getDescricao()) != null ? s.getDescricao().trim() : "Safra sem nome";
        }
        return rotulo.length() > MAX_SUGESTAO ? rotulo.substring(0, MAX_SUGESTAO).trim() : rotulo;
    }

    private static String area(Double ha) {
        if (ha == null) return "?";
        return String.format(Locale.ROOT, "%.2f", ha);
    }

    private String listaCategorias(ChatMensagemRequest.Contexto ctx) {
        List<String> cats = limitar(ctx.getCategorias());
        if (cats.isEmpty()) return "(nenhuma categoria informada)";
        StringBuilder sb = new StringBuilder();
        for (String c : cats) {
            if (c == null || c.isBlank()) continue;
            if (sb.length() > 0) sb.append(", ");
            sb.append('"').append(c.trim()).append('"');
        }
        return sb.toString();
    }

    // ------------------------------------------------------------------
    // Saída (revalidação de tudo que veio da IA)
    // ------------------------------------------------------------------

    ChatMensagemResponse validar(JsonNode saida, ChatMensagemRequest.Contexto ctx) {
        return validar(saida, ctx, LocalDate.now(ZoneId.of("America/Sao_Paulo")));
    }

    ChatMensagemResponse validar(JsonNode saida, ChatMensagemRequest.Contexto ctx, LocalDate hoje) {
        Map<String, String> categorias = new HashMap<>(); // minúsculo -> nome oficial
        for (String c : limitar(ctx.getCategorias())) {
            if (c != null && !c.isBlank()) categorias.put(c.trim().toLowerCase(Locale.ROOT), c.trim());
        }
        java.util.Set<String> safras = new java.util.HashSet<>();
        for (ChatMensagemRequest.Safra s : limitar(ctx.getSafras())) if (s != null && s.getUuid() != null) safras.add(s.getUuid());
        java.util.Set<String> maquinas = new java.util.HashSet<>();
        for (ChatMensagemRequest.Maquina m : limitar(ctx.getMaquinas())) if (m != null && m.getUuid() != null) maquinas.add(m.getUuid());

        ChatMensagemResponse resp = new ChatMensagemResponse();
        String texto = saida.path("resposta").asText("").trim();
        resp.setResposta(texto.isEmpty() ? "Não entendi. Pode repetir?" : texto);
        resp.setSugestoes(validarSugestoes(saida.path("sugestoes")));

        boolean faltaAlgo = false;
        boolean faltaSafra = false;
        boolean safraEscolhidaPelaIa = false;
        List<ChatMensagemRequest.Safra> ativas = new ArrayList<>();
        for (ChatMensagemRequest.Safra s : limitar(ctx.getSafras())) {
            if (s != null && s.getUuid() != null && Boolean.TRUE.equals(s.getAtiva())) ativas.add(s);
        }
        int descartados = 0;
        for (JsonNode item : saida.path("lancamentos")) {
            if (resp.getLancamentos().size() >= MAX_LANCAMENTOS) { descartados++; continue; }
            String tipo = item.path("tipo").asText("").toUpperCase(Locale.ROOT);
            if (!tipo.equals("DESPESA") && !tipo.equals("RECEITA")) continue;
            boolean despesa = tipo.equals("DESPESA");

            LancamentoProposto l = new LancamentoProposto();
            l.setTipo(tipo);
            List<String> faltando = new ArrayList<>();

            double valor = item.path("valor").asDouble(0);
            if (item.path("valor").isNumber() && valor > 0) {
                l.setValor(Math.round(valor * 100.0) / 100.0);
            } else {
                faltando.add("valor");
            }

            LocalDate data = parseData(textoOuNull(item.path("data")));
            if (data != null) l.setData(data.toString()); else faltando.add("data");

            String descricao = textoOuNull(item.path("descricao"));
            if (descricao != null) {
                if (descricao.length() > MAX_DESCRICAO) descricao = descricao.substring(0, MAX_DESCRICAO);
                l.setDescricao(descricao);
            } else {
                faltando.add("descricao");
            }

            if (despesa) {
                String cat = textoOuNull(item.path("categoria"));
                String oficial = cat == null ? null : categorias.get(cat.toLowerCase(Locale.ROOT));
                if (oficial != null) l.setCategoria(oficial); else faltando.add("categoria");

                String maq = textoOuNull(item.path("maquinaUuid"));
                if (maq != null && maquinas.contains(maq)) l.setMaquinaUuid(maq);
            }

            String safra = textoOuNull(item.path("safraUuid"));
            // Com 2+ safras ativas, a IA não pode escolher sozinha (regra do Gustavo):
            // se ela marcou que o produtor não citou a safra, o servidor pergunta.
            if (safra != null && ativas.size() >= 2 && !item.path("safraCitada").asBoolean(true)) {
                safra = null;
                safraEscolhidaPelaIa = true;
            }
            if (safra != null && safras.contains(safra)) {
                l.setSafraUuid(safra);
            } else {
                faltando.add("safra");
                faltaSafra = true;
            }

            l.setCamposFaltando(faltando);
            if (!faltando.isEmpty()) faltaAlgo = true;
            resp.getLancamentos().add(l);
        }

        if (descartados > 0) {
            resp.getAvisos().add("Só os primeiros " + MAX_LANCAMENTOS + " lançamentos foram considerados ("
                    + descartados + " ficaram de fora). Envie o restante em outro arquivo.");
        }

        String statusIa = saida.path("status").asText("").trim().toUpperCase(Locale.ROOT);
        if (!resp.getLancamentos().isEmpty()) {
            // Lançamento tem prioridade sobre pergunta (a pergunta vira sugestão).
            resp.setStatus(faltaAlgo ? PRECISA_INFORMACAO : PROPOSTA);
            if (safraEscolhidaPelaIa) {
                resp.setResposta(perguntaSafra(resp.getLancamentos(), ativas));
                resp.setSugestoes(sugestoesDeSafra(ctx));
            } else if (faltaSafra && resp.getSugestoes().isEmpty()) {
                resp.setSugestoes(sugestoesDeSafra(ctx));
            }
        } else if (CONSULTA.equals(statusIa)) {
            ChatMensagemResponse.Consulta consulta = validarConsulta(saida.path("consulta"), ctx, categorias, safras, maquinas, hoje);
            if (consulta != null) {
                resp.setStatus(CONSULTA);
                resp.setConsulta(consulta);
                // A IA não conhece os valores: qualquer "R$" na introdução seria inventado.
                if (resp.getResposta().contains("R$")) resp.setResposta("Aqui está o que encontrei:");
                resp.setResposta(avisarMesAtual(resp.getResposta(), consulta));
            } else {
                resp.setStatus(SEM_LANCAMENTO);
            }
        } else {
            // A IA pode estar no meio de uma pergunta sem ter devolvido o lançamento ainda:
            // respeita o PRECISA_INFORMACAO dela (o app só mostra a pergunta).
            resp.setStatus(PRECISA_INFORMACAO.equals(statusIa) ? PRECISA_INFORMACAO : SEM_LANCAMENTO);
        }
        return resp;
    }

    /**
     * Revalida a pergunta estruturada: tipo no enum (senão null → SEM_LANCAMENTO),
     * uuids existentes no contexto, categoria da lista, datas válidas (trocadas se
     * invertidas; data no futuro volta um ano), enums de situação/agrupamento.
     */
    ChatMensagemResponse.Consulta validarConsulta(JsonNode c, ChatMensagemRequest.Contexto ctx,
                                                  Map<String, String> categorias, java.util.Set<String> safras,
                                                  java.util.Set<String> maquinas, LocalDate hoje) {
        if (c == null || !c.isObject()) return null;
        String tipo = nn(textoOuNull(c.path("tipo"))).toUpperCase(Locale.ROOT);
        if (!TIPOS_CONSULTA.contains(tipo)) return null;

        java.util.Set<String> propriedades = new java.util.HashSet<>();
        for (ChatMensagemRequest.Propriedade p : limitar(ctx.getPropriedades())) if (p != null && p.getUuid() != null) propriedades.add(p.getUuid());
        java.util.Set<String> talhoes = new java.util.HashSet<>();
        for (ChatMensagemRequest.Talhao t : limitar(ctx.getTalhoes())) if (t != null && t.getUuid() != null) talhoes.add(t.getUuid());

        ChatMensagemResponse.Consulta q = new ChatMensagemResponse.Consulta();
        q.setTipo(tipo);

        LocalDate inicio = parseData(textoOuNull(c.path("dataInicio")));
        LocalDate fim = parseData(textoOuNull(c.path("dataFim")));
        if (inicio != null && fim != null && inicio.isAfter(fim)) { LocalDate t = inicio; inicio = fim; fim = t; }
        // "Quanto gastei dia 20/12?" perguntado em janeiro = dezembro passado.
        if (inicio != null && hoje != null && inicio.isAfter(hoje)) {
            inicio = inicio.minusYears(1);
            if (fim != null) fim = fim.minusYears(1);
        }
        q.setDataInicio(inicio == null ? null : inicio.toString());
        q.setDataFim(fim == null ? null : fim.toString());

        q.setSafraUuid(daLista(textoOuNull(c.path("safraUuid")), safras));
        q.setPropriedadeUuid(daLista(textoOuNull(c.path("propriedadeUuid")), propriedades));
        q.setTalhaoUuid(daLista(textoOuNull(c.path("talhaoUuid")), talhoes));
        q.setMaquinaUuid(daLista(textoOuNull(c.path("maquinaUuid")), maquinas));

        String cat = textoOuNull(c.path("categoria"));
        q.setCategoria(cat == null ? null : categorias.get(cat.toLowerCase(Locale.ROOT)));

        q.setCultura(normalizarCultura(textoOuNull(c.path("cultura")), ctx));

        String situacao = nn(textoOuNull(c.path("situacao"))).toUpperCase(Locale.ROOT);
        q.setSituacao(SITUACOES.contains(situacao) ? situacao : null);
        String agrupar = nn(textoOuNull(c.path("agrupar"))).toUpperCase(Locale.ROOT);
        q.setAgrupar(AGRUPAMENTOS.contains(agrupar) ? agrupar : null);
        return q;
    }

    /**
     * Pergunta montada pelo servidor quando a IA escolheu a safra sozinha com 2+
     * ativas: resume o lançamento e lista as safras ativas numeradas.
     */
    static String perguntaSafra(List<LancamentoProposto> lancamentos, List<ChatMensagemRequest.Safra> ativas) {
        StringBuilder sb = new StringBuilder();
        if (lancamentos.size() == 1) {
            LancamentoProposto l = lancamentos.get(0);
            sb.append("Entendi: ").append("RECEITA".equals(l.getTipo()) ? "receita" : "despesa");
            if (l.getValor() != null) sb.append(" de ").append(reais(l.getValor()));
            if (l.getDescricao() != null) sb.append(" (").append(l.getDescricao()).append(')');
            sb.append(". ");
        } else {
            sb.append("Entendi ").append(lancamentos.size()).append(" lançamentos. ");
        }
        sb.append("Em qual safra eu lanço?");
        int i = 0;
        for (ChatMensagemRequest.Safra s : ativas) {
            if (++i > 10) break;
            sb.append('\n').append(i).append(") ").append(rotuloSafra(s));
        }
        return sb.toString();
    }

    static String reais(double v) {
        return String.format(Locale.forLanguageTag("pt-BR"), "R$ %,.2f", v);
    }

    /**
     * GASTOS/RECEITAS/SALDO/LANCAMENTOS sem período e sem safra/cultura/talhão: o
     * app usa o mês atual. Garante que a introdução deixe isso claro.
     */
    static String avisarMesAtual(String resposta, ChatMensagemResponse.Consulta q) {
        boolean periodico = java.util.Set.of("GASTOS", "RECEITAS", "SALDO", "LANCAMENTOS").contains(q.getTipo());
        boolean semEscopo = q.getDataInicio() == null && q.getDataFim() == null && q.getSafraUuid() == null
                && q.getCultura() == null && q.getTalhaoUuid() == null;
        if (!periodico || !semEscopo) return resposta;
        String minusculo = resposta.toLowerCase(Locale.ROOT);
        if (minusculo.contains("mês") || minusculo.contains("mes ")) return resposta;
        String r = resposta.trim();
        if (r.endsWith(":")) return r.substring(0, r.length() - 1).trim() + " neste mês:";
        return r + " (neste mês)";
    }

    /** Usa a grafia da cultura que já existe no contexto (ex.: "soja" → "Soja"). */
    private static String normalizarCultura(String cultura, ChatMensagemRequest.Contexto ctx) {
        if (cultura == null) return null;
        for (ChatMensagemRequest.Safra s : limitar(ctx.getSafras())) {
            if (s != null && s.getCultura() != null && s.getCultura().trim().equalsIgnoreCase(cultura)) return s.getCultura().trim();
        }
        for (ChatMensagemRequest.Talhao t : limitar(ctx.getTalhoes())) {
            if (t != null && t.getCultura() != null && t.getCultura().trim().equalsIgnoreCase(cultura)) return t.getCultura().trim();
        }
        return cultura.length() > MAX_SUGESTAO ? cultura.substring(0, MAX_SUGESTAO).trim() : cultura;
    }

    /** Até 5 sugestões, sem repetidas, cada uma com até 60 caracteres. */
    static List<String> validarSugestoes(JsonNode n) {
        java.util.LinkedHashSet<String> saida = new java.util.LinkedHashSet<>();
        if (n != null && n.isArray()) {
            for (JsonNode s : n) {
                if (saida.size() >= MAX_SUGESTOES) break;
                String t = textoOuNull(s);
                if (t == null) continue;
                t = t.replaceAll("\\s+", " ");
                if (t.length() > MAX_SUGESTAO) t = t.substring(0, MAX_SUGESTAO).trim();
                saida.add(t);
            }
        }
        return new ArrayList<>(saida);
    }

    /** Se faltou a safra e a IA não sugeriu nada, oferece as safras ativas como botões. */
    private static List<String> sugestoesDeSafra(ChatMensagemRequest.Contexto ctx) {
        List<String> saida = new ArrayList<>();
        for (ChatMensagemRequest.Safra s : limitar(ctx.getSafras())) {
            if (s == null || s.getUuid() == null || !Boolean.TRUE.equals(s.getAtiva())) continue;
            String r = rotuloSafra(s);
            if (!saida.contains(r)) saida.add(r);
            if (saida.size() >= MAX_SUGESTOES) break;
        }
        return saida.size() >= 2 ? saida : new ArrayList<>();
    }

    private static String daLista(String uuid, java.util.Set<String> validos) {
        return uuid != null && validos.contains(uuid) ? uuid : null;
    }

    // ------------------------------------------------------------------
    // Utilitários
    // ------------------------------------------------------------------

    private static <T> List<T> limitar(List<T> lista) {
        if (lista == null) return new ArrayList<>();
        return lista.size() > MAX_ITENS_CONTEXTO ? lista.subList(0, MAX_ITENS_CONTEXTO) : lista;
    }

    private static LocalDate parseData(String iso) {
        if (iso == null || iso.isBlank()) return null;
        try { return LocalDate.parse(iso.trim()); } catch (Exception e) { return null; }
    }

    private static String textoOuNull(JsonNode n) {
        if (n == null || n.isNull() || n.isMissingNode()) return null;
        String s = n.asText("").trim();
        return s.isEmpty() || s.equalsIgnoreCase("null") ? null : s;
    }

    private static String nn(String s) { return s == null ? "" : s; }

    private static String vazioParaNull(String s) { return s == null || s.isBlank() ? null : s.trim(); }

    private static String resumir(String s) {
        if (s == null) return "";
        return s.length() > 300 ? s.substring(0, 300) + "…" : s;
    }

    /** Ferramenta que o modelo é obrigado a usar: define o formato da resposta. */
    private static final String FERRAMENTA_JSON = """
        {
          "name": "responder_produtor",
          "description": "Envia a resposta ao produtor: lançamentos financeiros (despesas/receitas) identificados na conversa, ou os filtros de uma pergunta (consulta), e sugestões de resposta rápida.",
          "input_schema": {
            "type": "object",
            "properties": {
              "status": {
                "type": "string",
                "enum": ["PROPOSTA", "PRECISA_INFORMACAO", "CONSULTA", "SEM_LANCAMENTO"],
                "description": "PROPOSTA: lançamentos completos, pedir confirmação. PRECISA_INFORMACAO: falta algum dado (ex.: qual safra). CONSULTA: o produtor fez uma pergunta sobre os dados dele. SEM_LANCAMENTO: conversa sem lançamento nem pergunta."
              },
              "resposta": {
                "type": "string",
                "description": "Mensagem curta em português do Brasil para mostrar ao produtor. Na CONSULTA, só uma frase de introdução, sem valores."
              },
              "lancamentos": {
                "type": "array",
                "description": "Lançamentos identificados na conversa (vazio se não houver, e sempre vazio na CONSULTA). Em correções, devolver a proposta inteira atualizada.",
                "items": {
                  "type": "object",
                  "properties": {
                    "tipo": {"type": "string", "enum": ["DESPESA", "RECEITA"]},
                    "valor": {"type": ["number", "null"], "description": "Valor em reais, positivo. null se não informado."},
                    "data": {"type": ["string", "null"], "description": "Data no formato AAAA-MM-DD."},
                    "descricao": {"type": ["string", "null"], "description": "Descrição curta do lançamento."},
                    "categoria": {"type": ["string", "null"], "description": "Só para despesa: nome exato de uma categoria da lista. null para receita."},
                    "safraUuid": {"type": ["string", "null"], "description": "uuid de uma safra da lista, ou null se precisar perguntar."},
                    "maquinaUuid": {"type": ["string", "null"], "description": "uuid de uma máquina da lista, só se o produtor citar uma máquina."},
                    "safraCitada": {"type": "boolean", "description": "true só se o produtor indicou a safra nesta conversa (cultura, talhão, ano, propriedade ou escolha da lista); false se você escolheu sozinho."}
                  },
                  "required": ["tipo", "valor", "data", "descricao", "categoria", "safraUuid", "maquinaUuid", "safraCitada"]
                }
              },
              "consulta": {
                "type": ["object", "null"],
                "description": "Só quando status = CONSULTA (senão null): filtros da pergunta. O app calcula o resultado. Campos não ditos pelo produtor ficam null.",
                "properties": {
                  "tipo": {"type": "string", "enum": ["GASTOS", "RECEITAS", "SALDO", "LANCAMENTOS", "SAFRAS", "AREA", "METAS", "MAQUINAS"]},
                  "dataInicio": {"type": ["string", "null"], "description": "AAAA-MM-DD, ou null se o produtor não disse período."},
                  "dataFim": {"type": ["string", "null"], "description": "AAAA-MM-DD, ou null se o produtor não disse período."},
                  "safraUuid": {"type": ["string", "null"], "description": "uuid de uma safra da lista."},
                  "propriedadeUuid": {"type": ["string", "null"], "description": "uuid de uma propriedade da lista."},
                  "talhaoUuid": {"type": ["string", "null"], "description": "uuid de um talhão da lista."},
                  "maquinaUuid": {"type": ["string", "null"], "description": "uuid de uma máquina da lista."},
                  "categoria": {"type": ["string", "null"], "description": "Nome exato de uma categoria da lista."},
                  "cultura": {"type": ["string", "null"], "description": "Cultura citada em geral (ex.: Soja)."},
                  "situacao": {"type": ["string", "null"], "description": "Só para SAFRAS: ATIVAS, ENCERRADAS ou TODAS (null = ATIVAS)."},
                  "agrupar": {"type": ["string", "null"], "description": "Só se o produtor pedir detalhamento: CATEGORIA, SAFRA, MES ou MAQUINA."}
                },
                "required": ["tipo"]
              },
              "sugestoes": {
                "type": "array",
                "description": "Até 5 respostas rápidas (até 60 caracteres) que o app mostra como botões; tocar envia o texto como mensagem do produtor.",
                "items": {"type": "string"},
                "maxItems": 5
              }
            },
            "required": ["status", "resposta", "lancamentos"]
          }
        }
        """;
}
