package com.mantix.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.EntityGraph;

import com.mantix.domain.Equipo;
import com.mantix.domain.EstadoEquipo;

public interface EquipoRepository extends JpaRepository<Equipo, Long> {

    boolean existsByCodigo(String codigo);

        @EntityGraph(attributePaths = "ubicacion" )
        @Query("""
            select e from Equipo e
            where (:estado is null or e.estado = :estado)
              and (:ubicacionId is null or e.ubicacion.id = :ubicacionId)
              and (:q is null
                   or lower(e.codigo) like lower(concat('%', :q, '%'))
                   or lower(e.nombre) like lower(concat('%', :q, '%')))
            order by e.codigo
            """)
    List<Equipo> buscar(EstadoEquipo estado, Long ubicacionId, String q);

    @Override
    @EntityGraph(attributePaths = "ubicacion")
    Optional<Equipo> findById(Long id);

}
