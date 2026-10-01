package com.safrapay.backend.repository;

import com.safrapay.backend.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    /** Encerra todas as sessões ativas do usuário (troca de senha ou suspeita de roubo). */
    @Modifying
    @Query("update RefreshToken r set r.dataRevogacao = :agora where r.usuario.id = :usuarioId and r.dataRevogacao is null")
    int revogarTodos(@Param("usuarioId") Long usuarioId, @Param("agora") Instant agora);

    /** Limpeza: apaga os tokens já vencidos do usuário (roda a cada login). */
    @Modifying
    @Query("delete from RefreshToken r where r.usuario.id = :usuarioId and r.dataExpiracao < :agora")
    int apagarVencidos(@Param("usuarioId") Long usuarioId, @Param("agora") Instant agora);
}
