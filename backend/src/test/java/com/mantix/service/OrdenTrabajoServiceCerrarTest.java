package com.mantix.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.time.Instant;
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
import com.mantix.domain.EstadoOrden;
import com.mantix.domain.Historial;
import com.mantix.domain.Incidencia;
import com.mantix.domain.OrdenTrabajo;
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
class OrdenTrabajoServiceCerrarTest {

	@Autowired
	private OrdenTrabajoService ordenService;

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
		equipo = equipoService.crear(responsable.getId(), new DatosEquipo("BOM-001", "Bomba principal",
				null, null, null, ubicaciones.findAll().getFirst().getId(), null));
	}

	private Incidencia abrir(Prioridad prioridad) {
		return incidenciaService.crear(tecnico.getId(),
				new DatosIncidencia(equipo.getId(), "Fuga en el sello", "Gotea agua por el eje.", prioridad));
	}

	private OrdenTrabajo ordenAsignada(Incidencia incidencia) {
		return ordenService.crear(responsable.getId(),
				new DatosOrden(incidencia.getId(), tecnico.getId(), "Cambiar el sello mecánico."));
	}

	private OrdenTrabajo ordenResuelta(Incidencia incidencia) {
		OrdenTrabajo orden = ordenAsignada(incidencia);
		ordenService.empezar(tecnico.getId(), orden.getId());
		ordenService.resolver(tecnico.getId(), orden.getId(), "Sello sustituido.");
		return orden;
	}

	// Cancelar llegará en el trozo siguiente: de momento se fuerza el estado en la base.
	private void forzarEstado(OrdenTrabajo orden, String estado) {
		em.flush();
		jdbc.update("UPDATE orden_trabajo SET estado = ? WHERE id = ?", estado, orden.getId());
		em.clear();
	}

	@Test
	void elResponsableValidaUnaOrdenResueltaYQuedaEnElHistorial() {
		OrdenTrabajo orden = ordenResuelta(abrir(Prioridad.ALTA));

		ordenService.validar(responsable.getId(), orden.getId());

		assertThat(orden.getEstado()).isEqualTo(EstadoOrden.CERRADA);
		Historial validacion = historialService.consultar(TipoEntidad.ORDEN, orden.getId()).getLast();
		assertThat(validacion.getAccion()).isEqualTo(AccionHistorial.CAMBIO_ESTADO);
		assertThat(validacion.getValorAnterior()).isEqualTo("RESUELTA");
		assertThat(validacion.getValorNuevo()).isEqualTo("CERRADA");
		assertThat(validacion.getUsuario().getId()).isEqualTo(responsable.getId());
		assertThat(validacion.getOrigen()).isNull();
	}

	@Test
	void validarLaOrdenCierraLaIncidenciaYLaEnlazaConLaValidacion() {
		Incidencia incidencia = abrir(Prioridad.ALTA);
		OrdenTrabajo orden = ordenResuelta(incidencia);
		Instant antes = Instant.now();

		ordenService.validar(responsable.getId(), orden.getId());

		assertThat(incidencia.getEstado()).isEqualTo(EstadoIncidencia.CERRADA);
		assertThat(incidencia.getFechaCierre()).isBetween(antes, Instant.now());
		Historial validacion = historialService.consultar(TipoEntidad.ORDEN, orden.getId()).getLast();
		Historial cierre = historialService.consultar(TipoEntidad.INCIDENCIA, incidencia.getId()).getLast();
		assertThat(cierre.getAccion()).isEqualTo(AccionHistorial.CAMBIO_ESTADO);
		assertThat(cierre.getValorAnterior()).isEqualTo("EN_CURSO");
		assertThat(cierre.getValorNuevo()).isEqualTo("CERRADA");
		assertThat(cierre.getOrigen().getId()).isEqualTo(validacion.getId());
		assertThat(cierre.getUsuario().getId()).isEqualTo(responsable.getId());
	}

	@Test
	void rn05UnTecnicoNoPuedeValidarNiSiquieraElAsignado() {
		OrdenTrabajo orden = ordenResuelta(abrir(Prioridad.ALTA));

		assertThatExceptionOfType(PermisoDenegadoException.class)
				.isThrownBy(() -> ordenService.validar(tecnico.getId(), orden.getId()))
				.extracting(PermisoDenegadoException::getCodigo).isEqualTo("RN-05");
	}

	@Test
	void validarUnaOrdenQueNoExisteDevuelveNoEncontrado() {
		assertThatExceptionOfType(RecursoNoEncontradoException.class)
				.isThrownBy(() -> ordenService.validar(responsable.getId(), 999_999L))
				.extracting(RecursoNoEncontradoException::getCodigo).isEqualTo("NO_ENCONTRADO");
	}

	@Test
	void rn01SoloSePuedeValidarUnaOrdenResuelta() {
		OrdenTrabajo orden = ordenAsignada(abrir(Prioridad.ALTA));

		assertThatExceptionOfType(ReglaNegocioException.class)
				.isThrownBy(() -> ordenService.validar(responsable.getId(), orden.getId()))
				.extracting(ReglaNegocioException::getCodigo).isEqualTo("RN-01");
	}

	@Test
	void rn23UnaOrdenCerradaNoSeValidaDeNuevo() {
		OrdenTrabajo orden = ordenResuelta(abrir(Prioridad.ALTA));
		forzarEstado(orden, "CERRADA");

		assertThatExceptionOfType(ReglaNegocioException.class)
				.isThrownBy(() -> ordenService.validar(responsable.getId(), orden.getId()))
				.extracting(ReglaNegocioException::getCodigo).isEqualTo("RN-23");
	}

	@Test
	void rn11ValidarLaOrdenDeLaUnicaCriticaDevuelveElEquipoAOperativo() {
		Incidencia incidencia = abrir(Prioridad.CRITICA);
		OrdenTrabajo orden = ordenResuelta(incidencia);
		assertThat(equipo.getEstado()).isEqualTo(EstadoEquipo.AVERIADO);

		ordenService.validar(responsable.getId(), orden.getId());

		assertThat(equipo.getEstado()).isEqualTo(EstadoEquipo.OPERATIVO);
		Historial validacion = historialService.consultar(TipoEntidad.ORDEN, orden.getId()).getLast();
		Historial cambioEquipo = historialService.consultar(TipoEntidad.EQUIPO, equipo.getId()).getLast();
		assertThat(cambioEquipo.getValorAnterior()).isEqualTo("AVERIADO");
		assertThat(cambioEquipo.getValorNuevo()).isEqualTo("OPERATIVO");
		assertThat(cambioEquipo.getOrigen().getId()).isEqualTo(validacion.getId());
	}

	@Test
	void conOtraCriticaSinCerrarValidarUnaOrdenNoRecuperaElEquipo() {
		OrdenTrabajo orden = ordenResuelta(abrir(Prioridad.CRITICA));
		abrir(Prioridad.CRITICA);

		ordenService.validar(responsable.getId(), orden.getId());

		assertThat(equipo.getEstado()).isEqualTo(EstadoEquipo.AVERIADO);
	}

	@Test
	void validarLaOrdenDeUnaNoCriticaNoCambiaElEstadoDelEquipo() {
		OrdenTrabajo orden = ordenResuelta(abrir(Prioridad.ALTA));

		ordenService.validar(responsable.getId(), orden.getId());

		assertThat(equipo.getEstado()).isEqualTo(EstadoEquipo.OPERATIVO);
		assertThat(historialService.consultar(TipoEntidad.EQUIPO, equipo.getId())).hasSize(1);
	}

	@Test
	void elResponsableRechazaUnaResolucionYLaOrdenVuelveAEnCurso() {
		Incidencia incidencia = abrir(Prioridad.ALTA);
		OrdenTrabajo orden = ordenResuelta(incidencia);

		ordenService.rechazar(responsable.getId(), orden.getId(), "La fuga continúa.");

		assertThat(orden.getEstado()).isEqualTo(EstadoOrden.EN_CURSO);
		assertThat(orden.getFechaFin()).isNull();
		assertThat(incidencia.getEstado()).isEqualTo(EstadoIncidencia.EN_CURSO);
		Historial rechazo = historialService.consultar(TipoEntidad.ORDEN, orden.getId()).getLast();
		assertThat(rechazo.getAccion()).isEqualTo(AccionHistorial.CAMBIO_ESTADO);
		assertThat(rechazo.getValorAnterior()).isEqualTo("RESUELTA");
		assertThat(rechazo.getValorNuevo()).isEqualTo("EN_CURSO");
		assertThat(rechazo.getComentario()).isEqualTo("La fuga continúa.");
		assertThat(rechazo.getUsuario().getId()).isEqualTo(responsable.getId());
	}

	@Test
	void trasUnRechazoElHistorialConservaLasDosResoluciones() {
		OrdenTrabajo orden = ordenResuelta(abrir(Prioridad.ALTA));
		ordenService.rechazar(responsable.getId(), orden.getId(), "La fuga continúa.");

		ordenService.resolver(tecnico.getId(), orden.getId(), "Segundo sello colocado.");

		assertThat(orden.getResolucion()).isEqualTo("Segundo sello colocado.");
		List<String> comentarios = historialService.consultar(TipoEntidad.ORDEN, orden.getId()).stream()
				.map(Historial::getComentario).filter(c -> c != null).toList();
		assertThat(comentarios).containsExactly("Sello sustituido.", "La fuga continúa.",
				"Segundo sello colocado.");
	}

	@Test
	void rn05UnTecnicoNoPuedeRechazar() {
		OrdenTrabajo orden = ordenResuelta(abrir(Prioridad.ALTA));

		assertThatExceptionOfType(PermisoDenegadoException.class)
				.isThrownBy(() -> ordenService.rechazar(tecnico.getId(), orden.getId(), "No vale."))
				.extracting(PermisoDenegadoException::getCodigo).isEqualTo("RN-05");
	}

	@Test
	void rechazarUnaOrdenQueNoExisteDevuelveNoEncontrado() {
		assertThatExceptionOfType(RecursoNoEncontradoException.class)
				.isThrownBy(() -> ordenService.rechazar(responsable.getId(), 999_999L, "No vale."))
				.extracting(RecursoNoEncontradoException::getCodigo).isEqualTo("NO_ENCONTRADO");
	}

	@Test
	void rn01SoloSePuedeRechazarUnaOrdenResuelta() {
		OrdenTrabajo orden = ordenAsignada(abrir(Prioridad.ALTA));
		ordenService.empezar(tecnico.getId(), orden.getId());

		assertThatExceptionOfType(ReglaNegocioException.class)
				.isThrownBy(() -> ordenService.rechazar(responsable.getId(), orden.getId(), "No vale."))
				.extracting(ReglaNegocioException::getCodigo).isEqualTo("RN-01");
	}

	@Test
	void rn23UnaOrdenCanceladaNoSeRechaza() {
		OrdenTrabajo orden = ordenResuelta(abrir(Prioridad.ALTA));
		forzarEstado(orden, "CANCELADA");

		assertThatExceptionOfType(ReglaNegocioException.class)
				.isThrownBy(() -> ordenService.rechazar(responsable.getId(), orden.getId(), "No vale."))
				.extracting(ReglaNegocioException::getCodigo).isEqualTo("RN-23");
	}

}
