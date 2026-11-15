package com.mantix.web.dto;

import java.time.LocalDate;

import com.mantix.service.DatosEquipo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record EquipoRequest(
		@NotBlank(message = "El código es obligatorio.")
		@Size(max = 20, message = "El código admite como máximo 20 caracteres.")
		String codigo,

		@NotBlank(message = "El nombre es obligatorio.")
		@Size(max = 100, message = "El nombre admite como máximo 100 caracteres.")
		String nombre,

		@Size(max = 100, message = "El fabricante admite como máximo 100 caracteres.")
		String fabricante,

		@Size(max = 100, message = "El modelo admite como máximo 100 caracteres.")
		String modelo,

		@Size(max = 100, message = "El número de serie admite como máximo 100 caracteres.")
		String numeroSerie,

		@NotNull(message = "La ubicación es obligatoria.")
		Long ubicacionId,

		LocalDate fechaAlta) {

	public DatosEquipo aDatos() {
		return new DatosEquipo(codigo, nombre, fabricante, modelo, numeroSerie, ubicacionId, fechaAlta);
	}

}