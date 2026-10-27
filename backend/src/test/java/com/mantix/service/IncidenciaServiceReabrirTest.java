package com.mantix.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import com.mantix.TestcontainersConfiguration;
import com.mantix.domain.AccionHistorial;
import com.mantix.domain.Equipo;
import com.mantix.domain.EstadoEquipo;
import com.mantix.domain.EstadoIncidencia;
import com.mantix.domain.Historial;
import com.mantix.domain.Incidencia;
import com.mantix.domain.Perfil;
import com.mantix.domain.Prioridad;
import com.mantix.domain.TipoEntidad;
import com.mantix.domain.Usuario;
import com.mantix.repository.UbicacionRepository;
import com.mantix.repository.UsuarioRepository;

import jakarta.persistence.EntityManager;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@Transactional
class IncidenciaServiceReabrirTest {

	@Autowired
	private IncidenciaService incidenciaService;

	@Autowired
	private EquipoService equipoService;

	@Autowired
	private HistorialService historialService;

	@Autowired
	private UbicacionRepository ubicaciones;

	@Autowired
	private UsuarioRepository usuarios;

	@Autowired
	private JdbcTemplate jdbc;

	@Autowired
	private EntityManager em;

	private Usuario responsable;
	private Usuario tecnico;
	private Equipo equipo;

	@BeforeEach
	void prepararDatos() {
		responsable = usuarios.findAll().stream()
				.filter(u -> u.getPerfil() == Perfil.RESPONSABLE).findFirst().orElseThrow();
		tecnico = usuarios.findAll().stream()
				.filter(u -> u.getPerfil() == Perfil.TECNICO && u.isActivo()).findFirst().orElseThrow();
		equipo = equipoService.crear(responsable.getId(), new DatosEquipo("BOM-001", "Bomba principal", null,
				null, null, ubicaciones.findAll().getFirst().getId(), null));
	}

	private Incidencia abrir(Prioridad prioridad) {
		return incidenciaService.crear(tecnico.getId(),
				new DatosIncidencia(equipo.getId(), "Fuga en el sello", "Gotea agua por el eje.", prioridad));
	}

	// Aún no hay forma de cerrar una incidencia desde el servicio (llegará con las órdenes),
	// así que la dejamos cerrada directamente en la base de datos, como lo haría una orden validada.
	private void cerrarPorSql(Incidencia incidencia) {
		em.flush();
		jdbc.update("UPDATE incidencia SET estado = 'CERRADA', fecha_cierre = '2026-10-20 10:00:00' WHERE id = ?",
				incidencia.getId());
		jdbc.update("UPDATE equipo SET estado = 'OPERATIVO' WHERE id = ? AND estado = 'AVERIADO'",
				incidencia.getEquipo().getId());
		em.clear();
	}

	@Test
	void elResponsableReabreUnaIncidenciaCerradaYVaciaLaFechaDeCierre() {
		Incidencia incidencia = abrir(Prioridad.ALTA);
		cerrarPorSql(incidencia);

		Incidencia reabierta = incidenciaService.reabrir(responsable.getId(), incidencia.getId(),
				"La avería ha vuelto");

		assertThat(reabierta.getEstado()).isEqualTo(EstadoIncidencia.ABIERTA);
		assertThat(reabierta.getFechaCierre()).isNull();
		List<Historial> filas = historialService.consultar(TipoEntidad.INCIDENCIA, incidencia.getId());
		Historial reapertura = filas.getLast();
		assertThat(reapertura.getAccion()).isEqualTo(AccionHistorial.CAMBIO_ESTADO);
		assertThat(reapertura.getValorAnterior()).isEqualTo("CERRADA");
		assertThat(reapertura.getValorNuevo()).isEqualTo("ABIERTA");
		assertThat(reapertura.getUsuario().getId()).isEqualTo(responsable.getId());
		assertThat(reapertura.getComentario()).isEqualTo("La avería ha vuelto");
	}

	@Test
	void rn05UnTecnicoNoPuedeReabrir() {
		Incidencia incidencia = abrir(Prioridad.ALTA);
		cerrarPorSql(incidencia);

		assertThatExceptionOfType(PermisoDenegadoException.class)
				.isThrownBy(() -> incidenciaService.reabrir(tecnico.getId(), incidencia.getId(), "Ha vuelto"))
				.extracting(PermisoDenegadoException::getCodigo).isEqualTo("RN-05");
	}

	@Test
	void unaIncidenciaQueNoExisteDevuelveNoEncontrado() {
		assertThatExceptionOfType(RecursoNoEncontradoException.class)
				.isThrownBy(() -> incidenciaService.reabrir(responsable.getId(), 999_999L, "Ha vuelto"))
				.extracting(RecursoNoEncontradoException::getCodigo).isEqualTo("NO_ENCONTRADO");
	}

	@Test
	void rn17NoSeReabreUnaIncidenciaQueNoEstaCerrada() {
		Incidencia incidencia = abrir(Prioridad.ALTA);

		assertThatExceptionOfType(ReglaNegocioException.class)
				.isThrownBy(() -> incidenciaService.reabrir(responsable.getId(), incidencia.getId(), "Ha vuelto"))
				.extracting(ReglaNegocioException::getCodigo).isEqualTo("RN-17");
	}

	@Test
	void rn17NoSeReabreUnaIncidenciaDescartada() {
		Incidencia incidencia = abrir(Prioridad.ALTA);
		incidenciaService.descartar(responsable.getId(), incidencia.getId(), "Falsa alarma");

		assertThatExceptionOfType(ReglaNegocioException.class)
				.isThrownBy(() -> incidenciaService.reabrir(responsable.getId(), incidencia.getId(), "Ha vuelto"))
				.extracting(ReglaNegocioException::getCodigo).isEqualTo("RN-17");
	}

	@Test
	void rn10ReabrirUnaCriticaAveriaElEquipoYLoEnlazaConLaReapertura() {
		Incidencia incidencia = abrir(Prioridad.CRITICA);
		cerrarPorSql(incidencia);

		incidenciaService.reabrir(responsable.getId(), incidencia.getId(), "La avería ha vuelto");

		assertThat(em.find(Equipo.class, equipo.getId()).getEstado()).isEqualTo(EstadoEquipo.AVERIADO);
		Historial reapertura = historialService.consultar(TipoEntidad.INCIDENCIA, incidencia.getId()).getLast();
		Historial cambioEquipo = historialService.consultar(TipoEntidad.EQUIPO, equipo.getId()).getLast();
		assertThat(cambioEquipo.getValorAnterior()).isEqualTo("OPERATIVO");
		assertThat(cambioEquipo.getValorNuevo()).isEqualTo("AVERIADO");
		assertThat(cambioEquipo.getOrigen().getId()).isEqualTo(reapertura.getId());
	}

	@Test
	void reabrirUnaNoCriticaNoCambiaElEstadoDelEquipo() {
		Incidencia incidencia = abrir(Prioridad.ALTA);
		cerrarPorSql(incidencia);

		incidenciaService.reabrir(responsable.getId(), incidencia.getId(), "Ha vuelto");

		assertThat(em.find(Equipo.class, equipo.getId()).getEstado()).isEqualTo(EstadoEquipo.OPERATIVO);
	}

	@Test
	void rn14NoSeReabreUnaIncidenciaDeUnEquipoDeBaja() {
		Incidencia incidencia = abrir(Prioridad.ALTA);
		cerrarPorSql(incidencia);
		equipoService.darDeBaja(responsable.getId(), equipo.getId(), "Retirada de la planta");

		assertThatExceptionOfType(ReglaNegocioException.class)
				.isThrownBy(() -> incidenciaService.reabrir(responsable.getId(), incidencia.getId(), "Ha vuelto"))
				.extracting(ReglaNegocioException::getCodigo).isEqualTo("RN-14");
	}

}
