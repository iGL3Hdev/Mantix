package com.mantix.repository;

import java.util.Collection;

import org.springframework.data.jpa.repository.JpaRepository;

import com.mantix.domain.EstadoIncidencia;
import com.mantix.domain.Incidencia;
import com.mantix.domain.Prioridad;

public interface IncidenciaRepository extends JpaRepository<Incidencia, Long> {

	boolean existsByEquipoIdAndEstadoIn(Long equipoId, Collection<EstadoIncidencia> estados);

	boolean existsByEquipoIdAndPrioridadAndEstadoIn(Long equipoId, Prioridad prioridad,
			Collection<EstadoIncidencia> estados);


}

