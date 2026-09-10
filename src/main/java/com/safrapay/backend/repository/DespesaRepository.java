package com.safrapay.backend.repository;

import com.safrapay.backend.entity.Despesa;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DespesaRepository extends JpaRepository<Despesa, Long> {

    List<Despesa> findBySafraId(Long safraId);

    List<Despesa> findByMaquinaId(Long maquinaId);
}
