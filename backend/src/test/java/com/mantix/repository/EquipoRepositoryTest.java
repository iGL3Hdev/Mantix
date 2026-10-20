package com.mantix.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import com.mantix.TestcontainersConfiguration;
import com.mantix.domain.Equipo;
import com.mantix.domain.EstadoEquipo;
import com.mantix.domain.Ubicacion;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@Transactional
class EquipoRepositoryTest {

	@Autowired
	private EquipoRepository equipos;

	@Autowired
	private UbicacionRepository ubicaciones;

	@Test
	void guardaUnEquipoConSuUbicacionYLoRecupera() {
		Ubicacion ubicacion = ubicaciones.findAll().getFirst();

		Equipo guardado = equipos.saveAndFlush(
				new Equipo("BOM-001", "Bomba principal", ubicacion, LocalDate.of(2026, 10, 20)));

		Equipo leido = equipos.findById(guardado.getId()).orElseThrow();
		assertThat(leido.getCodigo()).isEqualTo("BOM-001");
		assertThat(leido.getEstado()).isEqualTo(EstadoEquipo.OPERATIVO);
		assertThat(leido.getUbicacion().getId()).isEqualTo(ubicacion.getId());
	}

	@Test
	void noPermiteDosEquiposConElMismoCodigo() {
		Ubicacion ubicacion = ubicaciones.findAll().getFirst();
		equipos.saveAndFlush(new Equipo("BOM-001", "Bomba principal", ubicacion, LocalDate.of(2026, 10, 20)));

		assertThatThrownBy(() -> equipos.saveAndFlush(
				new Equipo("BOM-001", "Otra bomba", ubicacion, LocalDate.of(2026, 10, 20))))
				.isInstanceOf(DataIntegrityViolationException.class);
	}

}
