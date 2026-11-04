package com.safrapay.backend.admin.dto;

/**
 * Uma conta na lista do admin. Datas em ISO-8601 (UTC).
 *
 * @param papel        DONO (conta própria) ou GERENTE/COLABORADOR (membro aceito de uma equipe)
 * @param donoEmail    e-mail do dono da equipe, quando a pessoa é membro; senão null
 * @param membros      membros aceitos na equipe desta conta (quando ela é dona)
 * @param ultimoAcesso último login ou renovação de token; null se nunca entrou
 */
public record AdminUsuarioResponse(
        long id,
        String nome,
        String email,
        boolean emailVerificado,
        String criadoEm,
        String papel,
        String donoEmail,
        long membros,
        long propriedades,
        long receitas,
        long despesas,
        String ultimoAcesso) {
}
