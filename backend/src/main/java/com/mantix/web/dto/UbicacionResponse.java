package com.mantix.web.dto;

import com.mantix.domain.Ubicacion;

public record UbicacionResponse (Long id, String nombre, String descripcion) {

    public static UbicacionResponse de(Ubicacion ubicacion) {
        return new UbicacionResponse(ubicacion.getId(), ubicacion.getNombre(), ubicacion.getDescripcion());
    }
    
}
