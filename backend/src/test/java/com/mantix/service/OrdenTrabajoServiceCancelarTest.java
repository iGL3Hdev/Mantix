package com.mantix.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
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

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@Transactional
class OrdenTrabajoServiceCancelarTest {

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

	private OrdenTrabajo ordenPendiente(Incidencia incidencia) {
		return ordenService.crear(responsable.getId(),
				new DatosOrden(incidencia.getId(), null, "Cambiar el sello mecánico."));
	}

	private OrdenTrabajo ordenAsignada(Incidencia incidencia) {
		return ordenService.crear(responsable.getId(),
				new DatosOrden(incidencia.getId(), tecnico.getId(), "Cambiar el sello mecánico."));
	}

	private OrdenTrabajo ordenEnCurso(Incidencia incidencia) {
		OrdenTrabajo orden = ordenAsignada(incidencia);
		ordenService.empezar(tecnico.getId(), orden.getId());
		return orden;
	}

	private OrdenTrabajo ordenResuelta(Incidencia incidencia) {
		OrdenTrabajo orden = ordenEnCurso(incidencia);
		ordenService.resolver(tecnico.getId(), orden.getId(), "Sello sustituido.");
		return orden;
	}

	@Test
	void elResponsableCancelaUnaOrdenPendienteYQuedaEnElHistorial() {
		OrdenTrabajo orden = ordenPendiente(abrir(Prioridad.ALTA));

		ordenService.cancelar(responsable.getId(), orden.getId(), "Ya no hace falta");

		assertThat(orden.getEstado()).isEqualTo(EstadoOrden.CANCELADA);
		Historial cancelacion = historialService.consultar(TipoEntidad.ORDEN, orden.getId()).getLast();
		assertThat(cancelacion.getAccion()).isEqualTo(AccionHistorial.CAMBIO_ESTADO);
		assertThat(cancelacion.getValorAnterior()).isEqualTo("PENDIENTE");
		assertThat(cancelacion.getValorNuevo()).isEqualTo("CANCELADA");
		assertThat(cancelacion.getComentario()).isEqualTo("Ya no hace falta");
		assertThat(cancelacion.getUsuario().getId()).isEqualTo(responsable.getId());
		assertThat(cancelacion.getOrigen()).isNull();
	}

	@Test
	void tambienSePuedeCancelarUnaOrdenAsignada() {
		OrdenTrabajo orden = ordenAsignada(abrir(Prioridad.ALTA));

		ordenService.cancelar(responsable.getId(), orden.getId(), "Ya no hace falta");

		assertThat(orden.getEstado()).isEqualTo(EstadoOrden.CANCELADA);
	}

	@Test
	void tambienSePuedeCancelarUnaOrdenEnCurso() {
		OrdenTrabajo orden = ordenEnCurso(abrir(Prioridad.ALTA));

		ordenService.cancelar(responsable.getId(), orden.getId(), "Ya no hace falta");

		assertThat(orden.getEstado()).isEqualTo(EstadoOrden.CANCELADA);
	}

	@Test
	void cancelarLaOrdenDevuelveLaIncidenciaAAbiertaYLaEnlazaConLaCancelacion() {
		Incidencia incidencia = abrir(Prioridad.ALTA);
		OrdenTrabajo orden = ordenAsignada(incidencia);

		ordenService.cancelar(responsable.getId(), orden.getId(), "Ya no hace falta");

		assertThat(incidencia.getEstado()).isEqualTo(EstadoIncidencia.ABIERTA);
		assertThat(incidencia.getFechaCierre()).isNull();
		Historial cancelacion = historialService.consultar(TipoEntidad.ORDEN, orden.getId()).getLast();
		Historial vuelta = historialService.consultar(TipoEntidad.INCIDENCIA, incidencia.getId()).getLast();
		assertThat(vuelta.getAccion()).isEqualTo(AccionHistorial.CAMBIO_ESTADO);
		assertThat(vuelta.getValorAnterior()).isEqualTo("EN_CURSO");
		assertThat(vuelta.getValorNuevo()).isEqualTo("ABIERTA");
		assertThat(vuelta.getOrigen().getId()).isEqualTo(cancelacion.getId());
		assertThat(vuelta.getUsuario().getId()).isEqualTo(responsable.getId());
	}

	@Test
	void trasCancelarSePuedeCrearUnaOrdenNuevaParaLaMismaIncidencia() {
		Incidencia incidencia = abrir(Prioridad.ALTA);
		OrdenTrabajo primera = ordenAsignada(incidencia);
		ordenService.cancelar(responsable.getId(), primera.getId(), "Ya no hace falta");

		OrdenTrabajo segunda = ordenPendiente(incidencia);

		assertThat(segunda.getId()).isNotEqualTo(primera.getId());
		assertThat(segunda.getEstado()).isEqualTo(EstadoOrden.PENDIENTE);
		assertThat(incidencia.getEstado()).isEqualTo(EstadoIncidencia.EN_CURSO);
	}

	@Test
	void cancelarLaOrdenDeUnaCriticaNoRecuperaAlEquipoPorqueLaIncidenciaSigueSinCerrar() {
		OrdenTrabajo orden = ordenAsignada(abrir(Prioridad.CRITICA));

		ordenService.cancelar(responsable.getId(), orden.getId(), "Ya no hace falta");

		assertThat(equipo.getEstado()).isEqualTo(EstadoEquipo.AVERIADO);
	}

	@Test
	void rn05UnTecnicoNoPuedeCancelar() {
		OrdenTrabajo orden = ordenAsignada(abrir(Prioridad.ALTA));

		assertThatExceptionOfType(PermisoDenegadoException.class)
				.isThrownBy(() -> ordenService.cancelar(tecnico.getId(), orden.getId(), "No quiero hacerla"))
				.extracting(PermisoDenegadoException::getCodigo).isEqualTo("RN-05");
	}

	@Test
	void cancelarUnaOrdenQueNoExisteDevuelveNoEncontrado() {
		assertThatExceptionOfType(RecursoNoEncontradoException.class)
				.isThrownBy(() -> ordenService.cancelar(responsable.getId(), 999_999L, "Ya no hace falta"))
				.extracting(RecursoNoEncontradoException::getCodigo).isEqualTo("NO_ENCONTRADO");
	}

	@Test
	void rn01NoSePuedeCancelarUnaOrdenResuelta() {
		OrdenTrabajo orden = ordenResuelta(abrir(Prioridad.ALTA));

		assertThatExceptionOfType(ReglaNegocioException.class)
				.isThrownBy(() -> ordenService.cancelar(responsable.getId(), orden.getId(), "Ya no hace falta"))
				.extracting(ReglaNegocioException::getCodigo).isEqualTo("RN-01");
	}

	@Test
	void rn23UnaOrdenCanceladaNoSeCancelaDeNuevo() {
		OrdenTrabajo orden = ordenAsignada(abrir(Prioridad.ALTA));
		ordenService.cancelar(responsable.getId(), orden.getId(), "Ya no hace falta");

		assertThatExceptionOfType(ReglaNegocioException.class)
				.isThrownBy(() -> ordenService.cancelar(responsable.getId(), orden.getId(), "Otra vez"))
				.extracting(ReglaNegocioException::getCodigo).isEqualTo("RN-23");
	}

	@Test
	void rn23UnaOrdenCerradaNoSeCancela() {
		OrdenTrabajo orden = ordenResuelta(abrir(Prioridad.ALTA));
		ordenService.validar(responsable.getId(), orden.getId());

		assertThatExceptionOfType(ReglaNegocioException.class)
				.isThrownBy(() -> ordenService.cancelar(responsable.getId(), orden.getId(), "Ya no hace falta"))
				.extracting(ReglaNegocioException::getCodigo).isEqualTo("RN-23");
	}

}
