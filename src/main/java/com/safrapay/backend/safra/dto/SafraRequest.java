package com.safrapay.backend.safra.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public class SafraRequest {

    @NotNull(message = "talhaoId eh obrigatorio")
    private Long talhaoId;

    @NotNull(message = "culturaId eh obrigatorio")
    private Long culturaId;

    @NotBlank(message = "anoSafra eh obrigatorio")
    private String anoSafra;

    private LocalDate dataInicio;
    private LocalDate dataFim;

    public Long getTalhaoId() { return talhaoId; }
    public void setTalhaoId(Long talhaoId) { this.talhaoId = talhaoId; }
    public Long getCulturaId() { return culturaId; }
    public void setCulturaId(Long culturaId) { this.culturaId = culturaId; }
    public String getAnoSafra() { return anoSafra; }
    public void setAnoSafra(String anoSafra) { this.anoSafra = anoSafra; }
    public LocalDate getDataInicio() { return dataInicio; }
    public void setDataInicio(LocalDate dataInicio) { this.dataInicio = dataInicio; }
    public LocalDate getDataFim() { return dataFim; }
    public void setDataFim(LocalDate dataFim) { this.dataFim = dataFim; }
}
