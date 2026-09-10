package com.safrapay.backend.safra.dto;

import com.safrapay.backend.entity.Safra;

import java.time.LocalDate;

public class SafraResponse {

    private Long id;
    private String uuid;
    private Long talhaoId;
    private Long culturaId;
    private String anoSafra;
    private LocalDate dataInicio;
    private LocalDate dataFim;

    public static SafraResponse de(Safra s) {
        SafraResponse r = new SafraResponse();
        r.id = s.getId();
        r.uuid = s.getUuid();
        r.talhaoId = s.getTalhao().getId();
        r.culturaId = s.getCultura().getId();
        r.anoSafra = s.getAnoSafra();
        r.dataInicio = s.getDataInicio();
        r.dataFim = s.getDataFim();
        return r;
    }

    public Long getId() { return id; }
    public String getUuid() { return uuid; }
    public Long getTalhaoId() { return talhaoId; }
    public Long getCulturaId() { return culturaId; }
    public String getAnoSafra() { return anoSafra; }
    public LocalDate getDataInicio() { return dataInicio; }
    public LocalDate getDataFim() { return dataFim; }
}
