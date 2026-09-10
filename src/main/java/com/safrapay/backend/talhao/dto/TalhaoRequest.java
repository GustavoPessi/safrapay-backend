package com.safrapay.backend.talhao.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class TalhaoRequest {

    @NotNull(message = "propriedadeId eh obrigatorio")
    private Long propriedadeId;

    @NotBlank(message = "Nome eh obrigatorio")
    private String nome;

    private Double areaHa;

    public Long getPropriedadeId() { return propriedadeId; }
    public void setPropriedadeId(Long propriedadeId) { this.propriedadeId = propriedadeId; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public Double getAreaHa() { return areaHa; }
    public void setAreaHa(Double areaHa) { this.areaHa = areaHa; }
}
