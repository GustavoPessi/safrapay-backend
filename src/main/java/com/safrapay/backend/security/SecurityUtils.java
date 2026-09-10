package com.safrapay.backend.security;

import com.safrapay.backend.entity.Usuario;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Atalho para pegar o Usuario autenticado a partir do JWT da requisicao
 * atual. Usado pelos services de CRUD para garantir que cada usuario so
 * enxergue/edite os proprios dados.
 */
public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static Usuario usuarioLogado() {
        UsuarioDetailsImpl details = (UsuarioDetailsImpl)
                SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return details.getUsuario();
    }
}
