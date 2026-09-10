package com.safrapay.backend.metafinanceira.dto;

import com.safrapay.backend.entity.MetaFinanceira;

import java.math.BigDecimal;
import java.time.LocalDate;

public class MetaFinanceiraResponse {

    private Long id;
    private String uuid;
    private Long culturaId;
    private Long safraId;
    private String descricao;
    private BigDecimal valorAlvo;
    private LocalDate prazo;

    public static MetaFinanceiraResponse de(MetaFinanceira m) {
        MetaFinanceiraResponse r = new MetaFinanceiraResponse();
        r.id = m.getId();
        r.uuid = m.getUuid();
        r.culturaId = m.getCultura().getId();
        r.safraId = m.getSafra() != null ? m.getSafra().getId() : null;
        r.descricao = m.getDescricao();
        r.valorAlvo = m.getValorAlvo();
        r.prazo = m.getPrazo();
        return r;
    }

    public Long getId() { return id; }
    public String getUuid() { return uuid; }
    public Long getCulturaId() { return culturaId; }
    public Long getSafraId() { return safraId; }
    public String getDescricao() { return descricao; }
    public BigDecimal getValorAlvo() { return valorAlvo; }
    public LocalDate getPrazo() { return prazo; }
}
