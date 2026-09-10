package com.safrapay.backend.auth;

import com.safrapay.backend.auth.dto.AuthResponse;
import com.safrapay.backend.auth.dto.EsqueciSenhaRequest;
import com.safrapay.backend.auth.dto.LoginRequest;
import com.safrapay.backend.auth.dto.MensagemResponse;
import com.safrapay.backend.auth.dto.RedefinirSenhaRequest;
import com.safrapay.backend.auth.dto.RegisterRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
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
    public ResponseEntity<MensagemResponse> registrar(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authService.registrar(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
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
}
