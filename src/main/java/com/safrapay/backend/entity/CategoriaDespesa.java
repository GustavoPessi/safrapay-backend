package com.safrapay.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Tabela simples de lookup (as 7 categorias fixas do seed) - sem uuid/sync,
 * igual ao schema local.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "categoria_despesa")
public class CategoriaDespesa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String nome;
}
