package com.safrapay.backend.anotacaomaquina.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public class AnotacaoMaquinaRequest {

    @NotNull(message = "maquinaId eh obrigatorio")
    private Long maquinaId;

    @NotBlank(message = "Texto eh obrigatorio")
    private String texto;

    @NotNull(message = "Data eh obrigatoria")
    private LocalDate data;

    public Long getMaquinaId() { return maquinaId; }
    public void setMaquinaId(Long maquinaId) { this.maquinaId = maquinaId; }
    public String getTexto() { return texto; }
    public void setTexto(String texto) { this.texto = texto; }
    public LocalDate getData() { return data; }
    public void setData(LocalDate data) { this.data = data; }
}
