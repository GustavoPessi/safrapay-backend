package com.safrapay.backend.usuario;

import com.safrapay.backend.usuario.dto.PerfilRequest;
import com.safrapay.backend.usuario.dto.PerfilResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// Rotas autenticadas (exigem JWT) - nao entram em nenhuma lista de rotas
// publicas, entao ja caem na regra padrao do SecurityConfig
// (.anyRequest().authenticated()), igual as demais rotas de CRUD do app.
@RestController
@RequestMapping("/api/usuario")
public class UsuarioController {

    private final UsuarioService service;

    public UsuarioController(UsuarioService service) {
        this.service = service;
    }

    @GetMapping("/perfil")
    public PerfilResponse buscarPerfil() {
        return service.buscarPerfil();
    }

    @PutMapping("/perfil")
    public PerfilResponse atualizarPerfil(@Valid @RequestBody PerfilRequest request) {
        return service.atualizarPerfil(request);
    }
}
