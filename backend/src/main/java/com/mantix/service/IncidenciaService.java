package com.mantix.service;

import java.time.Clock;
import java.time.Instant;

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

@Service
public class IncidenciaService {

	private final IncidenciaRepository incidencias;
	private final EquipoRepository equipos;
	private final EquipoService equipoService;
	private final AutorizacionService autorizacion;
	private final HistorialService historial;
	private final Clock reloj;

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

}
