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
 * Anotacao de maquina, igual ao schema local do app: texto + data opcional
 * (no app funciona como data da revisao/prazo) + concluida.
 * "data" opcional e "concluida" entraram na V3 (Fase 4 do sync, 28/09/2026).
 * Os campos de ciclo de vida do modo multi-usuario (prazo/status/responsavel/
 * concluido_por) sao uma fase futura - ver Notion, pagina
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

    private LocalDate data;

    @Column(nullable = false)
    private boolean concluida = false;
}
