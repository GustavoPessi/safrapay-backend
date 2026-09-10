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

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "receita")
public class Receita extends BaseSyncEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "safra_id", nullable = false)
    private Safra safra;

    @Column(nullable = false)
    private String descricao;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal valor;

    @Column(name = "data_registro", nullable = false)
    private LocalDate dataRegistro;
}
