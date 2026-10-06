package com.safrapay.backend.equipe;

import com.safrapay.backend.email.EmailService;
import com.safrapay.backend.entity.AnotacaoMaquina;
import com.safrapay.backend.entity.EquipeMembro;
import com.safrapay.backend.entity.PapelEquipe;
import com.safrapay.backend.entity.StatusMembro;
import com.safrapay.backend.entity.Usuario;
import com.safrapay.backend.equipe.dto.ConviteRecebidoResponse;
import com.safrapay.backend.equipe.dto.ConviteRequest;
import com.safrapay.backend.equipe.dto.EquipeMembroResponse;
import com.safrapay.backend.equipe.dto.MinhaEquipeResponse;
import com.safrapay.backend.equipe.dto.PapelRequest;
import com.safrapay.backend.repository.AnotacaoMaquinaRepository;
import com.safrapay.backend.repository.EquipeMembroRepository;
import com.safrapay.backend.repository.UsuarioRepository;
import com.safrapay.backend.security.PapelAcesso;
import com.safrapay.backend.security.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Equipe da fazenda (multiusuário, 06/10/2026 — contrato no Notion).
 *
 * Quem não é membro de nenhuma equipe é DONO dos próprios dados e pode
 * convidar GERENTES e COLABORADORES por e-mail. Quem aceita um convite passa a
 * trabalhar nos dados do dono (ver UsuarioAutenticadoService) até sair ou ser
 * removido. Uma pessoa participa de no máximo uma equipe.
 */
@Service
public class EquipeService {

    private static final Logger log = LoggerFactory.getLogger(EquipeService.class);

    /** Convites ativos (pendentes + aceitos) por dono. */
    static final int LIMITE_EQUIPE = 20;
    /** Recusados aparecem na lista do dono por este tempo. */
    private static final Duration RECUSADOS_VISIVEIS = Duration.ofDays(30);
    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private final EquipeMembroRepository repository;
    private final UsuarioRepository usuarioRepository;
    private final AnotacaoMaquinaRepository anotacaoRepository;
    private final EmailService emailService;

    public EquipeService(EquipeMembroRepository repository, UsuarioRepository usuarioRepository,
                         AnotacaoMaquinaRepository anotacaoRepository, EmailService emailService) {
        this.repository = repository;
        this.usuarioRepository = usuarioRepository;
        this.anotacaoRepository = anotacaoRepository;
        this.emailService = emailService;
    }

    // ------------------------------------------------------------------
    // Qualquer papel
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public MinhaEquipeResponse minha() {
        return montarMinha(SecurityUtils.usuarioLogado());
    }

    @Transactional(readOnly = true)
    public List<ConviteRecebidoResponse> recebidos() {
        Usuario eu = SecurityUtils.usuarioLogado();
        return repository.convitesPendentesPara(eu.getId(), eu.getEmail()).stream()
                .map(ConviteRecebidoResponse::de).toList();
    }

    @Transactional
    public MinhaEquipeResponse aceitar(String uuid) {
        Usuario eu = SecurityUtils.usuarioLogado();
        EquipeMembro convite = conviteParaMim(uuid, eu);

        if (repository.buscarParticipacaoAceita(eu.getId()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Você já faz parte de outra equipe. Saia da equipe atual antes de aceitar.");
        }
        if (repository.contarAceitosDoDono(eu.getId()) > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Você tem membros na sua equipe. Remova seus membros antes de entrar em outra equipe.");
        }
        if (convite.getDono().getId().equals(eu.getId())
                || repository.buscarParticipacaoAceita(convite.getDono().getId()).isPresent()) {
            // Dono que virou membro de outra equipe não pode ter equipe própria.
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Este convite não é mais válido.");
        }

        // Quem entra numa equipe deixa de ser dono: cancela os convites que tinha enviado.
        for (EquipeMembro enviado : repository.listarDaEquipe(eu.getId(), EnumSet.of(StatusMembro.PENDENTE))) {
            enviado.setStatus(StatusMembro.CANCELADO);
        }

        convite.setMembro(eu);
        convite.setStatus(StatusMembro.ACEITO);
        convite.setRespondidoEm(Instant.now());
        try {
            repository.saveAndFlush(convite);
        } catch (DataIntegrityViolationException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Você já faz parte de outra equipe. Saia da equipe atual antes de aceitar.");
        }
        return montarMinha(eu);
    }

    @Transactional
    public void recusar(String uuid) {
        Usuario eu = SecurityUtils.usuarioLogado();
        EquipeMembro convite = conviteParaMim(uuid, eu);
        if (convite.getMembro() == null) convite.setMembro(eu);
        convite.setStatus(StatusMembro.RECUSADO);
        convite.setRespondidoEm(Instant.now());
        repository.save(convite);
    }

    /** Gerente/colaborador sai da equipe e volta a ser DONO dos próprios dados. */
    @Transactional
    public MinhaEquipeResponse sair() {
        Usuario eu = SecurityUtils.usuarioLogado();
        EquipeMembro participacao = repository.buscarParticipacaoAceita(eu.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "Você não faz parte de uma equipe."));
        participacao.setStatus(StatusMembro.SAIU);
        participacao.setRespondidoEm(Instant.now());
        repository.saveAndFlush(participacao);
        liberarTarefas(participacao.getDono().getId(), eu.getId());
        return montarMinha(eu);
    }

    // ------------------------------------------------------------------
    // Só o dono
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<EquipeMembroResponse> listar() {
        Usuario dono = exigirDono();
        Instant limiteRecusados = Instant.now().minus(RECUSADOS_VISIVEIS);
        return repository.listarDaEquipe(dono.getId(),
                        EnumSet.of(StatusMembro.PENDENTE, StatusMembro.ACEITO, StatusMembro.RECUSADO)).stream()
                .filter(e -> e.getStatus() != StatusMembro.RECUSADO
                        || (e.getRespondidoEm() != null && e.getRespondidoEm().isAfter(limiteRecusados)))
                .map(EquipeMembroResponse::de)
                .toList();
    }

    @Transactional
    public EquipeMembroResponse convidar(ConviteRequest request) {
        Usuario dono = exigirDono();
        String email = request == null || request.getEmail() == null ? "" : request.getEmail().trim();
        if (!EMAIL.matcher(email).matches() || email.length() > 255) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "E-mail inválido.");
        }
        PapelEquipe papel = lerPapel(request.getPapel());
        if (email.equalsIgnoreCase(dono.getEmail())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Você não pode convidar o seu próprio e-mail.");
        }
        if (repository.buscarAtivo(dono.getId(), email).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Já existe um convite ativo para este e-mail.");
        }
        if (repository.contarAtivosDoDono(dono.getId()) >= LIMITE_EQUIPE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Sua equipe chegou ao limite de " + LIMITE_EQUIPE + " pessoas (contando convites pendentes).");
        }

        Usuario convidado = usuarioRepository.findFirstByEmailIgnoreCase(email).orElse(null);
        EquipeMembro convite = new EquipeMembro();
        convite.setDono(dono);
        convite.setMembro(convidado);
        convite.setEmail(convidado != null ? convidado.getEmail() : email.toLowerCase(Locale.ROOT));
        convite.setPapel(papel);
        convite.setStatus(StatusMembro.PENDENTE);
        try {
            repository.saveAndFlush(convite);
        } catch (DataIntegrityViolationException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Já existe um convite ativo para este e-mail.");
        }

        try {
            emailService.enviarConviteEquipe(convite.getEmail(), dono.getNome(), papel.name(), convidado != null);
        } catch (RuntimeException e) {
            // O convite vale mesmo sem o e-mail (aparece no app do convidado).
            log.error("Falha ao enviar convite de equipe para {}: {}", convite.getEmail(), e.getMessage());
        }
        return EquipeMembroResponse.de(convite);
    }

    @Transactional
    public EquipeMembroResponse alterarPapel(String uuid, PapelRequest request) {
        Usuario dono = exigirDono();
        EquipeMembro membro = membroDaMinhaEquipe(uuid, dono);
        membro.setPapel(lerPapel(request == null ? null : request.getPapel()));
        repository.save(membro);
        return EquipeMembroResponse.de(membro);
    }

    /** Pendente vira CANCELADO; aceito vira REMOVIDO (e as tarefas pendentes dele ficam sem responsável). */
    @Transactional
    public void remover(String uuid) {
        Usuario dono = exigirDono();
        EquipeMembro membro = membroDaMinhaEquipe(uuid, dono);
        if (membro.getStatus() == StatusMembro.PENDENTE) {
            membro.setStatus(StatusMembro.CANCELADO);
            repository.save(membro);
        } else {
            membro.setStatus(StatusMembro.REMOVIDO);
            repository.saveAndFlush(membro);
            liberarTarefas(dono.getId(), membro.getMembro().getId());
        }
    }

    // ------------------------------------------------------------------
    // Usado por outras partes (sync de anotação)
    // ------------------------------------------------------------------

    /** Dono + membros aceitos: quem pode ser responsável por tarefa na fazenda do dono. */
    @Transactional(readOnly = true)
    public List<Usuario> pessoasDaFazenda(Usuario dono) {
        List<Usuario> pessoas = new ArrayList<>();
        pessoas.add(dono);
        for (EquipeMembro e : repository.listarDaEquipe(dono.getId(), EnumSet.of(StatusMembro.ACEITO))) {
            if (e.getMembro() != null) pessoas.add(e.getMembro());
        }
        return pessoas;
    }

    // ------------------------------------------------------------------

    private MinhaEquipeResponse montarMinha(Usuario eu) {
        MinhaEquipeResponse r = new MinhaEquipeResponse();
        EquipeMembro participacao = repository.buscarParticipacaoAceita(eu.getId()).orElse(null);
        Usuario dono;
        if (participacao == null) {
            r.setPapel(PapelAcesso.DONO.name());
            dono = eu;
        } else {
            r.setPapel(participacao.getPapel().name());
            dono = participacao.getDono();
            r.setDono(new MinhaEquipeResponse.Pessoa(dono.getUuid(), dono.getNome(), PapelAcesso.DONO.name()));
        }
        r.getPessoas().add(new MinhaEquipeResponse.Pessoa(dono.getUuid(), dono.getNome(), PapelAcesso.DONO.name()));
        for (EquipeMembro e : repository.listarDaEquipe(dono.getId(), EnumSet.of(StatusMembro.ACEITO))) {
            if (e.getMembro() == null) continue;
            r.getPessoas().add(new MinhaEquipeResponse.Pessoa(e.getMembro().getUuid(), e.getMembro().getNome(), e.getPapel().name()));
        }
        r.setConvitesPendentes(repository.convitesPendentesPara(eu.getId(), eu.getEmail()).size());
        return r;
    }

    /** Ações de gestão: só quem é DONO (gerente → 403; colaborador já é barrado no SecurityConfig). */
    private Usuario exigirDono() {
        if (SecurityUtils.papel() != PapelAcesso.DONO) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Só o dono da fazenda pode gerenciar a equipe.");
        }
        return SecurityUtils.usuarioLogado();
    }

    /** Convite pendente endereçado a quem está logado (pela conta ou pelo e-mail). */
    private EquipeMembro conviteParaMim(String uuid, Usuario eu) {
        EquipeMembro convite = repository.findByUuid(uuid).orElse(null);
        boolean paraMim = convite != null
                && (convite.getMembro() != null
                    ? convite.getMembro().getId().equals(eu.getId())
                    : convite.getEmail().equalsIgnoreCase(eu.getEmail()));
        if (!paraMim) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Convite não encontrado.");
        if (convite.getStatus() != StatusMembro.PENDENTE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Este convite não está mais pendente.");
        }
        return convite;
    }

    /** Convite pendente ou membro aceito da equipe do dono. */
    private EquipeMembro membroDaMinhaEquipe(String uuid, Usuario dono) {
        EquipeMembro e = repository.findByUuid(uuid)
                .filter(m -> m.getDono().getId().equals(dono.getId()))
                .filter(m -> m.getStatus() == StatusMembro.PENDENTE || m.getStatus() == StatusMembro.ACEITO)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Membro não encontrado."));
        return e;
    }

    private PapelEquipe lerPapel(String valor) {
        try {
            return PapelEquipe.valueOf(valor == null ? "" : valor.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Papel inválido: use GERENTE ou COLABORADOR.");
        }
    }

    /** Tarefas pendentes atribuídas a quem saiu/foi removido voltam a ficar sem responsável. */
    private void liberarTarefas(Long donoId, Long membroId) {
        for (AnotacaoMaquina a : anotacaoRepository.tarefasDoDono(donoId)) {
            if (!a.isConcluida() && a.getResponsavel() != null && a.getResponsavel().getId().equals(membroId)) {
                a.setResponsavel(null);
                anotacaoRepository.save(a);
            }
        }
    }
}
