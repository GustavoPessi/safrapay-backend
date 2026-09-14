package com.safrapay.backend.repository;

import com.safrapay.backend.entity.Talhao;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface TalhaoRepository extends JpaRepository<Talhao, Long> {

    List<Talhao> findByPropriedadeId(Long propriedadeId);

    // "Usuario_Id" navega Talhao -> propriedade -> usuario -> id (propriedades de outro
    // usuario nunca aparecem, mesmo que alguem tente adivinhar o uuid de um talhao alheio).
    Optional<Talhao> findByUuidAndPropriedade_Usuario_Id(String uuid, Long usuarioId);

    List<Talhao> findByPropriedade_Usuario_Id(Long usuarioId);

    List<Talhao> findByPropriedade_Usuario_IdAndAtualizadoEmAfter(Long usuarioId, Instant desde);
}
