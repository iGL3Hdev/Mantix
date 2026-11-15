package com.mantix.web.dto;

import com.mantix.service.CambiosEquipo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record EquipoEdicionRequest(
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
		Long ubicacionId) {

	public CambiosEquipo aCambios() {
		return new CambiosEquipo(nombre, fabricante, modelo, numeroSerie, ubicacionId);
	}

}
