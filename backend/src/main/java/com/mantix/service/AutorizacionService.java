package com.mantix.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mantix.domain.Perfil;
import com.mantix.domain.Usuario;
import com.mantix.repository.UsuarioRepository;


@Service
public class AutorizacionService {

    private final UsuarioRepository usuarios;

    public AutorizacionService(UsuarioRepository usuarios) {
        this.usuarios = usuarios;
    }

    @Transactional(readOnly = true)
    public Usuario exigirActivo(Long usuarioId){
        if (usuarioId == null) {
            throw new UsuarioNoIdentificadoException("Falta indicar el usuario que hace la acción.");
        }
        Usuario usuario = usuarios.findById(usuarioId)
            .orElseThrow(() -> new UsuarioNoIdentificadoException("El usuario " + usuarioId + " no existe."));
        if (!usuario.isActivo()) {
            throw new PermisoDenegadoException("RN-04", "El usuario " + usuarioId + " no está activo");
        }
        return usuario;
    }

    @Transactional (readOnly = true)
    public Usuario exigirResponsable(Long usuarioId) {
        Usuario usuario = exigirActivo(usuarioId);
        if(usuario.getPerfil() != Perfil.RESPONSABLE) {
            throw new PermisoDenegadoException("RN-05", "Esta acción está reservada al responsable.");
        }
        return usuario;
    }
}
