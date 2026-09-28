package com.safrapay.backend.safra.dto;

import com.safrapay.backend.entity.Safra;
import com.safrapay.backend.util.SyncDateUtil;

import java.time.format.DateTimeFormatter;

public class SafraSyncResponse {

    private String uuid;
    private String talhaoUuid;
    private String variedade;
    private String anoSafra;
    private String dataInicio;
    private String dataFim;
    private String atualizadoEm;

    public static SafraSyncResponse de(Safra s) {
        SafraSyncResponse r = new SafraSyncResponse();
        r.uuid = s.getUuid();
        r.talhaoUuid = s.getTalhao().getUuid();
        r.variedade = s.getVariedade();
        r.anoSafra = s.getAnoSafra();
        r.dataInicio = s.getDataInicio() == null ? null : s.getDataInicio().format(DateTimeFormatter.ISO_LOCAL_DATE);
        r.dataFim = s.getDataFim() == null ? null : s.getDataFim().format(DateTimeFormatter.ISO_LOCAL_DATE);
        r.atualizadoEm = SyncDateUtil.formatar(s.getAtualizadoEm());
        return r;
    }

    public String getUuid() { return uuid; }
    public String getTalhaoUuid() { return talhaoUuid; }
    public String getVariedade() { return variedade; }
    public String getAnoSafra() { return anoSafra; }
    public String getDataInicio() { return dataInicio; }
    public String getDataFim() { return dataFim; }
    public String getAtualizadoEm() { return atualizadoEm; }
}
