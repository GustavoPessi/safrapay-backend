package com.safrapay.backend.security;

/**
 * Papel de quem está logado (06/10/2026, multiusuário).
 * DONO = não é membro de equipe nenhuma (trabalha nos próprios dados).
 */
public enum PapelAcesso {
    DONO,
    GERENTE,
    COLABORADOR
}
