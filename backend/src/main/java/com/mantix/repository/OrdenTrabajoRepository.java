package com.mantix.repository;

import java.util.Collection;

import org.springframework.data.jpa.repository.JpaRepository;

import com.mantix.domain.EstadoOrden;
import com.mantix.domain.OrdenTrabajo;

public interface OrdenTrabajoRepository extends JpaRepository<OrdenTrabajo, Long> {

    boolean existsByIncidenciaIdAndEstadoIn(Long incidenciaId, Collection<EstadoOrden> estados);

}
