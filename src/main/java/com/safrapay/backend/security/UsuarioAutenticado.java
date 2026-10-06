package com.safrapay.backend.security;

import com.safrapay.backend.entity.Usuario;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * Adapta a entidade Usuario para o contrato UserDetails do Spring Security.
 *
 * Multiusuário (06/10/2026): além de quem está logado, guarda o papel e o
 * dono da equipe. "dono" é de quem são os dados que a pessoa enxerga: ela
 * mesma (DONO) ou o dono da equipe em que é GERENTE/COLABORADOR.
 */
public class UsuarioAutenticado implements UserDetails {

    private final Usuario usuario;
    private final Usuario dono;
    private final PapelAcesso papel;

    /** Pessoa sem equipe: dona dos próprios dados. */
    public UsuarioAutenticado(Usuario usuario) {
        this(usuario, usuario, PapelAcesso.DONO);
    }

    public UsuarioAutenticado(Usuario usuario, Usuario dono, PapelAcesso papel) {
        this.usuario = usuario;
        this.dono = dono == null ? usuario : dono;
        this.papel = papel == null ? PapelAcesso.DONO : papel;
    }

    /** Quem está logado (perfil, autoria, limite da IA). */
    public Usuario getUsuario() {
        return usuario;
    }

    /** De quem são os dados (propriedades, financeiro, máquinas...). */
    public Usuario getDono() {
        return dono;
    }

    public PapelAcesso getPapel() {
        return papel;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of();
    }

    @Override
    public String getPassword() {
        return usuario.getSenhaHash();
    }

    @Override
    public String getUsername() {
        return usuario.getEmail();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
