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

/**
 * Meta financeira vinculada a uma SAFRA (espelha o app: progresso = saldo
 * daquela safra).
 *
 * Mudanca de 28/09/2026 (Fase 3 da sincronizacao): removido o vinculo
 * obrigatorio com Cultura e a safra passou a ser obrigatoria. O app abandonou
 * a entidade Cultura em 14/09/2026 e sempre liga a meta a uma safra - mesmo
 * problema que a entidade Safra tinha (corrigido na Fase 2). A coluna antiga
 * cultura_id foi removida do banco na migration V2 (Flyway).
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "meta_financeira")
public class MetaFinanceira extends BaseSyncEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "safra_id", nullable = false)
    private Safra safra;

    @Column(nullable = false)
    private String descricao;

    @Column(name = "valor_alvo", nullable = false, precision = 14, scale = 2)
    private BigDecimal valorAlvo;

    private LocalDate prazo;
}
