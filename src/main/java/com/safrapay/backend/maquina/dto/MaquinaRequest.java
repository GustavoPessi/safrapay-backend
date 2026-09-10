package com.safrapay.backend.maquina.dto;

import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;

public class MaquinaRequest {

    @NotBlank(message = "Nome eh obrigatorio")
    private String nome;

    private String marca;
    private String tipo;
    private String ano;
    private String identificacao;
    private BigDecimal valor;
    private String observacoes;

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getMarca() { return marca; }
    public void setMarca(String marca) { this.marca = marca; }
    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }
    public String getAno() { return ano; }
    public void setAno(String ano) { this.ano = ano; }
    public String getIdentificacao() { return identificacao; }
    public void setIdentificacao(String identificacao) { this.identificacao = identificacao; }
    public BigDecimal getValor() { return valor; }
    public void setValor(BigDecimal valor) { this.valor = valor; }
    public String getObservacoes() { return observacoes; }
    public void setObservacoes(String observacoes) { this.observacoes = observacoes; }
}
