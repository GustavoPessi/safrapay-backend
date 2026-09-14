package com.safrapay.backend.usuario.dto;

import jakarta.validation.constraints.NotBlank;

public class PerfilRequest {

    @NotBlank(message = "Nome eh obrigatorio")
    private String nome;

    private String cpf;

    private String telefone;

    /** Formato ISO (yyyy-MM-dd), ou null. */
    private String nascimento;

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
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
