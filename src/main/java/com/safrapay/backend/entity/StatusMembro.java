package com.safrapay.backend.entity;

/** Situação do convite/participação em equipe_membro.status. */
public enum StatusMembro {
    PENDENTE,
    ACEITO,
    RECUSADO,
    /** Convite pendente cancelado pelo dono. */
    CANCELADO,
    /** Membro aceito removido pelo dono. */
    REMOVIDO,
    /** Membro que saiu da equipe por conta própria. */
    SAIU
}
