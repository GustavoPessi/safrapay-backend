package com.safrapay.backend.receita.dto;

import com.safrapay.backend.entity.Receita;

import java.math.BigDecimal;
import java.time.LocalDate;

public class ReceitaResponse {

    private Long id;
    private String uuid;
    private Long safraId;
    private String descricao;
    private BigDecimal valor;
    private LocalDate dataRegistro;

    public static ReceitaResponse de(Receita r0) {
        ReceitaResponse r = new ReceitaResponse();
        r.id = r0.getId();
        r.uuid = r0.getUuid();
        r.safraId = r0.getSafra().getId();
        r.descricao = r0.getDescricao();
        r.valor = r0.getValor();
        r.dataRegistro = r0.getDataRegistro();
        return r;
    }

    public Long getId() { return id; }
    public String getUuid() { return uuid; }
    public Long getSafraId() { return safraId; }
    public String getDescricao() { return descricao; }
    public BigDecimal getValor() { return valor; }
    public LocalDate getDataRegistro() { return dataRegistro; }
}
