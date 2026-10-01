package com.safrapay.backend.auth;

import com.safrapay.backend.entity.RefreshToken;
import com.safrapay.backend.entity.Usuario;
import com.safrapay.backend.repository.RefreshTokenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;

/**
 * Refresh token do login (01/10/2026).
 *
 * - O login devolve, além do JWT de acesso (24h), um refresh token aleatório
 *   de 256 bits que vale {@code jwt.refresh-expiration-dias} (padrão 90).
 * - POST /api/auth/refresh troca o refresh token por um JWT novo e um refresh
 *   token novo (rotação: o antigo é revogado). Quem usa o app pelo menos uma
 *   vez a cada 90 dias nunca precisa digitar a senha de novo.
 * - No banco fica só o hash SHA-256.
 * - Sinal ruim no campo: se a resposta do refresh se perder e o app repetir o
 *   token antigo em até 2 minutos, emite outro em vez de derrubar a sessão.
 * - Depois disso, reuso de um token já trocado indica cópia/roubo: todas as
 *   sessões do usuário são encerradas (ele precisa entrar com a senha).
 */
@Service
public class RefreshTokenService {

    private static final Logger log = LoggerFactory.getLogger(RefreshTokenService.class);
    static final Duration TOLERANCIA_REUSO = Duration.ofMinutes(2);
    private static final SecureRandom ALEATORIO = new SecureRandom();
    private static final String SESSAO_EXPIRADA = "Sessao expirada. Faca login novamente";

    private final RefreshTokenRepository repository;
    private final Duration validade;
    private final Clock relogio;

    @Autowired
    public RefreshTokenService(RefreshTokenRepository repository,
                               @Value("${jwt.refresh-expiration-dias:90}") long diasValidade) {
        this(repository, diasValidade, Clock.systemUTC());
    }

    RefreshTokenService(RefreshTokenRepository repository, long diasValidade, Clock relogio) {
        this.repository = repository;
        this.validade = Duration.ofDays(diasValidade);
        this.relogio = relogio;
    }

    /** Resultado do refresh: de quem é a sessão e o refresh token novo. */
    public record Renovacao(Usuario usuario, String refreshToken) {}

    /** Cria um refresh token novo para o usuário (no login). */
    @Transactional
    public String emitir(Usuario usuario) {
        Instant agora = relogio.instant();
        repository.apagarVencidos(usuario.getId(), agora);
        return criar(usuario, agora);
    }

    /**
     * Troca o refresh token por um novo (rotação). 401 se não existir, estiver
     * vencido ou revogado. noRollbackFor: a revogação geral (suspeita de roubo)
     * precisa ficar gravada mesmo com o 401.
     */
    @Transactional(noRollbackFor = ResponseStatusException.class)
    public Renovacao renovar(String bruto) {
        if (bruto == null || bruto.isBlank()) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, SESSAO_EXPIRADA);
        RefreshToken atual = repository.findByTokenHash(hash(bruto.trim()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, SESSAO_EXPIRADA));
        Instant agora = relogio.instant();
        Usuario usuario = atual.getUsuario();

        if (!atual.getDataExpiracao().isAfter(agora)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, SESSAO_EXPIRADA);
        }
        if (atual.getDataRevogacao() != null) {
            if (atual.getSubstituidoPorHash() == null) {
                // encerrado por logout ou troca de senha
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, SESSAO_EXPIRADA);
            }
            if (atual.getDataRevogacao().plus(TOLERANCIA_REUSO).isAfter(agora)) {
                // a resposta do refresh anterior provavelmente se perdeu: emite outro
                return new Renovacao(usuario, criar(usuario, agora));
            }
            int n = repository.revogarTodos(usuario.getId(), agora);
            log.warn("Refresh token reutilizado depois da rotacao (usuario {}): {} sessao(oes) encerrada(s)", usuario.getId(), n);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, SESSAO_EXPIRADA);
        }

        String novo = criar(usuario, agora);
        atual.setDataRevogacao(agora);
        atual.setSubstituidoPorHash(hash(novo));
        repository.save(atual);
        return new Renovacao(usuario, novo);
    }

    /** Logout: encerra só esta sessão. Token desconhecido é ignorado (logout sempre "funciona"). */
    @Transactional
    public void revogar(String bruto) {
        if (bruto == null || bruto.isBlank()) return;
        repository.findByTokenHash(hash(bruto.trim())).ifPresent(t -> {
            if (t.getDataRevogacao() == null) {
                t.setDataRevogacao(relogio.instant());
                repository.save(t);
            }
        });
    }

    /** Troca/redefinição de senha: encerra todas as sessões do usuário. */
    @Transactional
    public void revogarTodos(Usuario usuario) {
        repository.revogarTodos(usuario.getId(), relogio.instant());
    }

    private String criar(Usuario usuario, Instant agora) {
        byte[] bytes = new byte[32];
        ALEATORIO.nextBytes(bytes);
        String bruto = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        RefreshToken t = new RefreshToken();
        t.setUsuario(usuario);
        t.setTokenHash(hash(bruto));
        t.setDataCriacao(agora);
        t.setDataExpiracao(agora.plus(validade));
        repository.save(t);
        return bruto;
    }

    static String hash(String bruto) {
        try {
            MessageDigest sha = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(sha.digest(bruto.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
