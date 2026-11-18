package com.mantix.web;

import java.net.URI;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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

import com.mantix.domain.EstadoIncidencia;
import com.mantix.domain.EstadoOrden;
import com.mantix.domain.Incidencia;
import com.mantix.domain.OrdenTrabajo;
import com.mantix.domain.Prioridad;
import com.mantix.repository.HistorialRepository;
import com.mantix.repository.IncidenciaRepository;
import com.mantix.repository.OrdenTrabajoRepository;
import com.mantix.repository.HistorialRepository;
import com.mantix.service.IncidenciaService;
import com.mantix.service.RecursoNoEncontradoException;
import com.mantix.web.dto.ComentarioRequest;
import com.mantix.web.dto.EntradaHistorialResponse;
import com.mantix.web.dto.IncidenciaDetalleResponse;
import com.mantix.web.dto.IncidenciaRequest;
import com.mantix.web.dto.IncidenciaResponse;
import com.mantix.web.dto.PrioridadRequest;
import com.mantix.web.dto.ResumenOrden;
import com.mantix.web.dto.EntradaHistorialResponse;


import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/incidencias")
public class IncidenciaController {

	private static final Set<EstadoOrden> SIN_CERRAR = EnumSet.of(EstadoOrden.PENDIENTE, EstadoOrden.ASIGNADA,
			EstadoOrden.EN_CURSO, EstadoOrden.RESUELTA);

	private final IncidenciaRepository incidencias;
	private final OrdenTrabajoRepository ordenes;
	private final IncidenciaService servicio;
	private final HistorialRepository historial;

	public IncidenciaController(IncidenciaRepository incidencias, OrdenTrabajoRepository ordenes, IncidenciaService servicio,
								HistorialRepository historial) {
		this.incidencias = incidencias;
		this.ordenes = ordenes;
		this.servicio = servicio;
		this.historial = historial;
	}

	@GetMapping
	public List<IncidenciaResponse> listar(@RequestParam(required = false) EstadoIncidencia estado,
			@RequestParam(required = false) Prioridad prioridad, @RequestParam(required = false) Long equipoId) {
		List<Incidencia> lista = incidencias.buscar(estado, prioridad, equipoId);
		Map<Long, Long> ordenActiva = ordenes
				.findByIncidenciaIdInAndEstadoIn(lista.stream().map(Incidencia::getId).toList(), SIN_CERRAR).stream()
				.collect(Collectors.toMap(o -> o.getIncidencia().getId(), OrdenTrabajo::getId));
		return lista.stream().map(i -> IncidenciaResponse.de(i, ordenActiva.get(i.getId()))).toList();
	}

	@GetMapping("/{id}")
	public IncidenciaDetalleResponse obtener(@PathVariable Long id) {
		Incidencia incidencia = incidencias.findById(id)
				.orElseThrow(() -> new RecursoNoEncontradoException("La incidencia " + id + " no existe."));
		List<OrdenTrabajo> lista = ordenes.findByIncidenciaIdOrderByFechaCreacionAscIdAsc(id);
		Long ordenActivaId = lista.stream().filter(o -> SIN_CERRAR.contains(o.getEstado())).map(OrdenTrabajo::getId)
				.findFirst().orElse(null);
		return new IncidenciaDetalleResponse(IncidenciaResponse.de(incidencia, ordenActivaId),
				lista.stream().map(ResumenOrden::de).toList());
	}

	@PostMapping
	public ResponseEntity<IncidenciaResponse> crear(
			@RequestHeader(value = "X-Usuario-Id", required = false) Long usuarioId,
			@Valid @RequestBody IncidenciaRequest peticion) {
		Incidencia incidencia = servicio.crear(usuarioId, peticion.aDatos());
		URI ubicacion = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
				.buildAndExpand(incidencia.getId()).toUri();
		return ResponseEntity.created(ubicacion).body(IncidenciaResponse.de(incidencia, null));
	}

	@PostMapping("/{id}/prioridad")
	public IncidenciaResponse cambiarPrioridad(
			@RequestHeader(value = "X-Usuario-Id", required = false) Long usuarioId, @PathVariable Long id,
			@Valid @RequestBody PrioridadRequest peticion) {
		Incidencia incidencia = servicio.cambiarPrioridad(usuarioId, id, peticion.prioridad(),
				peticion.comentario());
		return IncidenciaResponse.de(incidencia, ordenActivaDe(id));
	}

	@PostMapping("/{id}/reabrir")
	public IncidenciaResponse reabrir(@RequestHeader(value = "X-Usuario-Id", required = false) Long usuarioId,
			@PathVariable Long id, @Valid @RequestBody ComentarioRequest peticion) {
		Incidencia incidencia = servicio.reabrir(usuarioId, id, peticion.comentario());
		return IncidenciaResponse.de(incidencia, ordenActivaDe(id));
	}

	@GetMapping("/{id}/historial")
	public List<EntradaHistorialResponse> historial(@PathVariable Long id) {
		if (!incidencias.existsById(id)) {
			throw new RecursoNoEncontradoException("La incidencia " + id + " no existe.");
		}
		return historial.deLaIncidenciaYSusOrdenes(id).stream().map(EntradaHistorialResponse::de).toList();
	}


	private Long ordenActivaDe(Long incidenciaId) {
		return ordenes.findByIncidenciaIdInAndEstadoIn(List.of(incidenciaId), SIN_CERRAR).stream()
				.map(OrdenTrabajo::getId).findFirst().orElse(null);
	}

	@PostMapping("/{id}/descartar")
	public IncidenciaResponse descartar(@RequestHeader(value = "X-Usuario-Id", required = false) Long usuarioId,
			@PathVariable Long id, @Valid @RequestBody ComentarioRequest peticion) {
		Incidencia incidencia = servicio.descartar(usuarioId, id, peticion.comentario());
		return IncidenciaResponse.de(incidencia, ordenActivaDe(id));
	}


}
