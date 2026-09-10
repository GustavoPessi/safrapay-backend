package com.safrapay.backend.propriedade.dto;

import com.safrapay.backend.entity.Propriedade;

import java.math.BigDecimal;

public class PropriedadeResponse {

    private Long id;
    private String uuid;
    private String nome;
    private String localidade;
    private Double areaTotal;
    private String tipoSolo;
    private String matricula;
    private String cib;
    private BigDecimal valor;
    private String observacoes;

    public static PropriedadeResponse de(Propriedade p) {
        PropriedadeResponse r = new PropriedadeResponse();
        r.id = p.getId();
        r.uuid = p.getUuid();
        r.nome = p.getNome();
        r.localidade = p.getLocalidade();
        r.areaTotal = p.getAreaTotal();
        r.tipoSolo = p.getTipoSolo();
        r.matricula = p.getMatricula();
        r.cib = p.getCib();
        r.valor = p.getValor();
        r.observacoes = p.getObservacoes();
        return r;
    }

    public Long getId() { return id; }
    public String getUuid() { return uuid; }
    public String getNome() { return nome; }
    public String getLocalidade() { return localidade; }
    public Double getAreaTotal() { return areaTotal; }
    public String getTipoSolo() { return tipoSolo; }
    public String getMatricula() { return matricula; }
    public String getCib() { return cib; }
    public BigDecimal getValor() { return valor; }
    public String getObservacoes() { return observacoes; }
}
