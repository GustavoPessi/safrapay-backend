package com.safrapay.backend.receita.dto;

import com.safrapay.backend.entity.Receita;
import com.safrapay.backend.util.SyncDateUtil;

import java.time.format.DateTimeFormatter;

public class ReceitaSyncResponse {

    private String uuid;
    private String safraUuid;
    private String descricao;
    private Double valor;
    private String dataRegistro;
    private String atualizadoEm;

    public static ReceitaSyncResponse de(Receita r0) {
        ReceitaSyncResponse r = new ReceitaSyncResponse();
        r.uuid = r0.getUuid();
        r.safraUuid = r0.getSafra().getUuid();
        r.descricao = r0.getDescricao();
        r.valor = r0.getValor() == null ? null : r0.getValor().doubleValue();
        r.dataRegistro = r0.getDataRegistro() == null ? null : r0.getDataRegistro().format(DateTimeFormatter.ISO_LOCAL_DATE);
        r.atualizadoEm = SyncDateUtil.formatar(r0.getAtualizadoEm());
        return r;
    }

    public String getUuid() { return uuid; }
    public String getSafraUuid() { return safraUuid; }
    public String getDescricao() { return descricao; }
    public Double getValor() { return valor; }
    public String getDataRegistro() { return dataRegistro; }
    public String getAtualizadoEm() { return atualizadoEm; }
}
