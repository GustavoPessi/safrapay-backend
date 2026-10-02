package com.safrapay.backend.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Filtro que le o header Authorization: Bearer <token> em cada requisicao,
 * valida o JWT e, se valido, autentica o usuario no SecurityContext.
 */
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final List<AntPathRequestMatcher> ROTAS_PUBLICAS = List.of(
            new AntPathRequestMatcher("/api/status"),
            new AntPathRequestMatcher("/actuator/**"),
            new AntPathRequestMatcher("/api/auth/registrar"),
            new AntPathRequestMatcher("/api/auth/login"),
            new AntPathRequestMatcher("/api/auth/refresh"),
            new AntPathRequestMatcher("/api/auth/logout"),
            new AntPathRequestMatcher("/api/auth/confirmar-email"),
            new AntPathRequestMatcher("/api/auth/esqueci-senha"),
            new AntPathRequestMatcher("/api/auth/redefinir-senha"),
            new AntPathRequestMatcher("/redefinir-senha.html"),
            new AntPathRequestMatcher("/confirmar-email.html"),
            new AntPathRequestMatcher("/error")
            // /api/auth/alterar-senha NAO entra aqui de proposito: precisa que
            // este filtro tente ler e validar o JWT normalmente.
    );

    private final JwtService jwtService;
    private final UsuarioAutenticadoService usuarioAutenticadoService;

    public JwtAuthFilter(JwtService jwtService, UsuarioAutenticadoService usuarioAutenticadoService) {
        this.jwtService = jwtService;
        this.usuarioAutenticadoService = usuarioAutenticadoService;
    }

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        return ROTAS_PUBLICAS.stream().anyMatch(matcher -> matcher.matches(request));
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                     @NonNull HttpServletResponse response,
                                     @NonNull FilterChain filterChain) throws ServletException, IOException {

        String cabecalho = request.getHeader("Authorization");

        if (cabecalho == null || !cabecalho.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = cabecalho.substring(7);

        try {
            String email = jwtService.extrairEmail(token);

            if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails usuario = usuarioAutenticadoService.loadUserByUsername(email);

                if (jwtService.tokenValido(token, usuario)) {
                    UsernamePasswordAuthenticationToken autenticacao =
                            new UsernamePasswordAuthenticationToken(usuario, null, usuario.getAuthorities());
                    autenticacao.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(autenticacao);
                }
            }
        } catch (Exception e) {
            // token invalido/expirado - segue sem autenticar, o Security barra depois
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }
}
