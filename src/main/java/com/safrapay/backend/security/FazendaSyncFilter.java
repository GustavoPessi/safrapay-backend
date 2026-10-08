package com.safrapay.backend.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.regex.Pattern;

/**
 * Proteção da fazenda na sincronização (08/10/2026).
 *
 * O app manda em toda chamada de sync (GET/POST /api/{entidade}/sync) o cabeçalho
 * {@code X-SafraPay-Fazenda} dizendo de quem são os dados que estão no aparelho:
 * {@code PROPRIA} (a própria conta) ou o uuid do dono da equipe. Se não bater com a situação
 * atual no servidor — ex.: o gerente foi removido da equipe enquanto estava offline e o
 * aparelho ainda tem os dados da fazenda do dono —, a sincronização é recusada com 409 e
 * {@code X-SafraPay-Erro: FAZENDA}, pra nada ir parar na conta errada. O app então confere
 * o papel (GET /api/equipe/minha), troca os dados do aparelho e sincroniza de novo.
 *
 * Sem o cabeçalho (app antigo), passa como antes.
 */
public class FazendaSyncFilter extends OncePerRequestFilter {

    static final String CABECALHO = "X-SafraPay-Fazenda";
    static final String PROPRIA = "PROPRIA";
    private static final Pattern ROTA_SYNC = Pattern.compile("^/api/[^/]+/sync/?$");

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return request.getHeader(CABECALHO) == null || !ROTA_SYNC.matcher(request.getRequestURI()).matches();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        if (a != null && a.getPrincipal() instanceof UsuarioAutenticado u
                && !fazendaEsperada(u).equals(request.getHeader(CABECALHO).trim())) {
            response.setStatus(HttpServletResponse.SC_CONFLICT);
            response.setHeader("X-SafraPay-Erro", "FAZENDA");
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"status\":409,\"erro\":\"FAZENDA\",\"mensagem\":"
                    + "\"Sua participação na equipe mudou. Atualize a equipe antes de sincronizar.\"}");
            return;
        }
        chain.doFilter(request, response);
    }

    /** DONO → os próprios dados; gerente (ou colaborador) → os dados do dono da equipe. */
    static String fazendaEsperada(UsuarioAutenticado u) {
        return u.getPapel() == PapelAcesso.DONO ? PROPRIA : u.getDono().getUuid();
    }
}
