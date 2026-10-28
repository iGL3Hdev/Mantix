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
import org.springframework.transaction.annotation.Transactional;

import com.mantix.TestcontainersConfiguration;
import com.mantix.domain.AccionHistorial;
import com.mantix.domain.Equipo;
import com.mantix.domain.EstadoIncidencia;
import com.mantix.domain.EstadoOrden;
import com.mantix.domain.Historial;
import com.mantix.domain.Incidencia;
import com.mantix.domain.OrdenTrabajo;
import com.mantix.domain.Perfil;
import com.mantix.domain.Prioridad;
import com.mantix.domain.TipoEntidad;
import com.mantix.domain.Usuario;
import com.mantix.repository.OrdenTrabajoRepository;
import com.mantix.repository.UbicacionRepository;
import com.mantix.repository.UsuarioRepository;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@Transactional
class OrdenTrabajoServiceCrearTest {

	@Autowired
	private OrdenTrabajoService ordenService;

	@Autowired
	private IncidenciaService incidenciaService;

	@Autowired
	private EquipoService equipoService;

	@Autowired
	private HistorialService historialService;

	@Autowired
	private OrdenTrabajoRepository ordenes;

	@Autowired
	private UbicacionRepository ubicaciones;

	@Autowired
	private UsuarioRepository usuarios;

	private Usuario responsable;
	private Usuario tecnico;
	private Usuario tecnicoInactivo;
	private Incidencia incidencia;

	@BeforeEach
	void prepararDatos() {
		responsable = usuarios.findAll().stream()
				.filter(u -> u.getPerfil() == Perfil.RESPONSABLE).findFirst().orElseThrow();
		tecnico = usuarios.findAll().stream()
				.filter(u -> u.getPerfil() == Perfil.TECNICO && u.isActivo()).findFirst().orElseThrow();
		tecnicoInactivo = usuarios.findAll().stream()
				.filter(u -> u.getPerfil() == Perfil.TECNICO && !u.isActivo()).findFirst().orElseThrow();
		Equipo equipo = equipoService.crear(responsable.getId(), new DatosEquipo("BOM-001", "Bomba principal",
				null, null, null, ubicaciones.findAll().getFirst().getId(), null));
		incidencia = incidenciaService.crear(tecnico.getId(),
				new DatosIncidencia(equipo.getId(), "Fuga en el sello", "Gotea agua por el eje.", Prioridad.ALTA));
	}

	private DatosOrden datos(Long tecnicoId) {
		return new DatosOrden(incidencia.getId(), tecnicoId, "Cambiar el sello mecánico.");
	}

	@Test
	void sinTecnicoLaOrdenQuedaPendienteYLaIncidenciaEnCurso() {
		Instant antes = Instant.now();

		OrdenTrabajo orden = ordenService.crear(responsable.getId(), datos(null));

		assertThat(orden.getId()).isNotNull();
		assertThat(orden.getEstado()).isEqualTo(EstadoOrden.PENDIENTE);
		assertThat(orden.getTecnico()).isNull();
		assertThat(orden.getCreadaPor().getId()).isEqualTo(responsable.getId());
		assertThat(orden.getFechaCreacion()).isBetween(antes, Instant.now());
		assertThat(incidencia.getEstado()).isEqualTo(EstadoIncidencia.EN_CURSO);
		List<Historial> filas = historialService.consultar(TipoEntidad.ORDEN, orden.getId());
		assertThat(filas).hasSize(1);
		assertThat(filas.getFirst().getAccion()).isEqualTo(AccionHistorial.ALTA);
		assertThat(filas.getFirst().getValorNuevo()).isEqualTo("PENDIENTE");
	}

	@Test
	void conTecnicoLaOrdenQuedaAsignadaYRegistraElCambioDeTecnico() {
		OrdenTrabajo orden = ordenService.crear(responsable.getId(), datos(tecnico.getId()));

		assertThat(orden.getEstado()).isEqualTo(EstadoOrden.ASIGNADA);
		assertThat(orden.getTecnico().getId()).isEqualTo(tecnico.getId());
		assertThat(incidencia.getEstado()).isEqualTo(EstadoIncidencia.EN_CURSO);
		List<Historial> filas = historialService.consultar(TipoEntidad.ORDEN, orden.getId());
		assertThat(filas).hasSize(2);
		Historial alta = filas.getFirst();
		Historial asignacion = filas.getLast();
		assertThat(alta.getValorNuevo()).isEqualTo("ASIGNADA");
		assertThat(asignacion.getAccion()).isEqualTo(AccionHistorial.CAMBIO_TECNICO);
		assertThat(asignacion.getValorAnterior()).isNull();
		assertThat(asignacion.getValorNuevo()).isEqualTo(String.valueOf(tecnico.getId()));
		assertThat(asignacion.getOrigen().getId()).isEqualTo(alta.getId());
	}

	@Test
	void elCambioDeEstadoDeLaIncidenciaQuedaEnlazadoConElAltaDeLaOrden() {
		OrdenTrabajo orden = ordenService.crear(responsable.getId(), datos(null));

		Historial altaOrden = historialService.consultar(TipoEntidad.ORDEN, orden.getId()).getFirst();
		Historial cambioIncidencia = historialService.consultar(TipoEntidad.INCIDENCIA, incidencia.getId())
				.getLast();
		assertThat(cambioIncidencia.getAccion()).isEqualTo(AccionHistorial.CAMBIO_ESTADO);
		assertThat(cambioIncidencia.getValorAnterior()).isEqualTo("ABIERTA");
		assertThat(cambioIncidencia.getValorNuevo()).isEqualTo("EN_CURSO");
		assertThat(cambioIncidencia.getOrigen().getId()).isEqualTo(altaOrden.getId());
		assertThat(cambioIncidencia.getUsuario().getId()).isEqualTo(responsable.getId());
	}

	@Test
	void rn05UnTecnicoNoPuedeCrearOrdenes() {
		assertThatExceptionOfType(PermisoDenegadoException.class)
				.isThrownBy(() -> ordenService.crear(tecnico.getId(), datos(null)))
				.extracting(PermisoDenegadoException::getCodigo).isEqualTo("RN-05");
	}

	@Test
	void unaIncidenciaQueNoExisteDevuelveNoEncontrado() {
		DatosOrden sinIncidencia = new DatosOrden(999_999L, null, "Cambiar el sello.");

		assertThatExceptionOfType(RecursoNoEncontradoException.class)
				.isThrownBy(() -> ordenService.crear(responsable.getId(), sinIncidencia))
				.extracting(RecursoNoEncontradoException::getCodigo).isEqualTo("NO_ENCONTRADO");
	}

	@Test
	void rn19SoloSeCreaOrdenParaUnaIncidenciaAbierta() {
		incidenciaService.descartar(responsable.getId(), incidencia.getId(), "Falsa alarma");

		assertThatExceptionOfType(ReglaNegocioException.class)
				.isThrownBy(() -> ordenService.crear(responsable.getId(), datos(null)))
				.extracting(ReglaNegocioException::getCodigo).isEqualTo("RN-19");
	}

	@Test
	void rn19UnaSegundaOrdenSobreLaMismaIncidenciaSeRechaza() {
		ordenService.crear(responsable.getId(), datos(null));

		assertThatExceptionOfType(ReglaNegocioException.class)
				.isThrownBy(() -> ordenService.crear(responsable.getId(), datos(null)))
				.extracting(ReglaNegocioException::getCodigo).isEqualTo("RN-19");
	}

	@Test
	void rn20UnaIncidenciaAbiertaConUnaOrdenSinCerrarSeRechaza() {
		// Situación que el servicio no genera por sí solo: se fuerza para probar RN-20 aparte de RN-19.
		ordenes.save(new OrdenTrabajo(incidencia, responsable, null, "Orden previa.", Instant.now()));

		assertThatExceptionOfType(ReglaNegocioException.class)
				.isThrownBy(() -> ordenService.crear(responsable.getId(), datos(null)))
				.extracting(ReglaNegocioException::getCodigo).isEqualTo("RN-20");
	}

	@Test
	void rn07NoSeAsignaUnaOrdenAUnResponsable() {
		assertThatExceptionOfType(PermisoDenegadoException.class)
				.isThrownBy(() -> ordenService.crear(responsable.getId(), datos(responsable.getId())))
				.extracting(PermisoDenegadoException::getCodigo).isEqualTo("RN-07");
	}

	@Test
	void rn04NoSeAsignaUnaOrdenAUnTecnicoInactivo() {
		assertThatExceptionOfType(PermisoDenegadoException.class)
				.isThrownBy(() -> ordenService.crear(responsable.getId(), datos(tecnicoInactivo.getId())))
				.extracting(PermisoDenegadoException::getCodigo).isEqualTo("RN-04");
	}

}
