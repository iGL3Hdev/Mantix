package com.mantix.web;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.mantix.domain.Equipo;
import com.mantix.domain.EstadoEquipo;
import com.mantix.domain.TipoEntidad;
import com.mantix.repository.EquipoRepository;
import com.mantix.repository.HistorialRepository;
import com.mantix.service.EquipoService;
import com.mantix.service.RecursoNoEncontradoException;
import com.mantix.web.dto.ComentarioRequest;
import com.mantix.web.dto.EntradaHistorialResponse;
import com.mantix.web.dto.EquipoEdicionRequest;
import com.mantix.web.dto.EquipoRequest;
import com.mantix.web.dto.EquipoResponse;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/equipos")
public class EquipoController {

	private final EquipoRepository equipos;
	private final EquipoService servicio;
	private final HistorialRepository historial;

	public EquipoController(EquipoRepository equipos, EquipoService servicio, HistorialRepository historial) {
		this.equipos = equipos;
		this.servicio = servicio;
		this.historial = historial;
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

	@PostMapping
	public ResponseEntity<EquipoResponse> crear(
			@RequestHeader(value = "X-Usuario-Id", required = false) Long usuarioId,
			@Valid @RequestBody EquipoRequest peticion) {
		Equipo equipo = servicio.crear(usuarioId, peticion.aDatos());
		URI ubicacion = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
				.buildAndExpand(equipo.getId()).toUri();
		return ResponseEntity.created(ubicacion).body(EquipoResponse.de(equipo));
	}

	@PutMapping("/{id}")
	public EquipoResponse editar(@RequestHeader(value = "X-Usuario-Id", required = false) Long usuarioId,
			@PathVariable Long id, @Valid @RequestBody EquipoEdicionRequest peticion) {
		return EquipoResponse.de(servicio.editar(usuarioId, id, peticion.aCambios()));
	}

	@PostMapping("/{id}/baja")
	public EquipoResponse darDeBaja(@RequestHeader(value = "X-Usuario-Id", required = false) Long usuarioId,
			@PathVariable Long id, @Valid @RequestBody ComentarioRequest peticion) {
		return EquipoResponse.de(servicio.darDeBaja(usuarioId, id, peticion.comentario()));
	}

	@PostMapping("/{id}/reactivar")
	public EquipoResponse reactivar(@RequestHeader(value = "X-Usuario-Id", required = false) Long usuarioId,
			@PathVariable Long id, @Valid @RequestBody ComentarioRequest peticion) {
		return EquipoResponse.de(servicio.reactivar(usuarioId, id, peticion.comentario()));
	}

	@GetMapping("/{id}/historial")
	public List<EntradaHistorialResponse> historial(@PathVariable Long id) {
		if (!equipos.existsById(id)) {
			throw new RecursoNoEncontradoException("El equipo " + id + " no existe.");
		}
		return historial.findByTipoEntidadAndEntidadIdOrderByFechaAscIdAsc(TipoEntidad.EQUIPO, id).stream()
				.map(EntradaHistorialResponse::de).toList();
	}


}

