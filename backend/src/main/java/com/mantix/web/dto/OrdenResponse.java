package com.mantix.web.dto;

import java.time.Instant;

import com.mantix.domain.EstadoOrden;
import com.mantix.domain.Incidencia;
import com.mantix.domain.OrdenTrabajo;

public record OrdenResponse(Long id, ResumenIncidencia incidencia, ResumenEquipo equipo, String descripcion,
		String resolucion, EstadoOrden estado, ResumenUsuario creadaPor, ResumenUsuario tecnico,
		Instant fechaCreacion, Instant fechaInicio, Instant fechaFin) {

	public record ResumenIncidencia(Long id, String titulo) {
	}

	public static OrdenResponse de(OrdenTrabajo orden) {
		Incidencia incidencia = orden.getIncidencia();
		return new OrdenResponse(orden.getId(), new ResumenIncidencia(incidencia.getId(), incidencia.getTitulo()),
				ResumenEquipo.de(incidencia.getEquipo()), orden.getDescripcion(), orden.getResolucion(),
				orden.getEstado(), ResumenUsuario.de(orden.getCreadaPor()),
				orden.getTecnico() != null ? ResumenUsuario.de(orden.getTecnico()) : null,
				orden.getFechaCreacion(), orden.getFechaInicio(), orden.getFechaFin());
	}

}
