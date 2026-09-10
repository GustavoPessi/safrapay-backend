package com.safrapay.backend.metafinanceira.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public class MetaFinanceiraRequest {

    @NotNull(message = "culturaId eh obrigatorio")
    private Long culturaId;

    /** Opcional - meta pode ser da cultura toda ou de uma safra especifica. */
    private Long safraId;

    @NotBlank(message = "Descricao eh obrigatoria")
    private String descricao;

    @NotNull(message = "valorAlvo eh obrigatorio")
    private BigDecimal valorAlvo;

    private LocalDate prazo;

    public Long getCulturaId() { return culturaId; }
    public void setCulturaId(Long culturaId) { this.culturaId = culturaId; }
    public Long getSafraId() { return safraId; }
    public void setSafraId(Long safraId) { this.safraId = safraId; }
    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }
    public BigDecimal getValorAlvo() { return valorAlvo; }
    public void setValorAlvo(BigDecimal valorAlvo) { this.valorAlvo = valorAlvo; }
    public LocalDate getPrazo() { return prazo; }
    public void setPrazo(LocalDate prazo) { this.prazo = prazo; }
}
