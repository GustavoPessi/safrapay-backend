package com.safrapay.backend.auth;

import com.safrapay.backend.auth.dto.AlterarSenhaRequest;
import com.safrapay.backend.auth.dto.AuthResponse;
import com.safrapay.backend.auth.dto.EsqueciSenhaRequest;
import com.safrapay.backend.auth.dto.LoginRequest;
import com.safrapay.backend.auth.dto.MensagemResponse;
import com.safrapay.backend.auth.dto.RedefinirSenhaRequest;
import com.safrapay.backend.auth.dto.RefreshRequest;
import com.safrapay.backend.auth.dto.RegistrarRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/registrar")
    public ResponseEntity<MensagemResponse> registrar(@Valid @RequestBody RegistrarRequest request) {
        return ResponseEntity.ok(authService.registrar(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    /**
     * Troca o refresh token (recebido no login) por um token de acesso novo e um
     * refresh token novo. Publica: e chamada justamente quando o token de acesso
     * venceu. 401 = refresh token invalido/vencido/revogado -> pedir login.
     */
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> renovar(@Valid @RequestBody RefreshRequest request) {
        return ResponseEntity.ok(authService.renovar(request));
    }

    /** Encerra a sessao deste aparelho (botao "Sair" do app). Sempre 200. */
    @PostMapping("/logout")
    public ResponseEntity<MensagemResponse> logout(@Valid @RequestBody RefreshRequest request) {
        return ResponseEntity.ok(authService.logout(request));
    }

    @GetMapping("/confirmar-email")
    public ResponseEntity<MensagemResponse> confirmarEmail(@RequestParam String token) {
        return ResponseEntity.ok(authService.confirmarEmail(token));
    }

    @PostMapping("/esqueci-senha")
    public ResponseEntity<MensagemResponse> esqueciSenha(@Valid @RequestBody EsqueciSenhaRequest request) {
        return ResponseEntity.ok(authService.esqueciSenha(request));
    }

    @PostMapping("/redefinir-senha")
    public ResponseEntity<MensagemResponse> redefinirSenha(@Valid @RequestBody RedefinirSenhaRequest request) {
        return ResponseEntity.ok(authService.redefinirSenha(request));
    }

    // Rota autenticada (exige JWT) - usada pela tela "Alterar senha" em
    // Configuracoes, quando o usuario ja esta logado e sabe a senha atual.
    @PutMapping("/alterar-senha")
    public ResponseEntity<MensagemResponse> alterarSenha(@Valid @RequestBody AlterarSenhaRequest request) {
        return ResponseEntity.ok(authService.alterarSenha(request));
    }
}
