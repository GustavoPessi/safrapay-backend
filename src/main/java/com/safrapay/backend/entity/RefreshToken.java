package com.safrapay.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * Refresh token do login (V4, 01/10/2026). Guarda só o hash SHA-256 do token;
 * o valor em si só existe no aparelho do produtor. Tabela só do backend (não
 * estende BaseSyncEntity, não sincroniza com o app).
 */
@Entity
@Table(name = "refresh_token")
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Column(name = "data_criacao", nullable = false)
    private Instant dataCriacao;

    @Column(name = "data_expiracao", nullable = false)
    private Instant dataExpiracao;

    /** Quando foi trocado (rotação), encerrado no logout ou na troca de senha. null = ativo. */
    @Column(name = "data_revogacao")
    private Instant dataRevogacao;

    /** Hash do token que substituiu este na rotação; null = revogado por logout/senha. */
    @Column(name = "substituido_por_hash", length = 64)
    private String substituidoPorHash;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }
    public String getTokenHash() { return tokenHash; }
    public void setTokenHash(String tokenHash) { this.tokenHash = tokenHash; }
    public Instant getDataCriacao() { return dataCriacao; }
    public void setDataCriacao(Instant dataCriacao) { this.dataCriacao = dataCriacao; }
    public Instant getDataExpiracao() { return dataExpiracao; }
    public void setDataExpiracao(Instant dataExpiracao) { this.dataExpiracao = dataExpiracao; }
    public Instant getDataRevogacao() { return dataRevogacao; }
    public void setDataRevogacao(Instant dataRevogacao) { this.dataRevogacao = dataRevogacao; }
    public String getSubstituidoPorHash() { return substituidoPorHash; }
    public void setSubstituidoPorHash(String substituidoPorHash) { this.substituidoPorHash = substituidoPorHash; }
}
