package com.safrapay.backend.cultura.dto;

import com.safrapay.backend.entity.Cultura;

public class CulturaResponse {

    private Long id;
    private String uuid;
    private Long propriedadeId;
    private String nome;

    public static CulturaResponse de(Cultura c) {
        CulturaResponse r = new CulturaResponse();
        r.id = c.getId();
        r.uuid = c.getUuid();
        r.propriedadeId = c.getPropriedade().getId();
        r.nome = c.getNome();
        return r;
    }

    public Long getId() { return id; }
    public String getUuid() { return uuid; }
    public Long getPropriedadeId() { return propriedadeId; }
    public String getNome() { return nome; }
}
