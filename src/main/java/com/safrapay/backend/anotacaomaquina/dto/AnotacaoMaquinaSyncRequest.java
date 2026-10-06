package com.safrapay.backend.anotacaomaquina.dto;

/**
 * Item enviado pelo app no push de sincronizacao (POST /api/anotacoes-maquina/sync).
 * "maquinaUuid" identifica a maquina-pai (nunca o id). "data" eh opcional e
 * vem em ISO (yyyy-MM-dd), igual ao Room.
 *
 * Multiusuario (V5, 06/10/2026): "prazo" (yyyy-MM-dd) e "responsavelUuid"
 * (uuid do usuario dono ou membro aceito da equipe). Se o campo NAO vier no
 * JSON (app antigo), o valor do servidor e mantido; se vier null, e limpo.
 */
public class AnotacaoMaquinaSyncRequest {

    private String uuid;
    private String maquinaUuid;
    private String texto;
    private String data;
    private Boolean concluida;
    private String atualizadoEm;
    private String prazo;
    private String responsavelUuid;
    // Jackson so chama o setter quando o campo esta no JSON.
    private boolean prazoInformado;
    private boolean responsavelInformado;

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
    public String getPrazo() { return prazo; }
    public void setPrazo(String prazo) { this.prazo = prazo; this.prazoInformado = true; }
    public String getResponsavelUuid() { return responsavelUuid; }
    public void setResponsavelUuid(String responsavelUuid) { this.responsavelUuid = responsavelUuid; this.responsavelInformado = true; }
    public boolean prazoInformado() { return prazoInformado; }
    public boolean responsavelInformado() { return responsavelInformado; }
}
