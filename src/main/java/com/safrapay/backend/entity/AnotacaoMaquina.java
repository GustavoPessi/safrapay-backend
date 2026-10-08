package com.safrapay.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Anotacao de maquina, igual ao schema local do app: texto + data opcional
 * (no app funciona como data da revisao/prazo) + concluida.
 * "data" opcional e "concluida" entraram na V3 (Fase 4 do sync, 28/09/2026).
 * Multiusuario (V5, 06/10/2026): a anotacao vira tarefa, com prazo,
 * responsavel, quem criou e quem concluiu. "concluida" continua sendo o status
 * (pendente/realizada); "atrasada" e calculada (pendente e prazo < hoje).
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "anotacao_maquina")
public class AnotacaoMaquina extends BaseSyncEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "maquina_id", nullable = false)
    private Maquina maquina;

    @Column(nullable = false)
    private String texto;

    private LocalDate data;

    @Column(nullable = false)
    private boolean concluida = false;

    /** Prazo da tarefa (opcional). */
    private LocalDate prazo;

    /** Quem deve fazer (dono ou membro da equipe); null = sem responsável. */
    @ManyToOne
    @JoinColumn(name = "responsavel_usuario_id")
    private Usuario responsavel;

    @Column(name = "concluido_em")
    private Instant concluidoEm;

    @ManyToOne
    @JoinColumn(name = "concluido_por_usuario_id")
    private Usuario concluidoPor;

    @ManyToOne
    @JoinColumn(name = "criado_por_usuario_id")
    private Usuario criadoPor;

    /** Pendente com prazo vencido (não é guardado no banco). */
    public boolean isAtrasada(LocalDate hoje) {
        return !concluida && prazo != null && prazo.isBefore(hoje);
    }

    /**
     * Marca/desmarca como realizada registrando quem e quando. Só mexe nos
     * campos de conclusão quando o status muda de fato.
     */
    public void definirConcluida(boolean nova, Usuario quem) {
        if (nova && !concluida) {
            concluidoEm = Instant.now();
            concluidoPor = quem;
        } else if (!nova) {
            concluidoEm = null;
            concluidoPor = null;
        }
        concluida = nova;
    }

    /**
     * Exclusão sincronizada (V6, 08/10/2026): preenchido quando o item foi excluído (no app
     * ou em cascata no servidor). O item some das listas e do pull normal e vira um aviso de
     * exclusão em GET /api/exclusoes/sync, pros outros aparelhos apagarem também.
     */
    @Column(name = "excluido_em")
    private Instant excluidoEm;
}
