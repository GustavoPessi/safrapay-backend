package com.safrapay.backend.repository;

import com.safrapay.backend.entity.MetaFinanceira;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface MetaFinanceiraRepository extends JpaRepository<MetaFinanceira, Long> {

    List<MetaFinanceira> findBySafraId(Long safraId);

    // Sincronizacao: navega MetaFinanceira -> safra -> talhao -> propriedade -> usuario.
    Optional<MetaFinanceira> findByUuidAndSafra_Talhao_Propriedade_Usuario_Id(String uuid, Long usuarioId);

    List<MetaFinanceira> findBySafra_Talhao_Propriedade_Usuario_Id(Long usuarioId);

    List<MetaFinanceira> findBySafra_Talhao_Propriedade_Usuario_IdAndAtualizadoEmAfter(Long usuarioId, Instant desde);
}
