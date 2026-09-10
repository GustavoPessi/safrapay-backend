package com.safrapay.backend.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class RedefinirSenhaRequest {

    @NotBlank(message = "Token eh obrigatorio")
    private String token;

    @NotBlank(message = "Nova senha eh obrigatoria")
    @Size(min = 6, message = "Senha deve ter no minimo 6 caracteres")
    private String novaSenha;

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    public String getNovaSenha() { return novaSenha; }
    public void setNovaSenha(String novaSenha) { this.novaSenha = novaSenha; }
}
