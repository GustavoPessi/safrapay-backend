package com.safrapay.backend.repository;

import com.safrapay.backend.entity.Receita;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReceitaRepository extends JpaRepository<Receita, Long> {

    List<Receita> findBySafraId(Long safraId);
}
