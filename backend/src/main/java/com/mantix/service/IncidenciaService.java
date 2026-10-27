package com.mantix.service;

import java.time.Clock;
import java.time.Instant;

import java.util.EnumSet;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mantix.domain.AccionHistorial;
import com.mantix.domain.Equipo;
import com.mantix.domain.EstadoEquipo;
import com.mantix.domain.Historial;
import com.mantix.domain.Incidencia;
import com.mantix.domain.TipoEntidad;
import com.mantix.domain.Usuario;
import com.mantix.repository.EquipoRepository;
import com.mantix.repository.IncidenciaRepository;
import com.mantix.domain.Prioridad;
import com.mantix.domain.EstadoIncidencia;

@Service
public class IncidenciaService {

	private final IncidenciaRepository incidencias;
	private final EquipoRepository equipos;
	private final EquipoService equipoService;
	private final AutorizacionService autorizacion;
	private final HistorialService historial;
	private final Clock reloj;

	private static final Set<EstadoIncidencia> SIN_CERRAR = EnumSet.of(EstadoIncidencia.ABIERTA,
		EstadoIncidencia.EN_CURSO);


	public IncidenciaService(IncidenciaRepository incidencias, EquipoRepository equipos,
			EquipoService equipoService, AutorizacionService autorizacion, HistorialService historial,
			Clock reloj) {
		this.incidencias = incidencias;
		this.equipos = equipos;
		this.equipoService = equipoService;
		this.autorizacion = autorizacion;
		this.historial = historial;
		this.reloj = reloj;
	}

	@Transactional
	public Incidencia crear(Long usuarioId, DatosIncidencia datos) {
		Usuario usuario = autorizacion.exigirActivo(usuarioId);
		Equipo equipo = equipos.findById(datos.equipoId()).orElseThrow(
				() -> new RecursoNoEncontradoException("El equipo " + datos.equipoId() + " no existe."));
		if (equipo.getEstado() == EstadoEquipo.BAJA) {
			throw new ReglaNegocioException("RN-14", "No se pueden abrir incidencias sobre un equipo de baja.");
		}

		Incidencia incidencia = incidencias.save(new Incidencia(equipo, usuario, datos.titulo(),
				datos.descripcion(), datos.prioridad(), Instant.now(reloj)));
		Historial alta = historial.registrar(TipoEntidad.INCIDENCIA, incidencia.getId(), AccionHistorial.ALTA,
				null, incidencia.getEstado().name(), null, usuario, null);
		equipoService.reevaluarAveria(equipo, usuario, alta);
		return incidencia;
	}

	@Transactional
	public Incidencia cambiarPrioridad(Long usuarioId, Long incidenciaId, Prioridad nueva, String comentario) {
		Usuario usuario = autorizacion.exigirResponsable(usuarioId);
		Incidencia incidencia = buscar(incidenciaId);
		if (!SIN_CERRAR.contains(incidencia.getEstado())) {
			throw new ReglaNegocioException("RN-15",
					"No se puede cambiar la prioridad de una incidencia cerrada o descartada.");
		}
		Prioridad anterior = incidencia.getPrioridad();
		if (anterior == nueva) {
			return incidencia;
		}

		incidencia.cambiarPrioridad(nueva);
		Historial cambio = historial.registrar(TipoEntidad.INCIDENCIA, incidenciaId,
				AccionHistorial.CAMBIO_PRIORIDAD, anterior.name(), nueva.name(), null, usuario, comentario);
		equipoService.reevaluarAveria(incidencia.getEquipo(), usuario, cambio);
		return incidencia;
	}

	@Transactional
	public Incidencia descartar(Long usuarioId, Long incidenciaId, String motivo) {
		Usuario usuario = autorizacion.exigirResponsable(usuarioId);
		Incidencia incidencia = buscar(incidenciaId);
		if (incidencia.getEstado() != EstadoIncidencia.ABIERTA) {
			throw new ReglaNegocioException("RN-16", "Solo se puede descartar una incidencia abierta.");
		}
		EstadoIncidencia anterior = incidencia.getEstado();

		incidencia.descartar();
		Historial descarte = historial.registrar(TipoEntidad.INCIDENCIA, incidenciaId,
				AccionHistorial.CAMBIO_ESTADO, anterior.name(), incidencia.getEstado().name(), null, usuario,
				motivo);
		equipoService.reevaluarAveria(incidencia.getEquipo(), usuario, descarte);
		return incidencia;
	}

	@Transactional
	public Incidencia reabrir(Long usuarioId, Long incidenciaId, String motivo) {
		Usuario usuario = autorizacion.exigirResponsable(usuarioId);
		Incidencia incidencia = buscar(incidenciaId);
		if (incidencia.getEstado() != EstadoIncidencia.CERRADA) {
			throw new ReglaNegocioException("RN-17", "Solo se puede reabrir una incidencia cerrada.");
		}
		if (incidencia.getEquipo().getEstado() == EstadoEquipo.BAJA) {
			throw new ReglaNegocioException("RN-14", "No se puede reabrir una incidencia de un equipo de baja.");
		}
		EstadoIncidencia anterior = incidencia.getEstado();

		incidencia.reabrir();
		Historial reapertura = historial.registrar(TipoEntidad.INCIDENCIA, incidenciaId,
				AccionHistorial.CAMBIO_ESTADO, anterior.name(), incidencia.getEstado().name(), null, usuario,
				motivo);
		equipoService.reevaluarAveria(incidencia.getEquipo(), usuario, reapertura);
		return incidencia;
	}

	private Incidencia buscar(Long incidenciaId) {
		return incidencias.findById(incidenciaId).orElseThrow(
				() -> new RecursoNoEncontradoException("La incidencia " + incidenciaId + " no existe."));
	}
	
	

}
