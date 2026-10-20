package com.mantix.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

import com.mantix.TestcontainersConfiguration;
import com.mantix.domain.Equipo;
import com.mantix.domain.EstadoIncidencia;
import com.mantix.domain.Incidencia;
import com.mantix.domain.Prioridad;
import com.mantix.domain.Usuario;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@Transactional
class IncidenciaRepositoryTest {

	@Autowired
	private IncidenciaRepository incidencias;

	@Autowired
	private EquipoRepository equipos;

	@Autowired
	private UbicacionRepository ubicaciones;

	@Autowired
	private UsuarioRepository usuarios;

	@Test
	void guardaUnaIncidenciaYConservaLaFechaEnUtc() {
		Equipo equipo = equipos.saveAndFlush(
				new Equipo("BOM-001", "Bomba principal", ubicaciones.findAll().getFirst(), LocalDate.of(2026, 10, 20)));
		Usuario usuario = usuarios.findAll().getFirst();
		Instant apertura = Instant.parse("2026-10-20T09:30:00Z");

		Incidencia guardada = incidencias.saveAndFlush(
				new Incidencia(equipo, usuario, "Fuga en el sello", "Gotea agua por el eje.", Prioridad.ALTA, apertura));

		Incidencia leida = incidencias.findById(guardada.getId()).orElseThrow();
		assertThat(leida.getEstado()).isEqualTo(EstadoIncidencia.ABIERTA);
		assertThat(leida.getPrioridad()).isEqualTo(Prioridad.ALTA);
		assertThat(leida.getFechaApertura()).isEqualTo(apertura);
		assertThat(leida.getFechaCierre()).isNull();
		assertThat(leida.getEquipo().getCodigo()).isEqualTo("BOM-001");
	}

}
