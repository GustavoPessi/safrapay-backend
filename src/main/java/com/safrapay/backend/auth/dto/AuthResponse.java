package com.safrapay.backend.auth.dto;

public class AuthResponse {

    private String token;
    private String nome;
    private String email;
    private String cpf;
    private String telefone;
    /** Formato ISO (yyyy-MM-dd), ou null se não preenchida. */
    private String nascimento;
    /**
     * Refresh token (01/10/2026): o app guarda e troca em POST /api/auth/refresh
     * quando o token de acesso vencer (401/403). Vale 90 dias e muda a cada troca.
     */
    private String refreshToken;

    public AuthResponse(String token, String nome, String email,
                         String cpf, String telefone, String nascimento) {
        this.token = token;
        this.nome = nome;
        this.email = email;
        this.cpf = cpf;
        this.telefone = telefone;
        this.nascimento = nascimento;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

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

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }
}
