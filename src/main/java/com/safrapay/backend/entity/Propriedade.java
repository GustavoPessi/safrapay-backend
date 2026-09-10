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

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "propriedade")
public class Propriedade extends BaseSyncEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(nullable = false)
    private String nome;

    private String localidade;

    @Column(name = "area_total")
    private Double areaTotal;

    @Column(name = "tipo_solo")
    private String tipoSolo;

    private String matricula;

    private String cib;

    private BigDecimal valor;

    private String observacoes;
}
