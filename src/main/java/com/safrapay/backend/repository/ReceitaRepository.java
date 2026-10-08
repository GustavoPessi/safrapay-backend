package com.safrapay.backend.repository;

import com.safrapay.backend.entity.Receita;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface ReceitaRepository extends JpaRepository<Receita, Long> {

    @Query("select x from Receita x where x.safra.id = :safraId and x.excluidoEm is null")
    List<Receita> findBySafraId(@Param("safraId") Long safraId);

    // Sincronizacao: navega Receita -> safra -> talhao -> propriedade -> usuario
    // (mesmo padrao de SafraRepository/TalhaoRepository).
    Optional<Receita> findByUuidAndSafra_Talhao_Propriedade_Usuario_Id(String uuid, Long usuarioId);

    @Query("select x from Receita x where x.safra.talhao.propriedade.usuario.id = :usuarioId and x.excluidoEm is null")
    List<Receita> findBySafra_Talhao_Propriedade_Usuario_Id(@Param("usuarioId") Long usuarioId);

    @Query("select x from Receita x where x.safra.talhao.propriedade.usuario.id = :usuarioId and x.atualizadoEm > :desde and x.excluidoEm is null")
    List<Receita> findBySafra_Talhao_Propriedade_Usuario_IdAndAtualizadoEmAfter(@Param("usuarioId") Long usuarioId, @Param("desde") Instant desde);
}
