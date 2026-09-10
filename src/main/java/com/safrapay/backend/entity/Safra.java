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

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "safra")
public class Safra extends BaseSyncEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "talhao_id", nullable = false)
    private Talhao talhao;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cultura_id", nullable = false)
    private Cultura cultura;

    @Column(name = "ano_safra", nullable = false)
    private String anoSafra;

    @Column(name = "data_inicio")
    private LocalDate dataInicio;

    @Column(name = "data_fim")
    private LocalDate dataFim;
}
