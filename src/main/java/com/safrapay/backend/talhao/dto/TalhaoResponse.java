package com.safrapay.backend.talhao.dto;

import com.safrapay.backend.entity.Talhao;

public class TalhaoResponse {

    private Long id;
    private String uuid;
    private Long propriedadeId;
    private String nome;
    private Double areaHa;

    public static TalhaoResponse de(Talhao t) {
        TalhaoResponse r = new TalhaoResponse();
        r.id = t.getId();
        r.uuid = t.getUuid();
        r.propriedadeId = t.getPropriedade().getId();
        r.nome = t.getNome();
        r.areaHa = t.getAreaHa();
        return r;
    }

    public Long getId() { return id; }
    public String getUuid() { return uuid; }
    public Long getPropriedadeId() { return propriedadeId; }
    public String getNome() { return nome; }
    public Double getAreaHa() { return areaHa; }
}
