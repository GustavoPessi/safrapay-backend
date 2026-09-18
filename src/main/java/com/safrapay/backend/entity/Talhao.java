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

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "talhao")
public class Talhao extends BaseSyncEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "propriedade_id", nullable = false)
    private Propriedade propriedade;

    @Column(nullable = false)
    private String nome;

    @Column(name = "area_ha")
    private Double areaHa;

    // Cultura do talhao (ex.: Soja) - espelha o campo local do app, adicionado
    // para a sincronizacao (14/09/2026); coluna criada automaticamente pelo
    // ddl-auto=update no proximo deploy.
    private String cultura;

    private Double latitude;   // coordenada GPS - opcional (Geolocalizacao Nivel 1, 18/09/2026)
    private Double longitude;  // coordenada GPS - opcional (Geolocalizacao Nivel 1, 18/09/2026)
}
