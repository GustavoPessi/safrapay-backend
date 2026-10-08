package com.safrapay.backend.repository;

import com.safrapay.backend.entity.Propriedade;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface PropriedadeRepository extends JpaRepository<Propriedade, Long> {

    @Query("select x from Propriedade x where x.usuario.id = :usuarioId and x.excluidoEm is null")
    List<Propriedade> findByUsuarioId(@Param("usuarioId") Long usuarioId);

    Optional<Propriedade> findByUuidAndUsuarioId(String uuid, Long usuarioId);

    @Query("select x from Propriedade x where x.usuario.id = :usuarioId and x.atualizadoEm > :desde and x.excluidoEm is null")
    List<Propriedade> findByUsuarioIdAndAtualizadoEmAfter(@Param("usuarioId") Long usuarioId, @Param("desde") Instant desde);
}
