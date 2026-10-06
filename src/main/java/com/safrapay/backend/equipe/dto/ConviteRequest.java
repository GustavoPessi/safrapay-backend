package com.safrapay.backend.equipe.dto;

/** POST /api/equipe/convites (só o dono). */
public class ConviteRequest {

    private String email;
    /** GERENTE ou COLABORADOR. */
    private String papel;

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPapel() { return papel; }
    public void setPapel(String papel) { this.papel = papel; }
}
