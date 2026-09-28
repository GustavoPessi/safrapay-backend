package com.safrapay.backend.metafinanceira.dto;

import com.safrapay.backend.entity.MetaFinanceira;
import com.safrapay.backend.util.SyncDateUtil;

import java.time.format.DateTimeFormatter;

public class MetaFinanceiraSyncResponse {

    private String uuid;
    private String safraUuid;
    private String descricao;
    private Double valorAlvo;
    private String prazo;
    private String atualizadoEm;

    public static MetaFinanceiraSyncResponse de(MetaFinanceira m) {
        MetaFinanceiraSyncResponse r = new MetaFinanceiraSyncResponse();
        r.uuid = m.getUuid();
        r.safraUuid = m.getSafra().getUuid();
        r.descricao = m.getDescricao();
        r.valorAlvo = m.getValorAlvo() == null ? null : m.getValorAlvo().doubleValue();
        r.prazo = m.getPrazo() == null ? null : m.getPrazo().format(DateTimeFormatter.ISO_LOCAL_DATE);
        r.atualizadoEm = SyncDateUtil.formatar(m.getAtualizadoEm());
        return r;
    }

    public String getUuid() { return uuid; }
    public String getSafraUuid() { return safraUuid; }
    public String getDescricao() { return descricao; }
    public Double getValorAlvo() { return valorAlvo; }
    public String getPrazo() { return prazo; }
    public String getAtualizadoEm() { return atualizadoEm; }
}
