package com.safrapay.backend.talhao.dto;

/**
 * Item enviado pelo app no push de sincronizacao (POST /api/talhoes/sync).
 * Carrega "propriedadeUuid" (nao um id local do celular nem do servidor) pra
 * resolver o talhao-pai sem depender de ids que nunca coincidem entre os dois lados.
 */
public class TalhaoSyncRequest {

    private String uuid;
    private String propriedadeUuid;
    private String nome;
    private Double areaHa;
    private String cultura;
    private Double latitude;
    private Double longitude;
    private String atualizadoEm;

    public String getUuid() { return uuid; }
    public void setUuid(String uuid) { this.uuid = uuid; }
    public String getPropriedadeUuid() { return propriedadeUuid; }
    public void setPropriedadeUuid(String propriedadeUuid) { this.propriedadeUuid = propriedadeUuid; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public Double getAreaHa() { return areaHa; }
    public void setAreaHa(Double areaHa) { this.areaHa = areaHa; }
    public String getCultura() { return cultura; }
    public void setCultura(String cultura) { this.cultura = cultura; }
    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }
    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }
    public String getAtualizadoEm() { return atualizadoEm; }
    public void setAtualizadoEm(String atualizadoEm) { this.atualizadoEm = atualizadoEm; }
}
