package com.safrapay.backend.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;

import java.util.function.Supplier;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final UsuarioAutenticadoService usuarioAutenticadoService;
    private final JwtAuthFilter jwtAuthFilter;

    public SecurityConfig(UsuarioAutenticadoService usuarioAutenticadoService, JwtAuthFilter jwtAuthFilter) {
        this.usuarioAutenticadoService = usuarioAutenticadoService;
        this.jwtAuthFilter = jwtAuthFilter;
    }

    /** Rotas autenticadas liberadas também para o papel COLABORADOR. */
    static final String[] ROTAS_COLABORADOR = {
            "/api/auth/alterar-senha",
            "/api/usuario/**",
            "/api/equipe/minha",
            "/api/equipe/convites/recebidos",
            "/api/equipe/convites/*/aceitar",
            "/api/equipe/convites/*/recusar",
            "/api/equipe/sair",
            "/api/colaborador/**"
    };

    /** Autenticado e não é colaborador. */
    static AuthorizationDecision naoColaborador(Supplier<Authentication> autenticacao, RequestAuthorizationContext contexto) {
        Authentication a = autenticacao.get();
        boolean ok = a != null && a.isAuthenticated() && a.getPrincipal() instanceof UsuarioAutenticado u
                && u.getPapel() != PapelAcesso.COLABORADOR;
        return new AuthorizationDecision(ok);
    }

    /**
     * 403 de papel com o cabeçalho X-SafraPay-Erro: PAPEL, pro app diferenciar de
     * sessão expirada (que também vem 403) e não tentar renovar o token à toa.
     */
    static void acessoNegado(HttpServletRequest request, HttpServletResponse response, AccessDeniedException e)
            throws java.io.IOException {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        boolean colaborador = a != null && a.getPrincipal() instanceof UsuarioAutenticado u
                && u.getPapel() == PapelAcesso.COLABORADOR;
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json;charset=UTF-8");
        if (colaborador) {
            response.setHeader("X-SafraPay-Erro", "PAPEL");
            response.getWriter().write("{\"status\":403,\"erro\":\"PAPEL\",\"mensagem\":\"Seu acesso de colaborador não permite esta ação\"}");
        } else {
            response.getWriter().write("{\"status\":403,\"erro\":\"ACESSO_NEGADO\",\"mensagem\":\"Acesso negado\"}");
        }
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(usuarioAutenticadoService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/api/status",
                                "/actuator/**",
                                "/api/auth/registrar",
                                "/api/auth/login",
                                "/api/auth/refresh",
                                "/api/auth/logout",
                                "/api/auth/confirmar-email",
                                "/api/auth/esqueci-senha",
                                "/api/auth/redefinir-senha",
                                "/redefinir-senha.html",
                                "/confirmar-email.html",
                                "/error"
                        ).permitAll()
                        // /api/auth/alterar-senha fica de fora da lista acima de proposito:
                        // exige usuario autenticado (JWT), pois so faz sentido pra quem ja esta logado.
                        // Multiusuario (06/10/2026): o COLABORADOR so acessa estas rotas; o resto
                        // (dados da fazenda, sync, chat de IA) e so de DONO e GERENTE.
                        .requestMatchers(ROTAS_COLABORADOR).authenticated()
                        .anyRequest().access(SecurityConfig::naoColaborador)
                )
                .exceptionHandling(e -> e.accessDeniedHandler(SecurityConfig::acessoNegado))
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
