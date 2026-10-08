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

    private Double latitude;   // coordenada GPS - opcional (Geolocalizacao Nivel 1, 18/09/2026)
    private Double longitude;  // coordenada GPS - opcional (Geolocalizacao Nivel 1, 18/09/2026)

    /**
     * Exclusão sincronizada (V6, 08/10/2026): preenchido quando o item foi excluído (no app
     * ou em cascata no servidor). O item some das listas e do pull normal e vira um aviso de
     * exclusão em GET /api/exclusoes/sync, pros outros aparelhos apagarem também.
     */
    @Column(name = "excluido_em")
    private Instant excluidoEm;
}
