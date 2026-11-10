package com.mantix.web;

import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.mantix.domain.Perfil;
import com.mantix.repository.UsuarioRepository;
import com.mantix.service.RecursoNoEncontradoException;
import com.mantix.web.dto.UsuarioResponse;

@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {

	private final UsuarioRepository usuarios;

	public UsuarioController(UsuarioRepository usuarios) {
		this.usuarios = usuarios;
	}

	@GetMapping
	public List<UsuarioResponse> listar(@RequestParam(required = false) Perfil perfil,
			@RequestParam(required = false) Boolean activo) {
		return usuarios.findAll(Sort.by("nombre")).stream()
				.filter(u -> perfil == null || u.getPerfil() == perfil)
				.filter(u -> activo == null || u.isActivo() == activo)
				.map(UsuarioResponse::de)
				.toList();
	}

	@GetMapping("/{id}")
	public UsuarioResponse obtener(@PathVariable Long id) {
		return usuarios.findById(id).map(UsuarioResponse::de)
				.orElseThrow(() -> new RecursoNoEncontradoException("El usuario " + id + " no existe."));
	}

}