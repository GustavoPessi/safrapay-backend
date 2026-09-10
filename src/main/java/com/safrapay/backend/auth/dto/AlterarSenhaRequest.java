package com.safrapay.backend.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class AlterarSenhaRequest {

    @NotBlank(message = "Senha atual eh obrigatoria")
    private String senhaAtual;

    @NotBlank(message = "Nova senha eh obrigatoria")
    @Size(min = 6, message = "Senha deve ter no minimo 6 caracteres")
    private String novaSenha;

    public String getSenhaAtual() { return senhaAtual; }
    public void setSenhaAtual(String senhaAtual) { this.senhaAtual = senhaAtual; }
    public String getNovaSenha() { return novaSenha; }
    public void setNovaSenha(String novaSenha) { this.novaSenha = novaSenha; }
}
