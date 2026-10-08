package com.safrapay.backend.repository;

import com.safrapay.backend.entity.AnotacaoMaquina;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface AnotacaoMaquinaRepository extends JpaRepository<AnotacaoMaquina, Long> {

    @Query("select x from AnotacaoMaquina x where x.maquina.id = :maquinaId and x.excluidoEm is null")
    List<AnotacaoMaquina> findByMaquinaId(@Param("maquinaId") Long maquinaId);

    // Sincronizacao (Fase 4): navega AnotacaoMaquina -> maquina -> usuario.
    Optional<AnotacaoMaquina> findByUuidAndMaquina_Usuario_Id(String uuid, Long usuarioId);

    @Query("select x from AnotacaoMaquina x where x.maquina.usuario.id = :usuarioId and x.excluidoEm is null")
    List<AnotacaoMaquina> findByMaquina_Usuario_Id(@Param("usuarioId") Long usuarioId);

    @Query("select x from AnotacaoMaquina x where x.maquina.usuario.id = :usuarioId and x.atualizadoEm > :desde and x.excluidoEm is null")
    List<AnotacaoMaquina> findByMaquina_Usuario_IdAndAtualizadoEmAfter(@Param("usuarioId") Long usuarioId, @Param("desde") Instant desde);
    // Multiusuario (V5): tarefas da fazenda do dono, com a maquina ja carregada.
    @org.springframework.data.jpa.repository.Query("select a from AnotacaoMaquina a join fetch a.maquina m where m.usuario.id = :donoId and a.excluidoEm is null and m.excluidoEm is null")
    List<AnotacaoMaquina> tarefasDoDono(@org.springframework.data.repository.query.Param("donoId") Long donoId);
}
