package com.safrapay.backend.usuario;

import com.safrapay.backend.entity.Usuario;
import com.safrapay.backend.repository.UsuarioRepository;
import com.safrapay.backend.security.SecurityUtils;
import com.safrapay.backend.usuario.dto.PerfilRequest;
import com.safrapay.backend.usuario.dto.PerfilResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;

    public UsuarioService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    public PerfilResponse buscarPerfil() {
        return PerfilResponse.de(SecurityUtils.usuarioLogado());
    }

    /**
     * Atualiza nome/CPF/telefone/nascimento do usuario logado. E-mail e senha
     * ficam fora daqui de proposito (tem fluxos proprios: alterar-senha, e
     * mudanca de e-mail ainda nao existe).
     *
     * Nao valida unicidade de CPF aqui de proposito, pra ficar consistente
     * com o cadastro (AuthService.registrar), que tambem nao valida - se
     * decidirmos aplicar essa regra, fazer nos dois lugares juntos.
     */
    public PerfilResponse atualizarPerfil(PerfilRequest request) {
        Usuario usuario = SecurityUtils.usuarioLogado();

        usuario.setNome(request.getNome());
        usuario.setCpf(request.getCpf() == null || request.getCpf().isBlank() ? null : request.getCpf());
        usuario.setTelefone(request.getTelefone() == null || request.getTelefone().isBlank() ? null : request.getTelefone());

        if (request.getNascimento() == null || request.getNascimento().isBlank()) {
            usuario.setNascimento(null);
        } else {
            try {
                usuario.setNascimento(java.time.LocalDate.parse(request.getNascimento()));
            } catch (java.time.format.DateTimeParseException e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Data de nascimento invalida");
            }
        }

        usuarioRepository.save(usuario);
        return PerfilResponse.de(usuario);
    }
}
