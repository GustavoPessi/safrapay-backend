package com.safrapay.backend.despesa.dto;

import com.safrapay.backend.entity.Despesa;

import java.math.BigDecimal;
import java.time.LocalDate;

public class DespesaResponse {

    private Long id;
    private String uuid;
    private Long safraId;
    private Long categoriaId;
    private Long maquinaId;
    private String descricao;
    private BigDecimal valor;
    private LocalDate dataRegistro;

    public static DespesaResponse de(Despesa d) {
        DespesaResponse r = new DespesaResponse();
        r.id = d.getId();
        r.uuid = d.getUuid();
        r.safraId = d.getSafra().getId();
        r.categoriaId = d.getCategoria().getId();
        r.maquinaId = d.getMaquina() != null ? d.getMaquina().getId() : null;
        r.descricao = d.getDescricao();
        r.valor = d.getValor();
        r.dataRegistro = d.getDataRegistro();
        return r;
    }

    public Long getId() { return id; }
    public String getUuid() { return uuid; }
    public Long getSafraId() { return safraId; }
    public Long getCategoriaId() { return categoriaId; }
    public Long getMaquinaId() { return maquinaId; }
    public String getDescricao() { return descricao; }
    public BigDecimal getValor() { return valor; }
    public LocalDate getDataRegistro() { return dataRegistro; }
}
