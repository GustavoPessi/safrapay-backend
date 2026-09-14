package com.safrapay.backend.auth;

import com.safrapay.backend.auth.dto.AlterarSenhaRequest;
import com.safrapay.backend.auth.dto.AuthResponse;
import com.safrapay.backend.auth.dto.EsqueciSenhaRequest;
import com.safrapay.backend.auth.dto.LoginRequest;
import com.safrapay.backend.auth.dto.MensagemResponse;
import com.safrapay.backend.auth.dto.RedefinirSenhaRequest;
import com.safrapay.backend.auth.dto.RegisterRequest;
import com.safrapay.backend.email.EmailService;
import com.safrapay.backend.entity.TipoToken;
import com.safrapay.backend.entity.Usuario;
import com.safrapay.backend.repository.UsuarioRepository;
import com.safrapay.backend.security.JwtService;
import com.safrapay.backend.security.SecurityUtils;
import com.safrapay.backend.security.UsuarioDetailsImpl;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

@Service
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final TokenService tokenService;
    private final EmailService emailService;

    public AuthService(UsuarioRepository usuarioRepository,
                        PasswordEncoder passwordEncoder,
                        JwtService jwtService,
                        AuthenticationManager authenticationManager,
                        TokenService tokenService,
                        EmailService emailService) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
        this.tokenService = tokenService;
        this.emailService = emailService;
    }

    public MensagemResponse registrar(RegisterRequest request) {
        if (usuarioRepository.existsByEmail(request.getEmail())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ja existe um usuario com esse email");
        }

        Usuario usuario = new Usuario();
        usuario.setNome(request.getNome());
        usuario.setEmail(request.getEmail());
        usuario.setSenhaHash(passwordEncoder.encode(request.getSenha()));
        usuario.setCpf(request.getCpf());
        usuario.setTelefone(request.getTelefone());
        if (request.getNascimento() != null && !request.getNascimento().isBlank()) {
            try {
                usuario.setNascimento(java.time.LocalDate.parse(request.getNascimento()));
            } catch (java.time.format.DateTimeParseException e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Data de nascimento invalida");
            }
        }
        usuario.setEmailVerificado(false);

        usuarioRepository.save(usuario);

        String token = tokenService.gerarTokenConfirmacaoEmail(usuario);
        emailService.enviarConfirmacaoEmail(usuario.getEmail(), usuario.getNome(), token);

        return new MensagemResponse("Cadastro realizado. Verifique seu e-mail para confirmar a conta antes de fazer login.");
    }

    public AuthResponse login(LoginRequest request) {
        // Capturamos a excecao aqui (em vez de deixar propagar) porque, sem
        // configuracao de AuthenticationEntryPoint, o Spring Security devolve
        // 403 para qualquer AuthenticationException - convertendo pra
        // ResponseStatusException nos garantimos o 401 correto.
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getSenha())
            );
        } catch (AuthenticationException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenciais invalidas");
        }

        Usuario usuario = usuarioRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenciais invalidas"));

        if (!usuario.isEmailVerificado()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Confirme seu e-mail antes de fazer login");
        }

        UsuarioDetailsImpl userDetails = new UsuarioDetailsImpl(usuario);
        String token = jwtService.gerarToken(userDetails);

        return new AuthResponse(
                token,
                usuario.getNome(),
                usuario.getEmail(),
                usuario.getCpf(),
                usuario.getTelefone(),
                usuario.getNascimento() == null ? null : usuario.getNascimento().toString());
    }

    public MensagemResponse confirmarEmail(String token) {
        Usuario usuario = tokenService.consumirToken(token, TipoToken.CONFIRMACAO_EMAIL);
        usuario.setEmailVerificado(true);
        usuarioRepository.save(usuario);
        return new MensagemResponse("E-mail confirmado com sucesso. Voce ja pode fazer login.");
    }

    public MensagemResponse esqueciSenha(EsqueciSenhaRequest request) {
        // Sempre devolvemos a mesma mensagem, exista ou nao o e-mail, para nao
        // deixar alguem descobrir quais e-mails estao cadastrados (enumeration).
        Optional<Usuario> usuarioOpt = usuarioRepository.findByEmail(request.getEmail());
        usuarioOpt.ifPresent(usuario -> {
            String token = tokenService.gerarTokenResetSenha(usuario);
            emailService.enviarRedefinicaoSenha(usuario.getEmail(), usuario.getNome(), token);
        });
        return new MensagemResponse("Se o e-mail existir na nossa base, voce vai receber um link de redefinicao de senha.");
    }

    public MensagemResponse redefinirSenha(RedefinirSenhaRequest request) {
        Usuario usuario = tokenService.consumirToken(request.getToken(), TipoToken.RESET_SENHA);
        usuario.setSenhaHash(passwordEncoder.encode(request.getNovaSenha()));
        usuarioRepository.save(usuario);
        return new MensagemResponse("Senha redefinida com sucesso.");
    }

    /**
     * Troca de senha para um usuario ja autenticado (tela "Alterar senha" do
     * app, dentro de Configuracoes). Diferente do fluxo de "esqueci minha
     * senha", aqui nao ha e-mail envolvido: exigimos a senha atual para
     * confirmar que quem esta fazendo a troca eh realmente o dono da conta.
     */
    public MensagemResponse alterarSenha(AlterarSenhaRequest request) {
        Usuario usuario = SecurityUtils.usuarioLogado();

        if (!passwordEncoder.matches(request.getSenhaAtual(), usuario.getSenhaHash())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Senha atual incorreta");
        }

        usuario.setSenhaHash(passwordEncoder.encode(request.getNovaSenha()));
        usuarioRepository.save(usuario);

        return new MensagemResponse("Senha alterada com sucesso.");
    }
}
