package com.mantix.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ComentarioRequest(
		@NotBlank(message = "El comentario es obligatorio.")
		@Size(max = 500, message = "El comentario admite como máximo 500 caracteres.")
		String comentario) {

}

