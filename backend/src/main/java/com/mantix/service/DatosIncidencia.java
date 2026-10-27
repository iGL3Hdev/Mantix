package com.mantix.service;

import com.mantix.domain.Prioridad;

public record DatosIncidencia(Long equipoId, String titulo, String descripcion, Prioridad prioridad) {
    
}
