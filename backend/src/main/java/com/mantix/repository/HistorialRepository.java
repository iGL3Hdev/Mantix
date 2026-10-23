package com.mantix.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.mantix.domain.Historial;
import com.mantix.domain.TipoEntidad;

public interface HistorialRepository extends JpaRepository<Historial, Long> {

	List<Historial> findByTipoEntidadAndEntidadIdOrderByFechaAscIdAsc(TipoEntidad tipoEntidad, Long entidadId);

}
