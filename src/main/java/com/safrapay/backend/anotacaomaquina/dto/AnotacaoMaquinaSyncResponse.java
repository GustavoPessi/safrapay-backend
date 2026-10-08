package com.safrapay.backend.anotacaomaquina.dto;

import com.safrapay.backend.entity.AnotacaoMaquina;
import com.safrapay.backend.util.DataSyncUtil;

import java.time.format.DateTimeFormatter;

public class AnotacaoMaquinaSyncResponse {

    private String uuid;
    private String maquinaUuid;
    private String texto;
    private String data;
    private Boolean concluida;
    private String atualizadoEm;
    // Multiusuario (V5): o app guarda os nomes pra exibir (Room nao tem tabela de usuarios).
    private String prazo;
    private String responsavelUuid;
    private String responsavelNome;
    private String concluidoEm;
    private String concluidoPorNome;
    private String criadoPorNome;
    /** Quando foi criada no servidor (instante UTC ISO, com hora). 08/10/2026. */
    private String criadoEm;

    public static AnotacaoMaquinaSyncResponse de(AnotacaoMaquina a) {
        AnotacaoMaquinaSyncResponse r = new AnotacaoMaquinaSyncResponse();
        r.uuid = a.getUuid();
        r.maquinaUuid = a.getMaquina().getUuid();
        r.texto = a.getTexto();
        r.data = a.getData() == null ? null : a.getData().format(DateTimeFormatter.ISO_LOCAL_DATE);
        r.concluida = a.isConcluida();
        r.atualizadoEm = DataSyncUtil.formatar(a.getAtualizadoEm());
        r.prazo = a.getPrazo() == null ? null : a.getPrazo().format(DateTimeFormatter.ISO_LOCAL_DATE);
        r.responsavelUuid = a.getResponsavel() == null ? null : a.getResponsavel().getUuid();
        r.responsavelNome = a.getResponsavel() == null ? null : a.getResponsavel().getNome();
        r.concluidoEm = DataSyncUtil.formatar(a.getConcluidoEm());
        r.concluidoPorNome = a.getConcluidoPor() == null ? null : a.getConcluidoPor().getNome();
        r.criadoPorNome = a.getCriadoPor() == null ? null : a.getCriadoPor().getNome();
        r.criadoEm = DataSyncUtil.formatar(a.getCriadoEm());
        return r;
    }

    public String getUuid() { return uuid; }
    public String getMaquinaUuid() { return maquinaUuid; }
    public String getTexto() { return texto; }
    public String getData() { return data; }
    public Boolean getConcluida() { return concluida; }
    public String getAtualizadoEm() { return atualizadoEm; }
    public String getPrazo() { return prazo; }
    public String getResponsavelUuid() { return responsavelUuid; }
    public String getResponsavelNome() { return responsavelNome; }
    public String getConcluidoEm() { return concluidoEm; }
    public String getConcluidoPorNome() { return concluidoPorNome; }
    public String getCriadoPorNome() { return criadoPorNome; }
    public String getCriadoEm() { return criadoEm; }
}
