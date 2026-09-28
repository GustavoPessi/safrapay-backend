package com.safrapay.backend.repository;

import com.safrapay.backend.entity.Receita;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface ReceitaRepository extends JpaRepository<Receita, Long> {

    List<Receita> findBySafraId(Long safraId);

    // Sincronizacao: navega Receita -> safra -> talhao -> propriedade -> usuario
    // (mesmo padrao de SafraRepository/TalhaoRepository).
    Optional<Receita> findByUuidAndSafra_Talhao_Propriedade_Usuario_Id(String uuid, Long usuarioId);

    List<Receita> findBySafra_Talhao_Propriedade_Usuario_Id(Long usuarioId);

    List<Receita> findBySafra_Talhao_Propriedade_Usuario_IdAndAtualizadoEmAfter(Long usuarioId, Instant desde);
}
