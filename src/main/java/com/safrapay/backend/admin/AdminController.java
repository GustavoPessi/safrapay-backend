package com.safrapay.backend.admin;

import com.safrapay.backend.admin.dto.AdminUsuariosResponse;
import com.safrapay.backend.security.SecurityUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Rotas de administração (04/11/2026). Hoje só a lista de contas cadastradas,
 * pra não precisar entrar na EC2 + psql toda vez. A página /admin.html usa.
 *
 * Quem é admin vem do .env (ADMIN_EMAILS, separados por vírgula). Sem a
 * variável ninguém é admin. Para quem não é admin a rota responde 404, como se
 * não existisse.
 */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService service;
    private final Set<String> emailsAdmin;

    public AdminController(AdminService service, @Value("${app.admin.emails:}") String emailsAdmin) {
        this.service = service;
        this.emailsAdmin = Arrays.stream(emailsAdmin.split(","))
                .map(e -> e.trim().toLowerCase(Locale.ROOT))
                .filter(e -> !e.isEmpty())
                .collect(Collectors.toUnmodifiableSet());
    }

    @GetMapping("/usuarios")
    public AdminUsuariosResponse usuarios() {
        exigirAdmin();
        return service.listarUsuarios();
    }

    private void exigirAdmin() {
        String email = SecurityUtils.usuarioLogado().getEmail();
        if (email == null || !emailsAdmin.contains(email.trim().toLowerCase(Locale.ROOT))) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
    }
}
