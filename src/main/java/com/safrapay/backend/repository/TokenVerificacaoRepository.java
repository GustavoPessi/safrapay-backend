package com.safrapay.backend.repository;

import com.safrapay.backend.entity.TokenVerificacao;
import com.safrapay.backend.entity.TipoToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TokenVerificacaoRepository extends JpaRepository<TokenVerificacao, Long> {
    Optional<TokenVerificacao> findByTokenAndTipo(String token, TipoToken tipo);
}
