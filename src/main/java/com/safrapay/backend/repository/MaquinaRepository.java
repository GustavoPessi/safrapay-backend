package com.safrapay.backend.repository;

import com.safrapay.backend.entity.Maquina;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface MaquinaRepository extends JpaRepository<Maquina, Long> {

    @Query("select x from Maquina x where x.usuario.id = :usuarioId and x.excluidoEm is null")
    List<Maquina> findByUsuarioId(@Param("usuarioId") Long usuarioId);

    // Usado pela sincronizacao de Despesa (maquina opcional, viaja como uuid).
    Optional<Maquina> findByUuidAndUsuarioId(String uuid, Long usuarioId);

    // Sincronizacao (Fase 4).
    @Query("select x from Maquina x where x.usuario.id = :usuarioId and x.atualizadoEm > :desde and x.excluidoEm is null")
    List<Maquina> findByUsuarioIdAndAtualizadoEmAfter(@Param("usuarioId") Long usuarioId, @Param("desde") Instant desde);
}
