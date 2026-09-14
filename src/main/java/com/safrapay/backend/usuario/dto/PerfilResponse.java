package com.safrapay.backend.usuario.dto;

import com.safrapay.backend.entity.Usuario;

/**
 * Espelha os dados de perfil do usuario logado. Nao inclui o e-mail como
 * campo editavel em nenhum outro DTO desta feature de proposito: mudar o
 * e-mail de uma conta ja verificada exigiria um fluxo de reconfirmacao que
 * ainda nao existe.
 */
public class PerfilResponse {

    private String nome;
    private String email;
    private String cpf;
    private String telefone;
    /** Formato ISO (yyyy-MM-dd), ou null se nao preenchida. */
    private String nascimento;
    private boolean emailVerificado;

    public static PerfilResponse de(Usuario u) {
        PerfilResponse r = new PerfilResponse();
        r.nome = u.getNome();
        r.email = u.getEmail();
        r.cpf = u.getCpf();
        r.telefone = u.getTelefone();
        r.nascimento = u.getNascimento() == null ? null : u.getNascimento().toString();
        r.emailVerificado = u.isEmailVerificado();
        return r;
    }

    public String getNome() { return nome; }
    public String getEmail() { return email; }
    public String getCpf() { return cpf; }
    public String getTelefone() { return telefone; }
    public String getNascimento() { return nascimento; }
    public boolean isEmailVerificado() { return emailVerificado; }
}
