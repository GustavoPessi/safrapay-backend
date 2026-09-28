package com.safrapay.backend.maquina.dto;

import java.math.BigDecimal;

/**
 * Item enviado pelo app no push de sincronizacao (POST /api/maquinas/sync).
 * Maquina pertence direto ao usuario (nao tem entidade-pai no sync), igual a
 * Propriedade. "atualizadoEm" eh o instante UTC da ultima alteracao local,
 * usado no "last write wins".
 */
public class MaquinaSyncRequest {

    private String uuid;
    private String nome;
    private String marca;
    private String tipo;
    private String modelo;
    private String ano;
    private String identificacao;
    private BigDecimal valor;
    private String observacoes;
    private String atualizadoEm;

    public String getUuid() { return uuid; }
    public void setUuid(String uuid) { this.uuid = uuid; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getMarca() { return marca; }
    public void setMarca(String marca) { this.marca = marca; }
    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }
    public String getModelo() { return modelo; }
    public void setModelo(String modelo) { this.modelo = modelo; }
    public String getAno() { return ano; }
    public void setAno(String ano) { this.ano = ano; }
    public String getIdentificacao() { return identificacao; }
    public void setIdentificacao(String identificacao) { this.identificacao = identificacao; }
    public BigDecimal getValor() { return valor; }
    public void setValor(BigDecimal valor) { this.valor = valor; }
    public String getObservacoes() { return observacoes; }
    public void setObservacoes(String observacoes) { this.observacoes = observacoes; }
    public String getAtualizadoEm() { return atualizadoEm; }
    public void setAtualizadoEm(String atualizadoEm) { this.atualizadoEm = atualizadoEm; }
}
