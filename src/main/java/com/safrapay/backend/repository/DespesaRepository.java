package com.safrapay.backend.repository;

import com.safrapay.backend.entity.Despesa;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface DespesaRepository extends JpaRepository<Despesa, Long> {

    List<Despesa> findBySafraId(Long safraId);

    List<Despesa> findByMaquinaId(Long maquinaId);

    // Sincronizacao: navega Despesa -> safra -> talhao -> propriedade -> usuario.
    Optional<Despesa> findByUuidAndSafra_Talhao_Propriedade_Usuario_Id(String uuid, Long usuarioId);

    List<Despesa> findBySafra_Talhao_Propriedade_Usuario_Id(Long usuarioId);

    List<Despesa> findBySafra_Talhao_Propriedade_Usuario_IdAndAtualizadoEmAfter(Long usuarioId, Instant desde);
}
