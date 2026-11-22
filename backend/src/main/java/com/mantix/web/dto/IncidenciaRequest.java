package com.mantix.web.dto;

import com.mantix.domain.Prioridad;
import com.mantix.service.DatosIncidencia;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record IncidenciaRequest (
            @NotNull(message = "El equipo es obligatorio.")
            Long equipoId,

            @NotBlank(message = "El título es obligatorio.")
            @Size(max = 150, message = "El título admite como máximo 150 caracteres.")
            String titulo,

            @NotBlank(message = "La descripción es obligatoria.")
            @Size(max = 5000, message = "La descripción admite como máximo 5000 caracteres.")
            String descripcion,

            @NotNull(message = "La prioridad es obligatoria.")
            Prioridad prioridad) {
    
    public DatosIncidencia aDatos() {
        return new DatosIncidencia(equipoId, titulo, descripcion, prioridad);

    }
    
}
