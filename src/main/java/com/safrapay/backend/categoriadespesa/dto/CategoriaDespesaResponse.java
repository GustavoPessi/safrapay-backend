package com.safrapay.backend.categoriadespesa.dto;

import com.safrapay.backend.entity.CategoriaDespesa;

public class CategoriaDespesaResponse {

    private Long id;
    private String nome;

    public static CategoriaDespesaResponse de(CategoriaDespesa c) {
        CategoriaDespesaResponse r = new CategoriaDespesaResponse();
        r.id = c.getId();
        r.nome = c.getNome();
        return r;
    }

    public Long getId() { return id; }
    public String getNome() { return nome; }
}
