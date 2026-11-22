package com.mantix.web.dto;

import com.mantix.domain.Prioridad;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PrioridadRequest(
		@NotNull(message = "La prioridad es obligatoria.")
		Prioridad prioridad,

		@Size(max = 500, message = "El comentario admite como máximo 500 caracteres.")
		String comentario) {

}
