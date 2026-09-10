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

/**
 * Versao base da anotacao, igual ao schema local (texto + data).
 * Os campos de ciclo de vida (prazo/status/responsavel/conclusao) para o
 * modo multi-usuario sao uma fase futura - ver Notion, pagina
 * "Backend & Multi-usuario".
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "anotacao_maquina")
public class AnotacaoMaquina extends BaseSyncEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "maquina_id", nullable = false)
    private Maquina maquina;

    @Column(nullable = false)
    private String texto;

    @Column(nullable = false)
    private LocalDate data;
}
