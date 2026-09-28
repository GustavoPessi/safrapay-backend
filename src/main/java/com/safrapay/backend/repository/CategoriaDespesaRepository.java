package com.safrapay.backend.repository;

import com.safrapay.backend.entity.CategoriaDespesa;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CategoriaDespesaRepository extends JpaRepository<CategoriaDespesa, Long> {

    // Usado na sincronizacao: o app identifica a categoria pelo NOME (unico nos
    // dois lados), porque os ids autoincremento nunca coincidem entre celular e servidor.
    Optional<CategoriaDespesa> findByNome(String nome);
}
