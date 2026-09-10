package com.safrapay.backend.receita.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public class ReceitaRequest {

    @NotNull(message = "safraId eh obrigatorio")
    private Long safraId;

    @NotBlank(message = "Descricao eh obrigatoria")
    private String descricao;

    @NotNull(message = "Valor eh obrigatorio")
    private BigDecimal valor;

    @NotNull(message = "dataRegistro eh obrigatoria")
    private LocalDate dataRegistro;

    public Long getSafraId() { return safraId; }
    public void setSafraId(Long safraId) { this.safraId = safraId; }
    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }
    public BigDecimal getValor() { return valor; }
    public void setValor(BigDecimal valor) { this.valor = valor; }
    public LocalDate getDataRegistro() { return dataRegistro; }
    public void setDataRegistro(LocalDate dataRegistro) { this.dataRegistro = dataRegistro; }
}
