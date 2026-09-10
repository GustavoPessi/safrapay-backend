package com.safrapay.backend.repository;

import com.safrapay.backend.entity.Talhao;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TalhaoRepository extends JpaRepository<Talhao, Long> {

    List<Talhao> findByPropriedadeId(Long propriedadeId);
}
