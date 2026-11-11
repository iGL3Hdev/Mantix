package com.mantix.web;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.mantix.domain.EstadoEquipo;
import com.mantix.repository.EquipoRepository;
import com.mantix.service.RecursoNoEncontradoException;
import com.mantix.web.dto.EquipoResponse;

@RestController
@RequestMapping("/api/equipos")
public class EquipoController {

	private final EquipoRepository equipos;

	public EquipoController(EquipoRepository equipos) {
		this.equipos = equipos;
	}

	@GetMapping
	public List<EquipoResponse> listar(@RequestParam(required = false) EstadoEquipo estado,
			@RequestParam(required = false) Long ubicacionId, @RequestParam(required = false) String q) {
		return equipos.buscar(estado, ubicacionId, q).stream().map(EquipoResponse::de).toList();
	}

	@GetMapping("/{id}")
	public EquipoResponse obtener(@PathVariable Long id) {
		return equipos.findById(id).map(EquipoResponse::de)
				.orElseThrow(() -> new RecursoNoEncontradoException("El equipo " + id + " no existe."));
	}

}

