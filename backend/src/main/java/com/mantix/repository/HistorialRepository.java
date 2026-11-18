package com.mantix.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;

import com.mantix.domain.Historial;
import com.mantix.domain.TipoEntidad;

public interface HistorialRepository extends JpaRepository<Historial, Long> {

	@EntityGraph(attributePaths = "usuario")
	List<Historial> findByTipoEntidadAndEntidadIdOrderByFechaAscIdAsc(TipoEntidad tipoEntidad, Long entidadId);

	// Las entradas de la incidencia y las de todas sus órdenes, en una sola lista cronológica.
	@EntityGraph(attributePaths = "usuario")
	@Query("""
			select h from Historial h
			where (h.tipoEntidad = com.mantix.domain.TipoEntidad.INCIDENCIA and h.entidadId = :incidenciaId)
			   or (h.tipoEntidad = com.mantix.domain.TipoEntidad.ORDEN
			       and h.entidadId in (select o.id from OrdenTrabajo o where o.incidencia.id = :incidenciaId))
			order by h.fecha asc, h.id asc
			""")
	List<Historial> deLaIncidenciaYSusOrdenes(Long incidenciaId);


}
