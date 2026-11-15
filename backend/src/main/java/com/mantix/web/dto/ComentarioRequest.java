package com.mantix.web.dto;

import jakarta.validation.constraints.NotBlank;

public record ComentarioRequest(
		@NotBlank(message = "El comentario es obligatorio.")
		String comentario) {

}
