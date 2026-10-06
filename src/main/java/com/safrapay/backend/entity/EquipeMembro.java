package com.safrapay.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Convite e participação de uma pessoa na equipe de um dono (V5, 06/10/2026).
 * Tabela só do backend (não sincroniza com o Room).
 */
@Entity
@Table(name = "equipe_membro")
public class EquipeMembro {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, updatable = false, length = 36)
    private String uuid;

    @ManyToOne(optional = false)
    @JoinColumn(name = "dono_usuario_id", nullable = false)
    private Usuario dono;

    /** null enquanto o convidado ainda não tem conta. */
    @ManyToOne
    @JoinColumn(name = "membro_usuario_id")
    private Usuario membro;

    @Column(nullable = false)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PapelEquipe papel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusMembro status = StatusMembro.PENDENTE;

    @Column(name = "criado_em", nullable = false)
    private Instant criadoEm;

    @Column(name = "atualizado_em", nullable = false)
    private Instant atualizadoEm;

    @Column(name = "respondido_em")
    private Instant respondidoEm;

    @PrePersist
    protected void aoPersistir() {
        if (uuid == null) uuid = UUID.randomUUID().toString();
        Instant agora = Instant.now();
        criadoEm = agora;
        atualizadoEm = agora;
    }

    @PreUpdate
    protected void aoAtualizar() {
        atualizadoEm = Instant.now();
    }

    public Long getId() { return id; }
    public String getUuid() { return uuid; }
    public void setUuid(String uuid) { this.uuid = uuid; }
    public Usuario getDono() { return dono; }
    public void setDono(Usuario dono) { this.dono = dono; }
    public Usuario getMembro() { return membro; }
    public void setMembro(Usuario membro) { this.membro = membro; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public PapelEquipe getPapel() { return papel; }
    public void setPapel(PapelEquipe papel) { this.papel = papel; }
    public StatusMembro getStatus() { return status; }
    public void setStatus(StatusMembro status) { this.status = status; }
    public Instant getCriadoEm() { return criadoEm; }
    public Instant getAtualizadoEm() { return atualizadoEm; }
    public Instant getRespondidoEm() { return respondidoEm; }
    public void setRespondidoEm(Instant respondidoEm) { this.respondidoEm = respondidoEm; }
}
