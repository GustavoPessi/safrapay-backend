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

    // Variedade da semente (ex.: "Intacta RR2") - opcional. Espelha o campo
    // local do app. A cultura NAO vive mais aqui: o app abandonou a entidade
    // Cultura em 14/09/2026 (a cultura passou a vir do talhao - ver
    // Talhao.cultura); o vinculo obrigatorio com Cultura foi removido daqui
    // em 28/09/2026 para destravar a Fase 2 do motor de sincronizacao, ja
    // que o app nunca envia um culturaId. A coluna orfa cultura_id foi
    // removida do banco na migration V2 (Flyway).
    private String variedade;

    @Column(name = "ano_safra", nullable = false)
    private String anoSafra;

    @Column(name = "data_inicio")
    private LocalDate dataInicio;

    @Column(name = "data_fim")
    private LocalDate dataFim;
}
