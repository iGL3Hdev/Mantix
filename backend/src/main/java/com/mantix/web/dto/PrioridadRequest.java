package com.mantix.web.dto;

import com.mantix.domain.Prioridad;

import jakarta.validation.constraints.NotNull;

public record PrioridadRequest(
		@NotNull(message = "La prioridad es obligatoria.")
		Prioridad prioridad,

		String comentario) {

}
