package com.safrapay.backend.receita.dto;

/**
 * Item enviado pelo app no push de sincronizacao (POST /api/receitas/sync).
 * "safraUuid" resolve a safra-pai (nunca um id local ou do servidor).
 * dataRegistro viaja em ISO (yyyy-MM-dd) - o app ja guarda nesse formato.
 * valor viaja como Double (o app usa double) e vira BigDecimal aqui.
 */
public class ReceitaSyncRequest {

    private String uuid;
    private String safraUuid;
    private String descricao;
    private Double valor;
    private String dataRegistro;
    private String atualizadoEm;

    public String getUuid() { return uuid; }
    public void setUuid(String uuid) { this.uuid = uuid; }
    public String getSafraUuid() { return safraUuid; }
    public void setSafraUuid(String safraUuid) { this.safraUuid = safraUuid; }
    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }
    public Double getValor() { return valor; }
    public void setValor(Double valor) { this.valor = valor; }
    public String getDataRegistro() { return dataRegistro; }
    public void setDataRegistro(String dataRegistro) { this.dataRegistro = dataRegistro; }
    public String getAtualizadoEm() { return atualizadoEm; }
    public void setAtualizadoEm(String atualizadoEm) { this.atualizadoEm = atualizadoEm; }
}
