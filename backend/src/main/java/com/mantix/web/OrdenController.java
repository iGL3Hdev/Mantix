package com.mantix.web;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.mantix.domain.EstadoOrden;
import com.mantix.domain.OrdenTrabajo;
import com.mantix.domain.TipoEntidad;
import com.mantix.repository.HistorialRepository;
import com.mantix.repository.OrdenTrabajoRepository;
import com.mantix.service.OrdenTrabajoService;
import com.mantix.service.RecursoNoEncontradoException;
import com.mantix.web.dto.OrdenRequest;
import com.mantix.web.dto.OrdenResponse;
import com.mantix.web.dto.ResolucionRequest;
import com.mantix.web.dto.TecnicoRequest;
import com.mantix.web.dto.ComentarioRequest;
import com.mantix.web.dto.EntradaHistorialResponse;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/ordenes")
public class OrdenController {

	private final OrdenTrabajoRepository ordenes;
	private final OrdenTrabajoService servicio;
	private final HistorialRepository historial;

	public OrdenController(OrdenTrabajoRepository ordenes, OrdenTrabajoService servicio,
							HistorialRepository historial) {
		this.ordenes = ordenes;
		this.servicio = servicio;
		this.historial = historial;
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

	@PostMapping
	public ResponseEntity<OrdenResponse> crear(
			@RequestHeader(value = "X-Usuario-Id", required = false) Long usuarioId,
			@Valid @RequestBody OrdenRequest peticion) {
		OrdenTrabajo orden = servicio.crear(usuarioId, peticion.aDatos());
		URI ubicacion = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
				.buildAndExpand(orden.getId()).toUri();
		return ResponseEntity.created(ubicacion).body(OrdenResponse.de(orden));
	}

	@PostMapping("/{id}/asignar")
	public OrdenResponse asignar(@RequestHeader(value = "X-Usuario-Id", required = false) Long usuarioId,
				@PathVariable Long id, @Valid @RequestBody TecnicoRequest peticion) {
					return OrdenResponse.de(servicio.asignar(usuarioId, id, peticion.tecnicoId()));
	}

	@PostMapping("/{id}/reasignar")
	public OrdenResponse reasignar(@RequestHeader(value = "X-Usuario-Id", required = false) Long usuarioId,
			@PathVariable Long id, @Valid @RequestBody TecnicoRequest peticion) {
		return OrdenResponse.de(servicio.reasignar(usuarioId, id, peticion.tecnicoId()));
	}

		@PostMapping("/{id}/empezar")
	public OrdenResponse empezar(@RequestHeader(value = "X-Usuario-Id", required = false) Long usuarioId,
			@PathVariable Long id) {
		return OrdenResponse.de(servicio.empezar(usuarioId, id));
	}

	@PostMapping("/{id}/resolver")
	public OrdenResponse resolver(@RequestHeader(value = "X-Usuario-Id", required = false) Long usuarioId,
			@PathVariable Long id, @Valid @RequestBody ResolucionRequest peticion) {
		return OrdenResponse.de(servicio.resolver(usuarioId, id, peticion.resolucion()));
	}

	@PostMapping("/{id}/validar")
	public OrdenResponse validar(@RequestHeader(value = "X-Usuario-Id", required = false) Long usuarioId,
					@PathVariable Long id) {
			return OrdenResponse.de(servicio.validar(usuarioId, id));

	}

	@PostMapping("/{id}/rechazar")
	public OrdenResponse rechazar(@RequestHeader(value = "X-Usuario-Id", required = false) Long usuarioId,
			@PathVariable Long id, @Valid @RequestBody ComentarioRequest peticion) {
		return OrdenResponse.de(servicio.rechazar(usuarioId, id, peticion.comentario()));
	}

	@PostMapping("/{id}/cancelar")
	public OrdenResponse cancelar(@RequestHeader(value = "X-Usuario-Id", required = false) Long usuarioId,
			@PathVariable Long id, @Valid @RequestBody ComentarioRequest peticion) {
		return OrdenResponse.de(servicio.cancelar(usuarioId, id, peticion.comentario()));
	}

	@GetMapping("/{id}/historial")
	public List<EntradaHistorialResponse> historial(@PathVariable Long id) {
		if (!ordenes.existsById(id)) {
			throw new RecursoNoEncontradoException("La orden " + id + " no existe.");
		}
		return historial.findByTipoEntidadAndEntidadIdOrderByFechaAscIdAsc(TipoEntidad.ORDEN, id).stream()
				.map(EntradaHistorialResponse::de).toList();
	}

}
