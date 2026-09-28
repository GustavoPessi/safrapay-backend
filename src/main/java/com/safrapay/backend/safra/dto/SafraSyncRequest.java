package com.safrapay.backend.safra.dto;

/**
 * Item enviado pelo app no push de sincronizacao (POST /api/safras/sync).
 * Carrega "talhaoUuid" (nao um id local do celular nem do servidor) pra
 * resolver a safra-pai sem depender de ids que nunca coincidem entre os dois
 * lados - mesmo padrao do TalhaoSyncRequest com propriedadeUuid.
 *
 * dataInicio/dataFim viajam como String no formato ISO (yyyy-MM-dd, "data
 * pura", sem hora) - o app converte do formato de exibicao (dd/MM/yyyy)
 * antes de mandar (ver DateUtils.brParaIso no app).
 */
public class SafraSyncRequest {

    private String uuid;
    private String talhaoUuid;
    private String variedade;
    private String anoSafra;
    private String dataInicio;
    private String dataFim;
    private String atualizadoEm;

    public String getUuid() { return uuid; }
    public void setUuid(String uuid) { this.uuid = uuid; }
    public String getTalhaoUuid() { return talhaoUuid; }
    public void setTalhaoUuid(String talhaoUuid) { this.talhaoUuid = talhaoUuid; }
    public String getVariedade() { return variedade; }
    public void setVariedade(String variedade) { this.variedade = variedade; }
    public String getAnoSafra() { return anoSafra; }
    public void setAnoSafra(String anoSafra) { this.anoSafra = anoSafra; }
    public String getDataInicio() { return dataInicio; }
    public void setDataInicio(String dataInicio) { this.dataInicio = dataInicio; }
    public String getDataFim() { return dataFim; }
    public void setDataFim(String dataFim) { this.dataFim = dataFim; }
    public String getAtualizadoEm() { return atualizadoEm; }
    public void setAtualizadoEm(String atualizadoEm) { this.atualizadoEm = atualizadoEm; }
}
