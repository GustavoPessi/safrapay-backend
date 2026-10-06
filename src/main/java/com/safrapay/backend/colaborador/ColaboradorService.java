package com.safrapay.backend.colaborador;

import com.safrapay.backend.colaborador.dto.MaquinaColaboradorResponse;
import com.safrapay.backend.colaborador.dto.ReportarRequest;
import com.safrapay.backend.colaborador.dto.TarefaResponse;
import com.safrapay.backend.entity.AnotacaoMaquina;
import com.safrapay.backend.entity.Maquina;
import com.safrapay.backend.entity.Usuario;
import com.safrapay.backend.repository.AnotacaoMaquinaRepository;
import com.safrapay.backend.repository.MaquinaRepository;
import com.safrapay.backend.security.PapelAcesso;
import com.safrapay.backend.security.SecurityUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Predicate;

/**
 * Parte online do colaborador (multiusuário, 06/10/2026): máquinas da fazenda
 * do dono e as tarefas (anotações de máquina) que ele pode ver — as atribuídas
 * a ele e as sem responsável. Dono e gerente também podem chamar estas rotas e
 * veem todas as tarefas (mas no dia a dia usam o app offline + sync).
 */
@Service
public class ColaboradorService {

    /** "Hoje" do produtor, pra calcular atrasadas e a data do problema reportado. */
    static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");
    private static final int TEXTO_MAX = 255;

    private final MaquinaRepository maquinaRepository;
    private final AnotacaoMaquinaRepository anotacaoRepository;

    public ColaboradorService(MaquinaRepository maquinaRepository, AnotacaoMaquinaRepository anotacaoRepository) {
        this.maquinaRepository = maquinaRepository;
        this.anotacaoRepository = anotacaoRepository;
    }

    public enum Filtro { TODAS, PENDENTES, ATRASADAS, FEITAS }

    @Transactional(readOnly = true)
    public List<MaquinaColaboradorResponse> maquinas() {
        Usuario dono = SecurityUtils.contaDados();
        LocalDate hoje = hoje();
        Map<Long, int[]> contagem = new HashMap<>();
        for (AnotacaoMaquina a : anotacaoRepository.tarefasDoDono(dono.getId())) {
            if (!visivel(a) || a.isConcluida()) continue;
            int[] c = contagem.computeIfAbsent(a.getMaquina().getId(), k -> new int[2]);
            c[0]++;
            if (a.isAtrasada(hoje)) c[1]++;
        }
        return maquinaRepository.findByUsuarioId(dono.getId()).stream()
                .sorted(Comparator.comparing(m -> m.getNome() == null ? "" : m.getNome().toLowerCase(Locale.ROOT)))
                .map(m -> {
                    int[] c = contagem.getOrDefault(m.getId(), new int[2]);
                    return MaquinaColaboradorResponse.de(m, c[0], c[1]);
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TarefaResponse> tarefas(String filtroTexto, String maquinaUuid) {
        Usuario dono = SecurityUtils.contaDados();
        Usuario eu = SecurityUtils.usuarioLogado();
        LocalDate hoje = hoje();
        Filtro filtro = lerFiltro(filtroTexto);

        Long maquinaId = null;
        if (maquinaUuid != null && !maquinaUuid.isBlank()) {
            maquinaId = buscarMaquina(maquinaUuid, dono).getId();
        }
        final Long idFiltro = maquinaId;

        Predicate<AnotacaoMaquina> doFiltro = switch (filtro) {
            case TODAS -> a -> true;
            case PENDENTES -> a -> !a.isConcluida();
            case ATRASADAS -> a -> a.isAtrasada(hoje);
            case FEITAS -> AnotacaoMaquina::isConcluida;
        };

        return anotacaoRepository.tarefasDoDono(dono.getId()).stream()
                .filter(this::visivel)
                .filter(a -> idFiltro == null || a.getMaquina().getId().equals(idFiltro))
                .filter(doFiltro)
                .sorted(ordem(hoje))
                .map(a -> TarefaResponse.de(a, eu, hoje))
                .toList();
    }

    @Transactional
    public TarefaResponse concluir(String uuid) {
        Usuario eu = SecurityUtils.usuarioLogado();
        AnotacaoMaquina a = buscarTarefa(uuid);
        if (!a.isConcluida()) {
            a.definirConcluida(true, eu);
            anotacaoRepository.save(a);
        }
        return TarefaResponse.de(a, eu, hoje());
    }

    /** Desfaz a conclusão. Colaborador só reabre o que ele mesmo concluiu. */
    @Transactional
    public TarefaResponse reabrir(String uuid) {
        Usuario eu = SecurityUtils.usuarioLogado();
        AnotacaoMaquina a = buscarTarefa(uuid);
        if (a.isConcluida()) {
            boolean fuiEu = a.getConcluidoPor() != null && a.getConcluidoPor().getId().equals(eu.getId());
            if (SecurityUtils.papel() == PapelAcesso.COLABORADOR && !fuiEu) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Só quem concluiu a tarefa pode reabri-la.");
            }
            a.definirConcluida(false, eu);
            anotacaoRepository.save(a);
        }
        return TarefaResponse.de(a, eu, hoje());
    }

    /** Reportar problema: vira tarefa pendente, sem responsável, com data de hoje. */
    @Transactional
    public TarefaResponse reportar(String maquinaUuid, ReportarRequest request) {
        Usuario dono = SecurityUtils.contaDados();
        Usuario eu = SecurityUtils.usuarioLogado();
        Maquina maquina = buscarMaquina(maquinaUuid, dono);
        String texto = request == null || request.getTexto() == null ? "" : request.getTexto().trim();
        if (texto.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Descreva o problema.");
        }
        if (texto.length() > TEXTO_MAX) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Descrição muito longa (máximo " + TEXTO_MAX + " caracteres).");
        }
        AnotacaoMaquina a = new AnotacaoMaquina();
        a.setMaquina(maquina);
        a.setTexto(texto);
        a.setData(hoje());
        a.setConcluida(false);
        a.setCriadoPor(eu);
        anotacaoRepository.save(a);
        return TarefaResponse.de(a, eu, hoje());
    }

    // ------------------------------------------------------------------

    /** Colaborador vê as tarefas dele e as sem responsável; dono/gerente veem todas. */
    private boolean visivel(AnotacaoMaquina a) {
        if (SecurityUtils.papel() != PapelAcesso.COLABORADOR) return true;
        Usuario eu = SecurityUtils.usuarioLogado();
        return a.getResponsavel() == null || a.getResponsavel().getId().equals(eu.getId());
    }

    private AnotacaoMaquina buscarTarefa(String uuid) {
        Usuario dono = SecurityUtils.contaDados();
        return anotacaoRepository.findByUuidAndMaquina_Usuario_Id(uuid, dono.getId())
                .filter(this::visivel)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tarefa não encontrada."));
    }

    private Maquina buscarMaquina(String uuid, Usuario dono) {
        return maquinaRepository.findByUuidAndUsuarioId(uuid, dono.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Máquina não encontrada."));
    }

    private Filtro lerFiltro(String valor) {
        if (valor == null || valor.isBlank()) return Filtro.TODAS;
        try {
            return Filtro.valueOf(valor.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Filtro inválido: use TODAS, PENDENTES, ATRASADAS ou FEITAS.");
        }
    }

    /**
     * Atrasadas primeiro, depois as outras pendentes, depois as feitas.
     * Pendentes: prazo mais próximo primeiro (sem prazo no fim). Feitas: as
     * concluídas mais recentemente primeiro. Desempate: mais recentes primeiro.
     */
    static Comparator<AnotacaoMaquina> ordem(LocalDate hoje) {
        Comparator<AnotacaoMaquina> grupo = Comparator.comparingInt(a -> a.isAtrasada(hoje) ? 0 : a.isConcluida() ? 2 : 1);
        Comparator<AnotacaoMaquina> dentro = (x, y) -> {
            if (!x.isConcluida()) {
                return Comparator.nullsLast(Comparator.<LocalDate>naturalOrder()).compare(x.getPrazo(), y.getPrazo());
            }
            return Comparator.nullsLast(Comparator.<Instant>reverseOrder()).compare(x.getConcluidoEm(), y.getConcluidoEm());
        };
        Comparator<AnotacaoMaquina> recentes = Comparator.comparing(AnotacaoMaquina::getCriadoEm,
                Comparator.nullsLast(Comparator.<Instant>reverseOrder()));
        return grupo.thenComparing(dentro).thenComparing(recentes);
    }

    static LocalDate hoje() {
        return LocalDate.now(FUSO);
    }
}
