package com.safrapay.backend.auth;

import com.safrapay.backend.entity.TipoToken;
import com.safrapay.backend.entity.TokenVerificacao;
import com.safrapay.backend.entity.Usuario;
import com.safrapay.backend.repository.TokenVerificacaoRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
public class TokenService {

    private static final long HORAS_EXPIRACAO_CONFIRMACAO = 24;
    private static final long HORAS_EXPIRACAO_RESET = 1;

    private final TokenVerificacaoRepository repository;

    public TokenService(TokenVerificacaoRepository repository) {
        this.repository = repository;
    }

    public String gerarTokenConfirmacaoEmail(Usuario usuario) {
        return gerarToken(usuario, TipoToken.CONFIRMACAO_EMAIL, HORAS_EXPIRACAO_CONFIRMACAO);
    }

    public String gerarTokenResetSenha(Usuario usuario) {
        return gerarToken(usuario, TipoToken.RESET_SENHA, HORAS_EXPIRACAO_RESET);
    }

    private String gerarToken(Usuario usuario, TipoToken tipo, long horasValidade) {
        TokenVerificacao t = new TokenVerificacao();
        t.setUsuario(usuario);
        t.setToken(UUID.randomUUID().toString());
        t.setTipo(tipo);
        t.setDataExpiracao(Instant.now().plus(horasValidade, ChronoUnit.HOURS));
        repository.save(t);
        return t.getToken();
    }

    /** Valida o token, marca como usado e devolve o usuario associado. */
    public Usuario consumirToken(String token, TipoToken tipo) {
        TokenVerificacao t = repository.findByTokenAndTipo(token, tipo)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Token invalido"));

        if (t.isUsado()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Token ja foi utilizado");
        }
        if (t.getDataExpiracao().isBefore(Instant.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Token expirado");
        }

        t.setUsado(true);
        repository.save(t);
        return t.getUsuario();
    }
}
