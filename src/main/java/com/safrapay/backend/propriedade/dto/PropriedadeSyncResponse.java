package com.safrapay.backend.propriedade.dto;

import com.safrapay.backend.entity.Propriedade;
import com.safrapay.backend.util.SyncDateUtil;

import java.math.BigDecimal;

/** Item devolvido pelo push e pelo pull de sincronizacao, com o estado final no servidor. */
public class PropriedadeSyncResponse {

    private String uuid;
    private String nome;
    private String localidade;
    private Double areaTotal;
    private String tipoSolo;
    private String matricula;
    private String cib;
    private BigDecimal valor;
    private String observacoes;
    private String atualizadoEm;

    public static PropriedadeSyncResponse de(Propriedade p) {
        PropriedadeSyncResponse r = new PropriedadeSyncResponse();
        r.uuid = p.getUuid();
        r.nome = p.getNome();
        r.localidade = p.getLocalidade();
        r.areaTotal = p.getAreaTotal();
        r.tipoSolo = p.getTipoSolo();
        r.matricula = p.getMatricula();
        r.cib = p.getCib();
        r.valor = p.getValor();
        r.observacoes = p.getObservacoes();
        r.atualizadoEm = SyncDateUtil.formatar(p.getAtualizadoEm());
        return r;
    }

    public String getUuid() { return uuid; }
    public String getNome() { return nome; }
    public String getLocalidade() { return localidade; }
    public Double getAreaTotal() { return areaTotal; }
    public String getTipoSolo() { return tipoSolo; }
    public String getMatricula() { return matricula; }
    public String getCib() { return cib; }
    public BigDecimal getValor() { return valor; }
    public String getObservacoes() { return observacoes; }
    public String getAtualizadoEm() { return atualizadoEm; }
}
