package com.mantix.web.dto;

import jakarta.validation.constraints.NotNull;

public record TecnicoRequest (
    @NotNull(message = "El técnico es obligatorio.")
    Long tecnicoId) {
        
}
