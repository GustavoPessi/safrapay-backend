package com.safrapay.backend.repository;

import com.safrapay.backend.entity.Maquina;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MaquinaRepository extends JpaRepository<Maquina, Long> {

    List<Maquina> findByUsuarioId(Long usuarioId);

    // Usado pela sincronizacao de Despesa (maquina opcional, viaja como uuid).
    Optional<Maquina> findByUuidAndUsuarioId(String uuid, Long usuarioId);
}
