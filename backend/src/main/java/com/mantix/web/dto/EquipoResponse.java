package com.mantix.web.dto;

import java.time.LocalDate;

import com.mantix.domain.Equipo;
import com.mantix.domain.EstadoEquipo;

public record EquipoResponse(Long id, String codigo, String nombre, String fabricante, String modelo,
		String numeroSerie, ResumenUbicacion ubicacion, EstadoEquipo estado, LocalDate fechaAlta) {

	public record ResumenUbicacion(Long id, String nombre) {
	}

	public static EquipoResponse de(Equipo equipo) {
		return new EquipoResponse(equipo.getId(), equipo.getCodigo(), equipo.getNombre(), equipo.getFabricante(),
				equipo.getModelo(), equipo.getNumeroSerie(),
				new ResumenUbicacion(equipo.getUbicacion().getId(), equipo.getUbicacion().getNombre()),
				equipo.getEstado(), equipo.getFechaAlta());
	}

}
