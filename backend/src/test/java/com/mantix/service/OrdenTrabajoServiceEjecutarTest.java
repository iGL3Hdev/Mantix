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
class OrdenTrabajoServiceEjecutarTest {

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
    private Usuario otroTecnico;
    private Usuario tecnicoInactivo;
    private Incidencia incidencia;

    @BeforeEach
	void prepararDatos() {
		responsable = usuarios.findAll().stream()
				.filter(u -> u.getPerfil() == Perfil.RESPONSABLE).findFirst().orElseThrow();
		tecnico = usuarios.findAll().stream()
				.filter(u -> u.getPerfil() == Perfil.TECNICO && u.isActivo()).findFirst().orElseThrow();
		otroTecnico = usuarios.findAll().stream()
				.filter(u -> u.getPerfil() == Perfil.TECNICO && u.isActivo() && !u.getId().equals(tecnico.getId()))
				.findFirst().orElseThrow();
		tecnicoInactivo = usuarios.findAll().stream()
				.filter(u -> u.getPerfil() == Perfil.TECNICO && !u.isActivo()).findFirst().orElseThrow();
		Equipo equipo = equipoService.crear(responsable.getId(), new DatosEquipo("BOM-001", "Bomba principal",
				null, null, null, ubicaciones.findAll().getFirst().getId(), null));
		incidencia = incidenciaService.crear(tecnico.getId(),
				new DatosIncidencia(equipo.getId(), "Fuga en el sello", "Gotea agua por el eje.", Prioridad.ALTA));
	}

	private OrdenTrabajo ordenAsignada() {
		return ordenService.crear(responsable.getId(),
				new DatosOrden(incidencia.getId(), tecnico.getId(), "Cambiar el sello mecánico."));
	}

	private OrdenTrabajo ordenEnCurso() {
		OrdenTrabajo orden = ordenAsignada();
		ordenService.empezar(tecnico.getId(), orden.getId());
		return orden;
	}

	// Validar y cancelar llegarán en los trozos siguientes: de momento se fuerza el estado en la base.
	private void forzarEstado(OrdenTrabajo orden, String estado) {
		em.flush();
		jdbc.update("UPDATE orden_trabajo SET estado = ? WHERE id = ?", estado, orden.getId());
		em.clear();
	}

	@Test
	void elTecnicoAsignadoEmpiezaLaOrdenYQuedaEnElHistorial() {
		OrdenTrabajo orden = ordenAsignada();
		Instant antes = Instant.now();

		ordenService.empezar(tecnico.getId(), orden.getId());

		assertThat(orden.getEstado()).isEqualTo(EstadoOrden.EN_CURSO);
		assertThat(orden.getFechaInicio()).isBetween(antes, Instant.now());
		List<Historial> filas = historialService.consultar(TipoEntidad.ORDEN, orden.getId());
		Historial inicio = filas.getLast();
		assertThat(inicio.getAccion()).isEqualTo(AccionHistorial.CAMBIO_ESTADO);
		assertThat(inicio.getValorAnterior()).isEqualTo("ASIGNADA");
		assertThat(inicio.getValorNuevo()).isEqualTo("EN_CURSO");
		assertThat(inicio.getUsuario().getId()).isEqualTo(tecnico.getId());
	}

	@Test
	void rn06OtroTecnicoNoPuedeEmpezarLaOrden() {
		OrdenTrabajo orden = ordenAsignada();

		assertThatExceptionOfType(PermisoDenegadoException.class)
				.isThrownBy(() -> ordenService.empezar(otroTecnico.getId(), orden.getId()))
				.extracting(PermisoDenegadoException::getCodigo).isEqualTo("RN-06");
	}

	@Test
	void rn06ElResponsableTampocoPuedeEmpezarLaOrden() {
		OrdenTrabajo orden = ordenAsignada();

		assertThatExceptionOfType(PermisoDenegadoException.class)
				.isThrownBy(() -> ordenService.empezar(responsable.getId(), orden.getId()))
				.extracting(PermisoDenegadoException::getCodigo).isEqualTo("RN-06");
	}

	@Test
	void rn04UnUsuarioInactivoNoPuedeEmpezar() {
		OrdenTrabajo orden = ordenAsignada();

		assertThatExceptionOfType(PermisoDenegadoException.class)
				.isThrownBy(() -> ordenService.empezar(tecnicoInactivo.getId(), orden.getId()))
				.extracting(PermisoDenegadoException::getCodigo).isEqualTo("RN-04");
	}

	@Test
	void empezarUnaOrdenQueNoExisteDevuelveNoEncontrado() {
		assertThatExceptionOfType(RecursoNoEncontradoException.class)
				.isThrownBy(() -> ordenService.empezar(tecnico.getId(), 999_999L))
				.extracting(RecursoNoEncontradoException::getCodigo).isEqualTo("NO_ENCONTRADO");
	}

	@Test
	void rn01SoloSePuedeEmpezarUnaOrdenAsignada() {
		OrdenTrabajo orden = ordenEnCurso();

		assertThatExceptionOfType(ReglaNegocioException.class)
				.isThrownBy(() -> ordenService.empezar(tecnico.getId(), orden.getId()))
				.extracting(ReglaNegocioException::getCodigo).isEqualTo("RN-01");
	}

	@Test
	void rn23UnaOrdenCerradaNoSeEmpieza() {
		OrdenTrabajo orden = ordenAsignada();
		forzarEstado(orden, "CERRADA");

		assertThatExceptionOfType(ReglaNegocioException.class)
				.isThrownBy(() -> ordenService.empezar(tecnico.getId(), orden.getId()))
				.extracting(ReglaNegocioException::getCodigo).isEqualTo("RN-23");
	}

	@Test
	void elTecnicoAsignadoResuelveLaOrdenYLaResolucionQuedaEnElHistorial() {
		OrdenTrabajo orden = ordenEnCurso();
		Instant antes = Instant.now();

		ordenService.resolver(tecnico.getId(), orden.getId(), "Sello sustituido y prueba de presión correcta.");

		assertThat(orden.getEstado()).isEqualTo(EstadoOrden.RESUELTA);
		assertThat(orden.getResolucion()).isEqualTo("Sello sustituido y prueba de presión correcta.");
		assertThat(orden.getFechaFin()).isBetween(antes, Instant.now());
		assertThat(orden.getFechaInicio()).isNotNull();
		Historial resolucion = historialService.consultar(TipoEntidad.ORDEN, orden.getId()).getLast();
		assertThat(resolucion.getAccion()).isEqualTo(AccionHistorial.CAMBIO_ESTADO);
		assertThat(resolucion.getValorAnterior()).isEqualTo("EN_CURSO");
		assertThat(resolucion.getValorNuevo()).isEqualTo("RESUELTA");
		assertThat(resolucion.getComentario()).isEqualTo("Sello sustituido y prueba de presión correcta.");
		assertThat(resolucion.getUsuario().getId()).isEqualTo(tecnico.getId());
	}

	@Test
	void resolverUnaOrdenNoCierraLaIncidencia() {
		OrdenTrabajo orden = ordenEnCurso();

		ordenService.resolver(tecnico.getId(), orden.getId(), "Sello sustituido.");

		assertThat(incidencia.getEstado()).isEqualTo(EstadoIncidencia.EN_CURSO);
	}

	@Test
	void rn06OtroTecnicoNoPuedeResolverLaOrden() {
		OrdenTrabajo orden = ordenEnCurso();

		assertThatExceptionOfType(PermisoDenegadoException.class)
				.isThrownBy(() -> ordenService.resolver(otroTecnico.getId(), orden.getId(), "Hecho."))
				.extracting(PermisoDenegadoException::getCodigo).isEqualTo("RN-06");
	}

	@Test
	void rn01SoloSePuedeResolverUnaOrdenEnCurso() {
		OrdenTrabajo orden = ordenAsignada();

		assertThatExceptionOfType(ReglaNegocioException.class)
				.isThrownBy(() -> ordenService.resolver(tecnico.getId(), orden.getId(), "Hecho."))
				.extracting(ReglaNegocioException::getCodigo).isEqualTo("RN-01");
	}

	@Test
	void rn23UnaOrdenCanceladaNoSeResuelve() {
		OrdenTrabajo orden = ordenEnCurso();
		forzarEstado(orden, "CANCELADA");

		assertThatExceptionOfType(ReglaNegocioException.class)
				.isThrownBy(() -> ordenService.resolver(tecnico.getId(), orden.getId(), "Hecho."))
				.extracting(ReglaNegocioException::getCodigo).isEqualTo("RN-23");
	}
    
}
