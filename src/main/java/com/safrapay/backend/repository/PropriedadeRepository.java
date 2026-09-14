package com.safrapay.backend.repository;

import com.safrapay.backend.entity.Propriedade;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface PropriedadeRepository extends JpaRepository<Propriedade, Long> {

    List<Propriedade> findByUsuarioId(Long usuarioId);

    Optional<Propriedade> findByUuidAndUsuarioId(String uuid, Long usuarioId);

    List<Propriedade> findByUsuarioIdAndAtualizadoEmAfter(Long usuarioId, Instant desde);
}
