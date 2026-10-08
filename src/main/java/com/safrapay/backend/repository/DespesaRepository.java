package com.safrapay.backend.repository;

import com.safrapay.backend.entity.Despesa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface DespesaRepository extends JpaRepository<Despesa, Long> {

    @Query("select x from Despesa x where x.safra.id = :safraId and x.excluidoEm is null")
    List<Despesa> findBySafraId(@Param("safraId") Long safraId);

    @Query("select x from Despesa x where x.maquina.id = :maquinaId and x.excluidoEm is null")
    List<Despesa> findByMaquinaId(@Param("maquinaId") Long maquinaId);

    // Sincronizacao: navega Despesa -> safra -> talhao -> propriedade -> usuario.
    Optional<Despesa> findByUuidAndSafra_Talhao_Propriedade_Usuario_Id(String uuid, Long usuarioId);

    @Query("select x from Despesa x where x.safra.talhao.propriedade.usuario.id = :usuarioId and x.excluidoEm is null")
    List<Despesa> findBySafra_Talhao_Propriedade_Usuario_Id(@Param("usuarioId") Long usuarioId);

    @Query("select x from Despesa x where x.safra.talhao.propriedade.usuario.id = :usuarioId and x.atualizadoEm > :desde and x.excluidoEm is null")
    List<Despesa> findBySafra_Talhao_Propriedade_Usuario_IdAndAtualizadoEmAfter(@Param("usuarioId") Long usuarioId, @Param("desde") Instant desde);
}
