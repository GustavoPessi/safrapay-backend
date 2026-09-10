package com.safrapay.backend.auth;

import com.safrapay.backend.auth.dto.AuthResponse;
import com.safrapay.backend.auth.dto.LoginRequest;
import com.safrapay.backend.auth.dto.RegisterRequest;
import com.safrapay.backend.entity.Usuario;
import com.safrapay.backend.repository.UsuarioRepository;
import com.safrapay.backend.security.JwtService;
import com.safrapay.backend.security.UsuarioDetailsImpl;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@Service
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public AuthService(UsuarioRepository usuarioRepository,
                        PasswordEncoder passwordEncoder,
                        JwtService jwtService,
                        AuthenticationManager authenticationManager) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
    }

    public AuthResponse registrar(RegisterRequest request) {
        if (usuarioRepository.existsByEmail(request.getEmail())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ja existe um usuario com esse email");
        }

        Usuario usuario = new Usuario();
        usuario.setNome(request.getNome());
        usuario.setEmail(request.getEmail());
        usuario.setSenhaHash(passwordEncoder.encode(request.getSenha()));
        usuario.setCpf(request.getCpf());
        usuario.setTelefone(request.getTelefone());

        usuarioRepository.save(usuario);

        UsuarioDetailsImpl userDetails = new UsuarioDetailsImpl(usuario);
        String token = jwtService.gerarToken(userDetails);

        return new AuthResponse(token, usuario.getNome(), usuario.getEmail());
    }

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getSenha())
        );

        Usuario usuario = usuarioRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenciais invalidas"));

        UsuarioDetailsImpl userDetails = new UsuarioDetailsImpl(usuario);
        String token = jwtService.gerarToken(userDetails);

        return new AuthResponse(token, usuario.getNome(), usuario.getEmail());
    }
}
