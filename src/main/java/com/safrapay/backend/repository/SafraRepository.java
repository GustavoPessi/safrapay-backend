package com.safrapay.backend.repository;

import com.safrapay.backend.entity.Safra;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface SafraRepository extends JpaRepository<Safra, Long> {

    List<Safra> findByTalhaoId(Long talhaoId);

    // "Talhao_Propriedade_Usuario_Id" navega Safra -> talhao -> propriedade -> usuario -> id
    // (mesmo padrao do TalhaoRepository: safras de outro usuario nunca aparecem, mesmo
    // que alguem tente adivinhar o uuid de uma safra alheia).
    Optional<Safra> findByUuidAndTalhao_Propriedade_Usuario_Id(String uuid, Long usuarioId);

    List<Safra> findByTalhao_Propriedade_Usuario_Id(Long usuarioId);

    List<Safra> findByTalhao_Propriedade_Usuario_IdAndAtualizadoEmAfter(Long usuarioId, Instant desde);
}
