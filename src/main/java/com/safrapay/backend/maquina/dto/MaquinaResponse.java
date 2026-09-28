package com.safrapay.backend.maquina.dto;

import com.safrapay.backend.entity.Maquina;

import java.math.BigDecimal;

public class MaquinaResponse {

    private Long id;
    private String uuid;
    private String nome;
    private String marca;
    private String tipo;
    private String modelo;
    private String ano;
    private String identificacao;
    private BigDecimal valor;
    private String observacoes;

    public static MaquinaResponse de(Maquina m) {
        MaquinaResponse r = new MaquinaResponse();
        r.id = m.getId();
        r.uuid = m.getUuid();
        r.nome = m.getNome();
        r.marca = m.getMarca();
        r.tipo = m.getTipo();
        r.modelo = m.getModelo();
        r.ano = m.getAno();
        r.identificacao = m.getIdentificacao();
        r.valor = m.getValor();
        r.observacoes = m.getObservacoes();
        return r;
    }

    public Long getId() { return id; }
    public String getUuid() { return uuid; }
    public String getNome() { return nome; }
    public String getMarca() { return marca; }
    public String getTipo() { return tipo; }
    public String getModelo() { return modelo; }
    public String getAno() { return ano; }
    public String getIdentificacao() { return identificacao; }
    public BigDecimal getValor() { return valor; }
    public String getObservacoes() { return observacoes; }
}
