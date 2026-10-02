package com.safrapay.backend.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class RegistrarRequest {

    @NotBlank(message = "Nome eh obrigatorio")
    private String nome;

    @NotBlank(message = "Email eh obrigatorio")
    @Email(message = "Email invalido")
    private String email;

    @NotBlank(message = "Senha eh obrigatoria")
    @Size(min = 6, message = "Senha deve ter no minimo 6 caracteres")
    private String senha;

    private String cpf;

    private String telefone;

    /** Formato ISO (yyyy-MM-dd), opcional. */
    private String nascimento;

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getSenha() {
        return senha;
    }

    public void setSenha(String senha) {
        this.senha = senha;
    }

    public String getCpf() {
        return cpf;
    }

    public void setCpf(String cpf) {
        this.cpf = cpf;
    }

    public String getTelefone() {
        return telefone;
    }

    public void setTelefone(String telefone) {
        this.telefone = telefone;
    }

    public String getNascimento() {
        return nascimento;
    }

    public void setNascimento(String nascimento) {
        this.nascimento = nascimento;
    }
}
