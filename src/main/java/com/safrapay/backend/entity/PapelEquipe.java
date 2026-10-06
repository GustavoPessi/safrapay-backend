package com.safrapay.backend.entity;

/** Papel de um membro na equipe do dono (gravado em equipe_membro.papel). */
public enum PapelEquipe {
    /** Quase igual ao dono: usa o app offline nos dados da fazenda, sem gerenciar a equipe. */
    GERENTE,
    /** Parte enxuta e online do app: máquinas e tarefas. */
    COLABORADOR
}
