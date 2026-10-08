package com.safrapay.backend.repository;

import com.safrapay.backend.entity.MetaFinanceira;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface MetaFinanceiraRepository extends JpaRepository<MetaFinanceira, Long> {

    @Query("select x from MetaFinanceira x where x.safra.id = :safraId and x.excluidoEm is null")
    List<MetaFinanceira> findBySafraId(@Param("safraId") Long safraId);

    // Sincronizacao: navega MetaFinanceira -> safra -> talhao -> propriedade -> usuario.
    Optional<MetaFinanceira> findByUuidAndSafra_Talhao_Propriedade_Usuario_Id(String uuid, Long usuarioId);

    @Query("select x from MetaFinanceira x where x.safra.talhao.propriedade.usuario.id = :usuarioId and x.excluidoEm is null")
    List<MetaFinanceira> findBySafra_Talhao_Propriedade_Usuario_Id(@Param("usuarioId") Long usuarioId);

    @Query("select x from MetaFinanceira x where x.safra.talhao.propriedade.usuario.id = :usuarioId and x.atualizadoEm > :desde and x.excluidoEm is null")
    List<MetaFinanceira> findBySafra_Talhao_Propriedade_Usuario_IdAndAtualizadoEmAfter(@Param("usuarioId") Long usuarioId, @Param("desde") Instant desde);
}
