package com.safrapay.backend.anotacaomaquina.dto;

/**
 * Item enviado pelo app no push de sincronizacao (POST /api/anotacoes-maquina/sync).
 * "maquinaUuid" identifica a maquina-pai (nunca o id). "data" eh opcional e
 * vem em ISO (yyyy-MM-dd), igual ao Room.
 */
public class AnotacaoMaquinaSyncRequest {

    private String uuid;
    private String maquinaUuid;
    private String texto;
    private String data;
    private Boolean concluida;
    private String atualizadoEm;

    public String getUuid() { return uuid; }
    public void setUuid(String uuid) { this.uuid = uuid; }
    public String getMaquinaUuid() { return maquinaUuid; }
    public void setMaquinaUuid(String maquinaUuid) { this.maquinaUuid = maquinaUuid; }
    public String getTexto() { return texto; }
    public void setTexto(String texto) { this.texto = texto; }
    public String getData() { return data; }
    public void setData(String data) { this.data = data; }
    public Boolean getConcluida() { return concluida; }
    public void setConcluida(Boolean concluida) { this.concluida = concluida; }
    public String getAtualizadoEm() { return atualizadoEm; }
    public void setAtualizadoEm(String atualizadoEm) { this.atualizadoEm = atualizadoEm; }
}
