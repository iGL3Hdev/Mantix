package com.mantix.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResolucionRequest(
		@NotBlank(message = "La resolución es obligatoria.")
		@Size(max = 5000, message = "La resolución admite como máximo 5000 caracteres.")
		String resolucion) {

}
