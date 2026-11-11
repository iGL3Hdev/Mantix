package com.mantix.web;

import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mantix.repository.UbicacionRepository;
import com.mantix.web.dto.UbicacionResponse;

@RestController
@RequestMapping("/api/ubicaciones")
public class UbicacionController {

    private final UbicacionRepository ubicaciones;

    public UbicacionController(UbicacionRepository ubicaciones) {
        this.ubicaciones = ubicaciones;
    }

    @GetMapping
    public List<UbicacionResponse> listar() {
        return ubicaciones.findAll(Sort.by("nombre"))
                .stream()
                .map(UbicacionResponse::de)
                .toList();
    }
    
}
