package com.safrapay.backend.repository;

import com.safrapay.backend.entity.Maquina;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MaquinaRepository extends JpaRepository<Maquina, Long> {

    List<Maquina> findByUsuarioId(Long usuarioId);
}
