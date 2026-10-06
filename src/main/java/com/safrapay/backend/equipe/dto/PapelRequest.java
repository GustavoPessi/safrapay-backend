package com.safrapay.backend.equipe.dto;

/** PATCH /api/equipe/membros/{uuid} (só o dono). */
public class PapelRequest {

    /** GERENTE ou COLABORADOR. */
    private String papel;

    public String getPapel() { return papel; }
    public void setPapel(String papel) { this.papel = papel; }
}
