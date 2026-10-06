package com.safrapay.backend.security;

import com.safrapay.backend.entity.EquipeMembro;
import com.safrapay.backend.entity.PapelEquipe;
import com.safrapay.backend.entity.Usuario;
import com.safrapay.backend.repository.EquipeMembroRepository;
import com.safrapay.backend.repository.UsuarioRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class UsuarioAutenticadoService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;
    private final EquipeMembroRepository equipeRepository;

    public UsuarioAutenticadoService(UsuarioRepository usuarioRepository, EquipeMembroRepository equipeRepository) {
        this.usuarioRepository = usuarioRepository;
        this.equipeRepository = equipeRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario nao encontrado: " + email));
        // Multiusuário: se a pessoa é membro aceito de uma equipe, trabalha nos dados do dono dela.
        EquipeMembro participacao = equipeRepository.buscarParticipacaoAceita(usuario.getId()).orElse(null);
        if (participacao == null) return new UsuarioAutenticado(usuario);
        PapelAcesso papel = participacao.getPapel() == PapelEquipe.GERENTE ? PapelAcesso.GERENTE : PapelAcesso.COLABORADOR;
        return new UsuarioAutenticado(usuario, participacao.getDono(), papel);
    }
}
