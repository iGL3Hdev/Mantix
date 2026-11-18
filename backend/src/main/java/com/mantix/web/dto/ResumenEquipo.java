package com.mantix.web.dto;

import com.mantix.domain.Equipo;

public record ResumenEquipo(Long id, String codigo, String nombre) {

	public static ResumenEquipo de(Equipo equipo) {
		return new ResumenEquipo(equipo.getId(), equipo.getCodigo(), equipo.getNombre());
	}

}
