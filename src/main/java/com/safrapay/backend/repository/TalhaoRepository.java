package com.safrapay.backend.repository;

import com.safrapay.backend.entity.Talhao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface TalhaoRepository extends JpaRepository<Talhao, Long> {

    @Query("select x from Talhao x where x.propriedade.id = :propriedadeId and x.excluidoEm is null")
    List<Talhao> findByPropriedadeId(@Param("propriedadeId") Long propriedadeId);

    // "Usuario_Id" navega Talhao -> propriedade -> usuario -> id (propriedades de outro
    // usuario nunca aparecem, mesmo que alguem tente adivinhar o uuid de um talhao alheio).
    Optional<Talhao> findByUuidAndPropriedade_Usuario_Id(String uuid, Long usuarioId);

    @Query("select x from Talhao x where x.propriedade.usuario.id = :usuarioId and x.excluidoEm is null")
    List<Talhao> findByPropriedade_Usuario_Id(@Param("usuarioId") Long usuarioId);

    @Query("select x from Talhao x where x.propriedade.usuario.id = :usuarioId and x.atualizadoEm > :desde and x.excluidoEm is null")
    List<Talhao> findByPropriedade_Usuario_IdAndAtualizadoEmAfter(@Param("usuarioId") Long usuarioId, @Param("desde") Instant desde);
}
