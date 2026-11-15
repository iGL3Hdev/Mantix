package com.mantix.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;

import com.mantix.domain.Historial;
import com.mantix.domain.TipoEntidad;

public interface HistorialRepository extends JpaRepository<Historial, Long> {

	@EntityGraph(attributePaths = "usuario")
	List<Historial> findByTipoEntidadAndEntidadIdOrderByFechaAscIdAsc(TipoEntidad tipoEntidad, Long entidadId);

}
