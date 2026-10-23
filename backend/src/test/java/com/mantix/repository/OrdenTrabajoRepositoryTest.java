package com.mantix.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

import com.mantix.TestcontainersConfiguration;
import com.mantix.domain.Equipo;
import com.mantix.domain.EstadoOrden;
import com.mantix.domain.Incidencia;
import com.mantix.domain.OrdenTrabajo;
import com.mantix.domain.Perfil;
import com.mantix.domain.Prioridad;
import com.mantix.domain.Usuario;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@Transactional
class OrdenTrabajoRepositoryTest {

	private static final Instant AHORA = Instant.parse("2026-10-21T08:00:00Z");

	@Autowired
	private OrdenTrabajoRepository ordenes;

	@Autowired
	private IncidenciaRepository incidencias;

	@Autowired
	private EquipoRepository equipos;

	@Autowired
	private UbicacionRepository ubicaciones;

	@Autowired
	private UsuarioRepository usuarios;

	private Incidencia incidencia;
	private Usuario responsable;
	private Usuario tecnico;

	@BeforeEach
	void prepararDatos() {
		List<Usuario> todos = usuarios.findAll();
		responsable = todos.stream().filter(u -> u.getPerfil() == Perfil.RESPONSABLE).findFirst().orElseThrow();
		tecnico = todos.stream().filter(u -> u.getPerfil() == Perfil.TECNICO).findFirst().orElseThrow();

		Equipo equipo = equipos.saveAndFlush(
				new Equipo("BOM-001", "Bomba principal", ubicaciones.findAll().getFirst(), LocalDate.of(2026, 10, 21)));
		incidencia = incidencias.saveAndFlush(
				new Incidencia(equipo, responsable, "Fuga en el sello", "Gotea agua por el eje.", Prioridad.ALTA, AHORA));
	}

	@Test
	void unaOrdenSinTecnicoNaceComoPendiente() {
		OrdenTrabajo guardada = ordenes.saveAndFlush(
				new OrdenTrabajo(incidencia, responsable, null, "Cambiar el sello mecánico.", AHORA));

		OrdenTrabajo leida = ordenes.findById(guardada.getId()).orElseThrow();
		assertThat(leida.getEstado()).isEqualTo(EstadoOrden.PENDIENTE);
		assertThat(leida.getTecnico()).isNull();
		assertThat(leida.getIncidencia().getId()).isEqualTo(incidencia.getId());
	}

	@Test
	void unaOrdenConTecnicoNaceComoAsignada() {
		OrdenTrabajo guardada = ordenes.saveAndFlush(
				new OrdenTrabajo(incidencia, responsable, tecnico, "Cambiar el sello mecánico.", AHORA));

		OrdenTrabajo leida = ordenes.findById(guardada.getId()).orElseThrow();
		assertThat(leida.getEstado()).isEqualTo(EstadoOrden.ASIGNADA);
		assertThat(leida.getTecnico().getId()).isEqualTo(tecnico.getId());
		assertThat(leida.getFechaInicio()).isNull();
		assertThat(leida.getFechaFin()).isNull();
	}

}
