package com.safrapay.backend.repository;

import com.safrapay.backend.entity.Safra;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SafraRepository extends JpaRepository<Safra, Long> {

    List<Safra> findByTalhaoId(Long talhaoId);

    List<Safra> findByCulturaId(Long culturaId);
}
