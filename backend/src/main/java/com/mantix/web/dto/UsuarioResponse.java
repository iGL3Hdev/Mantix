package com.mantix.web.dto;

import com.mantix.domain.Perfil;
import com.mantix.domain.Usuario;

public record UsuarioResponse(Long id, String nombre, String email, Perfil perfil, boolean activo) {

    public static UsuarioResponse de(Usuario usuario) {
        return new UsuarioResponse(
                usuario.getId(),
                usuario.getNombre(),
                usuario.getEmail(),
                usuario.getPerfil(),
                usuario.isActivo()
        );
    }
    
}
