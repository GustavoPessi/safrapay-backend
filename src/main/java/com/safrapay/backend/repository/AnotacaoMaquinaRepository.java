package com.safrapay.backend.repository;

import com.safrapay.backend.entity.AnotacaoMaquina;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AnotacaoMaquinaRepository extends JpaRepository<AnotacaoMaquina, Long> {

    List<AnotacaoMaquina> findByMaquinaId(Long maquinaId);
}
