package com.mantix.web;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.mantix.domain.EstadoOrden;
import com.mantix.repository.OrdenTrabajoRepository;
import com.mantix.service.RecursoNoEncontradoException;
import com.mantix.web.dto.OrdenResponse;

@RestController
@RequestMapping("/api/ordenes")
public class OrdenController {

	private final OrdenTrabajoRepository ordenes;

	public OrdenController(OrdenTrabajoRepository ordenes) {
		this.ordenes = ordenes;
	}

	@GetMapping
	public List<OrdenResponse> listar(@RequestParam(required = false) EstadoOrden estado,
			@RequestParam(required = false) Long tecnicoId, @RequestParam(required = false) Long incidenciaId) {
		return ordenes.buscar(estado, tecnicoId, incidenciaId).stream().map(OrdenResponse::de).toList();
	}

	@GetMapping("/{id}")
	public OrdenResponse obtener(@PathVariable Long id) {
		return ordenes.findById(id).map(OrdenResponse::de)
				.orElseThrow(() -> new RecursoNoEncontradoException("La orden " + id + " no existe."));
	}

}
