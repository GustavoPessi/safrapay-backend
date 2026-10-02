package com.safrapay.backend.maquina.dto;

import com.safrapay.backend.entity.Maquina;
import com.safrapay.backend.util.DataSyncUtil;

import java.math.BigDecimal;

public class MaquinaSyncResponse {

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

    public static MaquinaSyncResponse de(Maquina m) {
        MaquinaSyncResponse r = new MaquinaSyncResponse();
        r.uuid = m.getUuid();
        r.nome = m.getNome();
        r.marca = m.getMarca();
        r.tipo = m.getTipo();
        r.modelo = m.getModelo();
        r.ano = m.getAno();
        r.identificacao = m.getIdentificacao();
        r.valor = m.getValor();
        r.observacoes = m.getObservacoes();
        r.atualizadoEm = DataSyncUtil.formatar(m.getAtualizadoEm());
        return r;
    }

    public String getUuid() { return uuid; }
    public String getNome() { return nome; }
    public String getMarca() { return marca; }
    public String getTipo() { return tipo; }
    public String getModelo() { return modelo; }
    public String getAno() { return ano; }
    public String getIdentificacao() { return identificacao; }
    public BigDecimal getValor() { return valor; }
    public String getObservacoes() { return observacoes; }
    public String getAtualizadoEm() { return atualizadoEm; }
}
