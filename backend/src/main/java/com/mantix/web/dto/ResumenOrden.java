package com.mantix.web.dto;

import java.time.Instant;

import com.mantix.domain.EstadoOrden;
import com.mantix.domain.OrdenTrabajo;

public record ResumenOrden(Long id, EstadoOrden estado, ResumenUsuario tecnico, Instant fechaCreacion) {
    

    public static ResumenOrden de(OrdenTrabajo orden) {
        return new ResumenOrden(orden.getId(), orden.getEstado(),
                    orden.getTecnico() != null ? ResumenUsuario.de(orden.getTecnico()) : null, orden.getFechaCreacion());
    }
}
