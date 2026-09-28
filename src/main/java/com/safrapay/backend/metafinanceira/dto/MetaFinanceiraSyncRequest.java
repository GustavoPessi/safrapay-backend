package com.safrapay.backend.metafinanceira.dto;

/**
 * Item enviado pelo app no push de sincronizacao (POST /api/metas-financeiras/sync).
 * "safraUuid" resolve a safra-pai. prazo viaja em ISO (yyyy-MM-dd), opcional -
 * o app ja guarda nesse formato. valorAlvo viaja como Double.
 */
public class MetaFinanceiraSyncRequest {

    private String uuid;
    private String safraUuid;
    private String descricao;
    private Double valorAlvo;
    private String prazo;
    private String atualizadoEm;

    public String getUuid() { return uuid; }
    public void setUuid(String uuid) { this.uuid = uuid; }
    public String getSafraUuid() { return safraUuid; }
    public void setSafraUuid(String safraUuid) { this.safraUuid = safraUuid; }
    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }
    public Double getValorAlvo() { return valorAlvo; }
    public void setValorAlvo(Double valorAlvo) { this.valorAlvo = valorAlvo; }
    public String getPrazo() { return prazo; }
    public void setPrazo(String prazo) { this.prazo = prazo; }
    public String getAtualizadoEm() { return atualizadoEm; }
    public void setAtualizadoEm(String atualizadoEm) { this.atualizadoEm = atualizadoEm; }
}
