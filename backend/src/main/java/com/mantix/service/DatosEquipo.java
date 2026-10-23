package com.mantix.service;

import java.time.LocalDate;

public record DatosEquipo(String codigo, String nombre, String fabricante, String modelo, String numeroSerie,
        Long ubicacionId, LocalDate fechaAlta) {

            
}
