package com.safrapay.backend.repository;

import com.safrapay.backend.entity.MetaFinanceira;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MetaFinanceiraRepository extends JpaRepository<MetaFinanceira, Long> {

    List<MetaFinanceira> findByCulturaId(Long culturaId);
}
