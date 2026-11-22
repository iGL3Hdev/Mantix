package com.mantix.web.dto;

import com.mantix.service.DatosOrden;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record OrdenRequest(
		@NotNull(message = "La incidencia es obligatoria.")
		Long incidenciaId,

		Long tecnicoId,

		@NotBlank(message = "La descripción es obligatoria.")
		@Size(max = 5000, message = "La descripción admite como máximo 5000 caracteres.")
		String descripcion) {

	public DatosOrden aDatos() {
		return new DatosOrden(incidenciaId, tecnicoId, descripcion);
	}
}