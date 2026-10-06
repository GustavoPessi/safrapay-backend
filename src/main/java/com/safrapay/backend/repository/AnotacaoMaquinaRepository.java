package com.safrapay.backend.repository;

import com.safrapay.backend.entity.AnotacaoMaquina;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface AnotacaoMaquinaRepository extends JpaRepository<AnotacaoMaquina, Long> {

    List<AnotacaoMaquina> findByMaquinaId(Long maquinaId);

    // Sincronizacao (Fase 4): navega AnotacaoMaquina -> maquina -> usuario.
    Optional<AnotacaoMaquina> findByUuidAndMaquina_Usuario_Id(String uuid, Long usuarioId);

    List<AnotacaoMaquina> findByMaquina_Usuario_Id(Long usuarioId);

    List<AnotacaoMaquina> findByMaquina_Usuario_IdAndAtualizadoEmAfter(Long usuarioId, Instant desde);
    // Multiusuario (V5): tarefas da fazenda do dono, com a maquina ja carregada.
    @org.springframework.data.jpa.repository.Query("select a from AnotacaoMaquina a join fetch a.maquina m where m.usuario.id = :donoId")
    List<AnotacaoMaquina> tarefasDoDono(@org.springframework.data.repository.query.Param("donoId") Long donoId);
}
