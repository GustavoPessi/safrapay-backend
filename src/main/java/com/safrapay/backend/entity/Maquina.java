package com.safrapay.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "maquina")
public class Maquina extends BaseSyncEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(nullable = false)
    private String nome;

    private String marca;

    private String tipo;

    // Modelo (ex.: "MF 4275") - espelha o campo do app. Coluna criada na V3.
    private String modelo;

    private String ano;

    private String identificacao;

    private BigDecimal valor;

    private String observacoes;

    /**
     * Exclusão sincronizada (V6, 08/10/2026): preenchido quando o item foi excluído (no app
     * ou em cascata no servidor). O item some das listas e do pull normal e vira um aviso de
     * exclusão em GET /api/exclusoes/sync, pros outros aparelhos apagarem também.
     */
    @Column(name = "excluido_em")
    private Instant excluidoEm;
}
