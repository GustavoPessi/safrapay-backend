package com.safrapay.backend.repository;

import com.safrapay.backend.entity.Safra;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface SafraRepository extends JpaRepository<Safra, Long> {

    @Query("select x from Safra x where x.talhao.id = :talhaoId and x.excluidoEm is null")
    List<Safra> findByTalhaoId(@Param("talhaoId") Long talhaoId);

    // "Talhao_Propriedade_Usuario_Id" navega Safra -> talhao -> propriedade -> usuario -> id
    // (mesmo padrao do TalhaoRepository: safras de outro usuario nunca aparecem, mesmo
    // que alguem tente adivinhar o uuid de uma safra alheia).
    Optional<Safra> findByUuidAndTalhao_Propriedade_Usuario_Id(String uuid, Long usuarioId);

    @Query("select x from Safra x where x.talhao.propriedade.usuario.id = :usuarioId and x.excluidoEm is null")
    List<Safra> findByTalhao_Propriedade_Usuario_Id(@Param("usuarioId") Long usuarioId);

    @Query("select x from Safra x where x.talhao.propriedade.usuario.id = :usuarioId and x.atualizadoEm > :desde and x.excluidoEm is null")
    List<Safra> findByTalhao_Propriedade_Usuario_IdAndAtualizadoEmAfter(@Param("usuarioId") Long usuarioId, @Param("desde") Instant desde);
}
