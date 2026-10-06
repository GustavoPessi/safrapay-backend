package com.safrapay.backend.security;

import com.safrapay.backend.entity.Usuario;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Atalhos para quem está autenticado na requisição atual (a partir do JWT).
 *
 * Multiusuário (06/10/2026):
 * - {@link #usuarioLogado()}: a pessoa logada — perfil, senha, autoria
 *   ("concluído por", "criado por") e limite diário da IA.
 * - {@link #contaDados()}: de quem são os dados — a própria pessoa (DONO) ou o
 *   dono da equipe em que ela é GERENTE/COLABORADOR. Os services de dados
 *   (propriedades, financeiro, máquinas, sync...) filtram e gravam por aqui.
 */
public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static Usuario usuarioLogado() {
        return autenticado().getUsuario();
    }

    public static Usuario contaDados() {
        return autenticado().getDono();
    }

    public static PapelAcesso papel() {
        return autenticado().getPapel();
    }

    private static UsuarioAutenticado autenticado() {
        return (UsuarioAutenticado) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }
}
