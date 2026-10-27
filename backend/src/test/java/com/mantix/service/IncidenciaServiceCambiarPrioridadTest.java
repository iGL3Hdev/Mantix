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
class IncidenciaServiceCambiarPrioridadTest {

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
	void elResponsableCambiaLaPrioridadYQuedaEnElHistorial() {
		Incidencia incidencia = abrir(Prioridad.ALTA);

		incidenciaService.cambiarPrioridad(responsable.getId(), incidencia.getId(), Prioridad.MEDIA,
				"Se ha contenido la fuga");

		assertThat(incidencia.getPrioridad()).isEqualTo(Prioridad.MEDIA);
		List<Historial> filas = historialService.consultar(TipoEntidad.INCIDENCIA, incidencia.getId());
		assertThat(filas).hasSize(2);
		Historial cambio = filas.getLast();
		assertThat(cambio.getAccion()).isEqualTo(AccionHistorial.CAMBIO_PRIORIDAD);
		assertThat(cambio.getValorAnterior()).isEqualTo("ALTA");
		assertThat(cambio.getValorNuevo()).isEqualTo("MEDIA");
		assertThat(cambio.getUsuario().getId()).isEqualTo(responsable.getId());
		assertThat(cambio.getComentario()).isEqualTo("Se ha contenido la fuga");
	}

	@Test
	void rn05UnTecnicoNoPuedeCambiarLaPrioridad() {
		Incidencia incidencia = abrir(Prioridad.ALTA);

		assertThatExceptionOfType(PermisoDenegadoException.class)
				.isThrownBy(() -> incidenciaService.cambiarPrioridad(tecnico.getId(), incidencia.getId(),
						Prioridad.MEDIA, null))
				.extracting(PermisoDenegadoException::getCodigo).isEqualTo("RN-05");
	}

	@Test
	void unaIncidenciaQueNoExisteDevuelveNoEncontrado() {
		assertThatExceptionOfType(RecursoNoEncontradoException.class)
				.isThrownBy(() -> incidenciaService.cambiarPrioridad(responsable.getId(), 999_999L,
						Prioridad.MEDIA, null))
				.extracting(RecursoNoEncontradoException::getCodigo).isEqualTo("NO_ENCONTRADO");
	}

	@Test
	void rn15NoSeCambiaLaPrioridadDeUnaIncidenciaCerrada() {
		Incidencia incidencia = abrir(Prioridad.ALTA);
		em.flush();
		jdbc.update("UPDATE incidencia SET estado = 'CERRADA' WHERE id = ?", incidencia.getId());
		em.clear();

		assertThatExceptionOfType(ReglaNegocioException.class)
				.isThrownBy(() -> incidenciaService.cambiarPrioridad(responsable.getId(), incidencia.getId(),
						Prioridad.MEDIA, null))
				.extracting(ReglaNegocioException::getCodigo).isEqualTo("RN-15");
	}

	@Test
	void subirLaPrioridadACriticaAveriaElEquipoYLoEnlazaConElCambio() {
		Incidencia incidencia = abrir(Prioridad.ALTA);

		incidenciaService.cambiarPrioridad(responsable.getId(), incidencia.getId(), Prioridad.CRITICA, null);

		assertThat(equipo.getEstado()).isEqualTo(EstadoEquipo.AVERIADO);
		Historial cambioPrioridad = historialService.consultar(TipoEntidad.INCIDENCIA, incidencia.getId())
				.getLast();
		Historial cambioEquipo = historialService.consultar(TipoEntidad.EQUIPO, equipo.getId()).getLast();
		assertThat(cambioEquipo.getValorNuevo()).isEqualTo("AVERIADO");
		assertThat(cambioEquipo.getOrigen().getId()).isEqualTo(cambioPrioridad.getId());
	}

	@Test
	void rn11BajarLaUnicaCriticaDevuelveElEquipoAOperativo() {
		Incidencia incidencia = abrir(Prioridad.CRITICA);

		incidenciaService.cambiarPrioridad(responsable.getId(), incidencia.getId(), Prioridad.ALTA, null);

		assertThat(equipo.getEstado()).isEqualTo(EstadoEquipo.OPERATIVO);
		Historial cambioPrioridad = historialService.consultar(TipoEntidad.INCIDENCIA, incidencia.getId())
				.getLast();
		Historial cambioEquipo = historialService.consultar(TipoEntidad.EQUIPO, equipo.getId()).getLast();
		assertThat(cambioEquipo.getValorAnterior()).isEqualTo("AVERIADO");
		assertThat(cambioEquipo.getValorNuevo()).isEqualTo("OPERATIVO");
		assertThat(cambioEquipo.getOrigen().getId()).isEqualTo(cambioPrioridad.getId());
	}

	@Test
	void conOtraCriticaAbiertaBajarUnaNoRecuperaElEquipo() {
		Incidencia primera = abrir(Prioridad.CRITICA);
		abrir(Prioridad.CRITICA);

		incidenciaService.cambiarPrioridad(responsable.getId(), primera.getId(), Prioridad.ALTA, null);

		assertThat(equipo.getEstado()).isEqualTo(EstadoEquipo.AVERIADO);
	}

	@Test
	void pedirLaMismaPrioridadNoCambiaNadaNiRegistra() {
		Incidencia incidencia = abrir(Prioridad.ALTA);

		incidenciaService.cambiarPrioridad(responsable.getId(), incidencia.getId(), Prioridad.ALTA, null);

		assertThat(historialService.consultar(TipoEntidad.INCIDENCIA, incidencia.getId())).hasSize(1);
	}

}

