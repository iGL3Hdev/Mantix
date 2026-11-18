package com.mantix.web.dto;

import com.mantix.domain.Usuario;

public record ResumenUsuario(Long id, String nombre) {

	public static ResumenUsuario de(Usuario usuario) {
		return new ResumenUsuario(usuario.getId(), usuario.getNombre());
	}

}
