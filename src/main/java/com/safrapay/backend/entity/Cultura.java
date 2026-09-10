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

/**
 * Cultura eh entidade propria, irma de Talhao (nao filha) - ambas dependem
 * direto de Propriedade. Decisao de 12/08/2026 (ver Notion, pagina Detalhes).
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "cultura")
public class Cultura extends BaseSyncEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "propriedade_id", nullable = false)
    private Propriedade propriedade;

    @Column(nullable = false)
    private String nome;
}
