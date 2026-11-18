package com.mantix.web.dto;

import java.time.Instant;

import com.mantix.domain.EstadoIncidencia;
import com.mantix.domain.Incidencia;
import com.mantix.domain.Prioridad;

public record IncidenciaResponse(Long id, ResumenEquipo equipo, String titulo, String descripcion,
		Prioridad prioridad, EstadoIncidencia estado, ResumenUsuario reportadaPor, Instant fechaApertura,
		Instant fechaCierre, Long ordenActivaId) {

	public static IncidenciaResponse de(Incidencia incidencia, Long ordenActivaId) {
		return new IncidenciaResponse(incidencia.getId(), ResumenEquipo.de(incidencia.getEquipo()),
				incidencia.getTitulo(), incidencia.getDescripcion(), incidencia.getPrioridad(),
				incidencia.getEstado(), ResumenUsuario.de(incidencia.getReportadaPor()),
				incidencia.getFechaApertura(), incidencia.getFechaCierre(), ordenActivaId);
	}

}
