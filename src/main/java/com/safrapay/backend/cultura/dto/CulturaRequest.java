package com.safrapay.backend.cultura.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class CulturaRequest {

    @NotNull(message = "propriedadeId eh obrigatorio")
    private Long propriedadeId;

    @NotBlank(message = "Nome eh obrigatorio")
    private String nome;

    public Long getPropriedadeId() { return propriedadeId; }
    public void setPropriedadeId(Long propriedadeId) { this.propriedadeId = propriedadeId; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
}
