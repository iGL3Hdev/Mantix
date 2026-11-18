package com.mantix.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.mantix.domain.EstadoIncidencia;
import com.mantix.domain.Incidencia;
import com.mantix.domain.Prioridad;

public interface IncidenciaRepository extends JpaRepository<Incidencia, Long> {

	boolean existsByEquipoIdAndEstadoIn(Long equipoId, Collection<EstadoIncidencia> estados);

	boolean existsByEquipoIdAndPrioridadAndEstadoIn(Long equipoId, Prioridad prioridad,
			Collection<EstadoIncidencia> estados);

	@EntityGraph(attributePaths = { "equipo", "reportadaPor" })
	@Query("""
			select i from Incidencia i
			where (:estado is null or i.estado = :estado)
			  and (:prioridad is null or i.prioridad = :prioridad)
			  and (:equipoId is null or i.equipo.id = :equipoId)
			order by i.fechaApertura desc, i.id desc
			""")
	List<Incidencia> buscar(EstadoIncidencia estado, Prioridad prioridad, Long equipoId);

	@Override
	@EntityGraph(attributePaths = { "equipo", "reportadaPor" })
	Optional<Incidencia> findById(Long id);

}


