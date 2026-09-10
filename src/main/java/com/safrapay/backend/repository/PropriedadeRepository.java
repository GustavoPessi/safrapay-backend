package com.safrapay.backend.repository;

import com.safrapay.backend.entity.Propriedade;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PropriedadeRepository extends JpaRepository<Propriedade, Long> {

    List<Propriedade> findByUsuarioId(Long usuarioId);
}
