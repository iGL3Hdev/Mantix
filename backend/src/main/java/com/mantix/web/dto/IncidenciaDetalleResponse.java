package com.mantix.web.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonUnwrapped;

public record IncidenciaDetalleResponse(@JsonUnwrapped IncidenciaResponse incidencia, List<ResumenOrden> ordenes) {
    
}
