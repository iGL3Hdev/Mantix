package com.mantix.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.mantix.domain.EstadoOrden;
import com.mantix.domain.OrdenTrabajo;

public interface OrdenTrabajoRepository extends JpaRepository<OrdenTrabajo, Long> {

    boolean existsByIncidenciaIdAndEstadoIn(Long incidenciaId, Collection<EstadoOrden> estados);

    @EntityGraph(attributePaths = "tecnico")
    List<OrdenTrabajo> findByIncidenciaIdOrderByFechaCreacionAscIdAsc(Long incidenciaId);

    List<OrdenTrabajo> findByIncidenciaIdInAndEstadoIn(Collection<Long> incidenciaIds,
        Collection<EstadoOrden> estados);

    @EntityGraph(attributePaths = { "incidencia", "incidencia.equipo", "creadaPor", "tecnico" })
    @Query("""
            select o from OrdenTrabajo o
            where (:estado is null or o.estado = :estado)
              and (:tecnicoId is null or o.tecnico.id = :tecnicoId)
              and (:incidenciaId is null or o.incidencia.id = :incidenciaId)
            order by o.fechaCreacion desc, o.id desc
            """)
    List<OrdenTrabajo> buscar(EstadoOrden estado, Long tecnicoId, Long incidenciaId);

    @Override
    @EntityGraph(attributePaths = { "incidencia", "incidencia.equipo", "creadaPor", "tecnico" })
    Optional<OrdenTrabajo> findById(Long id);


}

