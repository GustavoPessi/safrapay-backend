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
 * Assistente de lançamentos (chat com IA dentro do app) — etapa 1: TEXTO.
 *
 * Recebe a conversa + o contexto do usuário, pede ao Claude que identifique
 * despesas/receitas e devolve a proposta estruturada. Não grava nada no banco:
 * quem grava é o app, depois que o produtor confirma.
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

    private static final int MAX_CARACTERES_MENSAGEM = 2000;
    private static final int MAX_MENSAGENS_HISTORICO = 20;
    private static final int MAX_ITENS_CONTEXTO = 300;
    private static final int MAX_DESCRICAO = 255;
    private static final String[] DIAS_SEMANA = {
        "segunda-feira", "terça-feira", "quarta-feira", "quinta-feira", "sexta-feira", "sábado", "domingo"
    };

    private final AnthropicClient ia;
    private final LimiteDiarioIa limite;
    private final Map<String, Object> ferramenta;

    public ChatService(AnthropicClient ia, LimiteDiarioIa limite, ObjectMapper mapper) {
        this.ia = ia;
        this.limite = limite;
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
        List<Map<String, Object>> mensagens = montarMensagens(req);
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
            saida = ia.chamarComFerramenta(promptSistema(hoje, ctx), mensagens, ferramenta);
        } catch (RestClientResponseException e) {
            limite.devolver(usuarioId);
            log.warn("API de IA respondeu {}: {}", e.getStatusCode().value(), resumir(e.getResponseBodyAsString()));
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Falha no serviço de IA");
        } catch (RestClientException | IllegalStateException e) {
            limite.devolver(usuarioId);
            log.warn("Falha ao chamar a API de IA: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Falha no serviço de IA");
        }

        ChatMensagemResponse resp = validar(saida, ctx);
        resp.setMensagensRestantesHoje(restantes);
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
        if (historico == null || historico.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nenhuma mensagem enviada");
        }
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
        if (saida.isEmpty() || !"user".equals(saida.get(saida.size() - 1).get("role"))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A última mensagem precisa ser do produtor");
        }
        return saida;
    }

    String promptSistema(LocalDate hoje, ChatMensagemRequest.Contexto ctx) {
        StringBuilder sb = new StringBuilder();
        sb.append("Você é o assistente de lançamentos do SafraPay, um app de gestão financeira rural usado por produtores no Brasil.\n")
          .append("Sua tarefa: a partir da conversa, identificar DESPESAS e RECEITAS que o produtor quer registrar e devolver tudo pela ferramenta responder_produtor. ")
          .append("Você não grava nada: o app mostra a sua proposta e o produtor confirma.\n\n")
          .append("Hoje é ").append(hoje).append(" (").append(DIAS_SEMANA[hoje.getDayOfWeek().getValue() - 1]).append("). ")
          .append("Converta datas relativas (\"ontem\", \"sexta passada\", \"dia 15\") para AAAA-MM-DD com base nisso. ")
          .append("Se o produtor não disser a data, use hoje — NUNCA pergunte a data.\n\n")
          .append("Regras:\n")
          .append("- Valores em reais. Entenda formas como \"200 conto\", \"2 mil\", \"1,5 mil\", \"R$ 1.234,56\". O valor é sempre positivo.\n")
          .append("- Despesa: a categoria é obrigatória e deve ser exatamente um nome desta lista: ")
          .append(listaCategorias(ctx)).append(". ")
          .append("Escolha a mais adequada (ex.: diesel/gasolina → Combustível; adubo, defensivo, calcário, ureia → Insumos; semente → Sementes; ")
          .append("peça, conserto, manutenção de máquina → Máquinas; imposto, ITR, taxa → Impostos; cartório, licença → Documentos), ")
          .append("sempre respeitando os nomes que existem na lista.\n")
          .append("- Receita: não tem categoria (use null).\n")
          .append("- Safra: use o uuid de uma das safras listadas abaixo. Se o produtor citar cultura, talhão ou ano, escolha a safra correspondente. ")
          .append("Se houver só uma safra ativa e ele não disser, use essa. Se houver mais de uma possível e ele não disser, deixe safraUuid null e pergunte qual. ")
          .append("Nunca invente uuid.\n")
          .append("- Máquina: só preencha maquinaUuid se o produtor citar uma máquina da lista; senão null. Receita nunca tem máquina.\n")
          .append("- Descrição: curta e clara, com a primeira letra maiúscula (ex.: \"Diesel S10\", \"Venda de 400 sacas de soja\").\n")
          .append("- Uma mensagem pode ter mais de um lançamento: devolva todos.\n")
          .append("- Sempre que o produtor estiver registrando algo, inclua o lançamento em lancamentos MESMO INCOMPLETO, ")
          .append("com null nos campos que não souber (ex.: safraUuid null quando precisar perguntar a safra). ")
          .append("Nunca devolva lancamentos vazio quando houver um lançamento em andamento na conversa.\n")
          .append("- Se o produtor corrigir algo (ex.: \"não, foi 250\"), devolva a proposta inteira de novo, já corrigida.\n")
          .append("- Se a mensagem não for um lançamento (dúvida, saudação, outro assunto), use status SEM_LANCAMENTO e responda em uma ou duas frases, ")
          .append("explicando que você ajuda a registrar despesas e receitas. Não dê conselhos financeiros, jurídicos ou agronômicos.\n")
          .append("- status: PROPOSTA quando todos os lançamentos estiverem completos; PRECISA_INFORMACAO quando faltar algo ")
          .append("(pergunte só o que falta, de forma objetiva, de preferência uma coisa por vez); SEM_LANCAMENTO quando não houver lançamento.\n")
          .append("- resposta: português do Brasil, simples e curta, como numa conversa de WhatsApp. Em PROPOSTA, resuma o que entendeu ")
          .append("e peça confirmação (ex.: \"Entendi: despesa de R$ 200,00 com Combustível em 30/09, safra Soja 2025/2026. Confirma?\").\n\n");

        sb.append("Safras do produtor (uuid | descrição | situação):\n");
        List<ChatMensagemRequest.Safra> safras = limitar(ctx.getSafras());
        if (safras.isEmpty()) sb.append("(nenhuma safra cadastrada — nesse caso explique que é preciso cadastrar uma safra no app antes)\n");
        for (ChatMensagemRequest.Safra s : safras) {
            if (s == null || s.getUuid() == null) continue;
            sb.append(s.getUuid()).append(" | ").append(nn(s.getDescricao())).append(" | ")
              .append(Boolean.TRUE.equals(s.getAtiva()) ? "ativa" : "encerrada").append('\n');
        }
        sb.append("\nMáquinas do produtor (uuid | nome):\n");
        List<ChatMensagemRequest.Maquina> maquinas = limitar(ctx.getMaquinas());
        if (maquinas.isEmpty()) sb.append("(nenhuma)\n");
        for (ChatMensagemRequest.Maquina m : maquinas) {
            if (m == null || m.getUuid() == null) continue;
            sb.append(m.getUuid()).append(" | ").append(nn(m.getNome())).append('\n');
        }
        return sb.toString();
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

        boolean faltaAlgo = false;
        for (JsonNode item : saida.path("lancamentos")) {
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
            if (safra != null && safras.contains(safra)) l.setSafraUuid(safra); else faltando.add("safra");

            l.setCamposFaltando(faltando);
            if (!faltando.isEmpty()) faltaAlgo = true;
            resp.getLancamentos().add(l);
        }

        if (resp.getLancamentos().isEmpty()) {
            // A IA pode estar no meio de uma pergunta sem ter devolvido o lançamento ainda:
            // respeita o PRECISA_INFORMACAO dela (o app só mostra a pergunta).
            String statusIa = saida.path("status").asText("");
            resp.setStatus(PRECISA_INFORMACAO.equals(statusIa) ? PRECISA_INFORMACAO : SEM_LANCAMENTO);
        } else {
            resp.setStatus(faltaAlgo ? PRECISA_INFORMACAO : PROPOSTA);
        }
        return resp;
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

    private static String resumir(String s) {
        if (s == null) return "";
        return s.length() > 300 ? s.substring(0, 300) + "…" : s;
    }

    /** Ferramenta que o modelo é obrigado a usar: define o formato da resposta. */
    private static final String FERRAMENTA_JSON = """
        {
          "name": "responder_produtor",
          "description": "Envia a resposta ao produtor e os lançamentos financeiros (despesas/receitas) identificados na conversa.",
          "input_schema": {
            "type": "object",
            "properties": {
              "status": {
                "type": "string",
                "enum": ["PROPOSTA", "PRECISA_INFORMACAO", "SEM_LANCAMENTO"],
                "description": "PROPOSTA: lançamentos completos, pedir confirmação. PRECISA_INFORMACAO: falta algum dado. SEM_LANCAMENTO: a mensagem não é um lançamento."
              },
              "resposta": {
                "type": "string",
                "description": "Mensagem curta em português do Brasil para mostrar ao produtor."
              },
              "lancamentos": {
                "type": "array",
                "description": "Lançamentos identificados na conversa (vazio se não houver). Em correções, devolver a proposta inteira atualizada.",
                "items": {
                  "type": "object",
                  "properties": {
                    "tipo": {"type": "string", "enum": ["DESPESA", "RECEITA"]},
                    "valor": {"type": ["number", "null"], "description": "Valor em reais, positivo. null se não informado."},
                    "data": {"type": ["string", "null"], "description": "Data no formato AAAA-MM-DD."},
                    "descricao": {"type": ["string", "null"], "description": "Descrição curta do lançamento."},
                    "categoria": {"type": ["string", "null"], "description": "Só para despesa: nome exato de uma categoria da lista. null para receita."},
                    "safraUuid": {"type": ["string", "null"], "description": "uuid de uma safra da lista, ou null se não der para saber."},
                    "maquinaUuid": {"type": ["string", "null"], "description": "uuid de uma máquina da lista, só se o produtor citar uma máquina."}
                  },
                  "required": ["tipo", "valor", "data", "descricao", "categoria", "safraUuid", "maquinaUuid"]
                }
              }
            },
            "required": ["status", "resposta", "lancamentos"]
          }
        }
        """;
}
