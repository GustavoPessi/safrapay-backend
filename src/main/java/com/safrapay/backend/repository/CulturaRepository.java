package com.safrapay.backend.repository;

import com.safrapay.backend.entity.Cultura;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CulturaRepository extends JpaRepository<Cultura, Long> {

    List<Cultura> findByPropriedadeId(Long propriedadeId);
}
