package com.safrapay.backend.anotacaomaquina.dto;

import com.safrapay.backend.entity.AnotacaoMaquina;
import com.safrapay.backend.util.SyncDateUtil;

import java.time.format.DateTimeFormatter;

public class AnotacaoMaquinaSyncResponse {

    private String uuid;
    private String maquinaUuid;
    private String texto;
    private String data;
    private Boolean concluida;
    private String atualizadoEm;

    public static AnotacaoMaquinaSyncResponse de(AnotacaoMaquina a) {
        AnotacaoMaquinaSyncResponse r = new AnotacaoMaquinaSyncResponse();
        r.uuid = a.getUuid();
        r.maquinaUuid = a.getMaquina().getUuid();
        r.texto = a.getTexto();
        r.data = a.getData() == null ? null : a.getData().format(DateTimeFormatter.ISO_LOCAL_DATE);
        r.concluida = a.isConcluida();
        r.atualizadoEm = SyncDateUtil.formatar(a.getAtualizadoEm());
        return r;
    }

    public String getUuid() { return uuid; }
    public String getMaquinaUuid() { return maquinaUuid; }
    public String getTexto() { return texto; }
    public String getData() { return data; }
    public Boolean getConcluida() { return concluida; }
    public String getAtualizadoEm() { return atualizadoEm; }
}
