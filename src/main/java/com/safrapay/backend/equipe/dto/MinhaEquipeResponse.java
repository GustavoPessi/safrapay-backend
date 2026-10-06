package com.safrapay.backend.equipe.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * GET /api/equipe/minha — o app chama no login e ao abrir com internet pra
 * saber o papel e de quem é a fazenda.
 */
public class MinhaEquipeResponse {

    /** DONO, GERENTE ou COLABORADOR. */
    private String papel;
    /** Dono da equipe (null quando o papel é DONO). */
    private Pessoa dono;
    /** Dono + membros aceitos — opções de responsável de tarefa. */
    private List<Pessoa> pessoas = new ArrayList<>();
    /** Convites pendentes recebidos por quem está logado. */
    private int convitesPendentes;

    public static class Pessoa {
        private String usuarioUuid;
        private String nome;
        /** DONO, GERENTE ou COLABORADOR. */
        private String papel;

        public Pessoa() {}

        public Pessoa(String usuarioUuid, String nome, String papel) {
            this.usuarioUuid = usuarioUuid;
            this.nome = nome;
            this.papel = papel;
        }

        public String getUsuarioUuid() { return usuarioUuid; }
        public void setUsuarioUuid(String usuarioUuid) { this.usuarioUuid = usuarioUuid; }
        public String getNome() { return nome; }
        public void setNome(String nome) { this.nome = nome; }
        public String getPapel() { return papel; }
        public void setPapel(String papel) { this.papel = papel; }
    }

    public String getPapel() { return papel; }
    public void setPapel(String papel) { this.papel = papel; }
    public Pessoa getDono() { return dono; }
    public void setDono(Pessoa dono) { this.dono = dono; }
    public List<Pessoa> getPessoas() { return pessoas; }
    public void setPessoas(List<Pessoa> pessoas) { this.pessoas = pessoas; }
    public int getConvitesPendentes() { return convitesPendentes; }
    public void setConvitesPendentes(int convitesPendentes) { this.convitesPendentes = convitesPendentes; }
}
