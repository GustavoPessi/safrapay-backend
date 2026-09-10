package com.safrapay.backend.anotacaomaquina.dto;

import com.safrapay.backend.entity.AnotacaoMaquina;

import java.time.LocalDate;

public class AnotacaoMaquinaResponse {

    private Long id;
    private String uuid;
    private Long maquinaId;
    private String texto;
    private LocalDate data;

    public static AnotacaoMaquinaResponse de(AnotacaoMaquina a) {
        AnotacaoMaquinaResponse r = new AnotacaoMaquinaResponse();
        r.id = a.getId();
        r.uuid = a.getUuid();
        r.maquinaId = a.getMaquina().getId();
        r.texto = a.getTexto();
        r.data = a.getData();
        return r;
    }

    public Long getId() { return id; }
    public String getUuid() { return uuid; }
    public Long getMaquinaId() { return maquinaId; }
    public String getTexto() { return texto; }
    public LocalDate getData() { return data; }
}
