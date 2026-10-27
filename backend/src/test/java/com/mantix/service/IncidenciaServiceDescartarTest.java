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
class IncidenciaServiceDescartarTest {

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

	@Test
	void elResponsableDescartaUnaIncidenciaAbiertaYQuedaEnElHistorial() {
		Incidencia incidencia = abrir(Prioridad.ALTA);

		incidenciaService.descartar(responsable.getId(), incidencia.getId(), "Falsa alarma");

		assertThat(incidencia.getEstado()).isEqualTo(EstadoIncidencia.DESCARTADA);
		assertThat(incidencia.getFechaCierre()).isNull();
		List<Historial> filas = historialService.consultar(TipoEntidad.INCIDENCIA, incidencia.getId());
		assertThat(filas).hasSize(2);
		Historial descarte = filas.getLast();
		assertThat(descarte.getAccion()).isEqualTo(AccionHistorial.CAMBIO_ESTADO);
		assertThat(descarte.getValorAnterior()).isEqualTo("ABIERTA");
		assertThat(descarte.getValorNuevo()).isEqualTo("DESCARTADA");
		assertThat(descarte.getUsuario().getId()).isEqualTo(responsable.getId());
		assertThat(descarte.getComentario()).isEqualTo("Falsa alarma");
	}

	@Test
	void rn05UnTecnicoNoPuedeDescartar() {
		Incidencia incidencia = abrir(Prioridad.ALTA);

		assertThatExceptionOfType(PermisoDenegadoException.class)
				.isThrownBy(() -> incidenciaService.descartar(tecnico.getId(), incidencia.getId(), "Duplicada"))
				.extracting(PermisoDenegadoException::getCodigo).isEqualTo("RN-05");
	}

	@Test
	void unaIncidenciaQueNoExisteDevuelveNoEncontrado() {
		assertThatExceptionOfType(RecursoNoEncontradoException.class)
				.isThrownBy(() -> incidenciaService.descartar(responsable.getId(), 999_999L, "Duplicada"))
				.extracting(RecursoNoEncontradoException::getCodigo).isEqualTo("NO_ENCONTRADO");
	}

	@Test
	void rn16NoSeDescartaUnaIncidenciaEnCurso() {
		Incidencia incidencia = abrir(Prioridad.ALTA);
		em.flush();
		jdbc.update("UPDATE incidencia SET estado = 'EN_CURSO' WHERE id = ?", incidencia.getId());
		em.clear();

		assertThatExceptionOfType(ReglaNegocioException.class)
				.isThrownBy(() -> incidenciaService.descartar(responsable.getId(), incidencia.getId(), "Duplicada"))
				.extracting(ReglaNegocioException::getCodigo).isEqualTo("RN-16");
	}

	@Test
	void rn16NoSeDescartaDosVecesLaMismaIncidencia() {
		Incidencia incidencia = abrir(Prioridad.ALTA);
		incidenciaService.descartar(responsable.getId(), incidencia.getId(), "Falsa alarma");

		assertThatExceptionOfType(ReglaNegocioException.class)
				.isThrownBy(() -> incidenciaService.descartar(responsable.getId(), incidencia.getId(), "Otra vez"))
				.extracting(ReglaNegocioException::getCodigo).isEqualTo("RN-16");
	}

	@Test
	void rn11DescartarLaUnicaCriticaDevuelveElEquipoAOperativo() {
		Incidencia incidencia = abrir(Prioridad.CRITICA);

		incidenciaService.descartar(responsable.getId(), incidencia.getId(), "Falsa alarma");

		assertThat(equipo.getEstado()).isEqualTo(EstadoEquipo.OPERATIVO);
		Historial descarte = historialService.consultar(TipoEntidad.INCIDENCIA, incidencia.getId()).getLast();
		Historial cambioEquipo = historialService.consultar(TipoEntidad.EQUIPO, equipo.getId()).getLast();
		assertThat(cambioEquipo.getValorAnterior()).isEqualTo("AVERIADO");
		assertThat(cambioEquipo.getValorNuevo()).isEqualTo("OPERATIVO");
		assertThat(cambioEquipo.getOrigen().getId()).isEqualTo(descarte.getId());
	}

	@Test
	void conOtraCriticaAbiertaDescartarUnaNoRecuperaElEquipo() {
		Incidencia primera = abrir(Prioridad.CRITICA);
		abrir(Prioridad.CRITICA);

		incidenciaService.descartar(responsable.getId(), primera.getId(), "Duplicada");

		assertThat(equipo.getEstado()).isEqualTo(EstadoEquipo.AVERIADO);
	}

}
