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
class OrdenTrabajoServiceAsignarTest {

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
            new DatosIncidencia(equipo.getId(), "Fuga en el sello", "Gotea agua por el eje", Prioridad.ALTA));
    }

    private OrdenTrabajo ordenPendiente() {
        return ordenService.crear(responsable.getId(),
            new DatosOrden(incidencia.getId(), null, "Cambiar el sello mecánico"));
    }

    private OrdenTrabajo ordenAsignada() {
        return ordenService.crear(responsable.getId(),
            new DatosOrden(incidencia.getId(), tecnico.getId(), "Cambiar el sello mecánico"));
    }

    // Empezar, resolver o cancelar llegarán en los trozos siguientes: de momento se fuerza el estado en la base.
    private void forzarEstado(OrdenTrabajo orden, String estado) {
        em.flush();
        jdbc.update("UPDATE orden_trabajo SET estado = ? WHERE id = ?", estado, orden.getId());
        em.clear();
    }

    @Test
    void elResponsableAsignaUnaOrdenPendienteYQuedaEnElHistorial() {
        OrdenTrabajo orden = ordenPendiente();

        ordenService.asignar(responsable.getId(), orden.getId(), tecnico.getId());

        assertThat(orden.getEstado()).isEqualTo(EstadoOrden.ASIGNADA);
		assertThat(orden.getTecnico().getId()).isEqualTo(tecnico.getId());
		List<Historial> filas = historialService.consultar(TipoEntidad.ORDEN, orden.getId());
		assertThat(filas).hasSize(3);
		Historial cambioEstado = filas.get(1);
		Historial cambioTecnico = filas.get(2);
		assertThat(cambioEstado.getAccion()).isEqualTo(AccionHistorial.CAMBIO_ESTADO);
		assertThat(cambioEstado.getValorAnterior()).isEqualTo("PENDIENTE");
		assertThat(cambioEstado.getValorNuevo()).isEqualTo("ASIGNADA");
		assertThat(cambioEstado.getUsuario().getId()).isEqualTo(responsable.getId());
		assertThat(cambioTecnico.getAccion()).isEqualTo(AccionHistorial.CAMBIO_TECNICO);
		assertThat(cambioTecnico.getValorAnterior()).isNull();
		assertThat(cambioTecnico.getValorNuevo()).isEqualTo(String.valueOf(tecnico.getId()));
		assertThat(cambioTecnico.getOrigen().getId()).isEqualTo(cambioEstado.getId());
    }

    @Test
	void rn05UnTecnicoNoPuedeAsignar() {
		OrdenTrabajo orden = ordenPendiente();

		assertThatExceptionOfType(PermisoDenegadoException.class)
				.isThrownBy(() -> ordenService.asignar(tecnico.getId(), orden.getId(), tecnico.getId()))
				.extracting(PermisoDenegadoException::getCodigo).isEqualTo("RN-05");
	}

	@Test
	void unaOrdenQueNoExisteDevuelveNoEncontrado() {
		assertThatExceptionOfType(RecursoNoEncontradoException.class)
				.isThrownBy(() -> ordenService.asignar(responsable.getId(), 999_999L, tecnico.getId()))
				.extracting(RecursoNoEncontradoException::getCodigo).isEqualTo("NO_ENCONTRADO");
	}

	@Test
	void rn07NoSeAsignaUnaOrdenAUnResponsable() {
		OrdenTrabajo orden = ordenPendiente();

		assertThatExceptionOfType(PermisoDenegadoException.class)
				.isThrownBy(() -> ordenService.asignar(responsable.getId(), orden.getId(), responsable.getId()))
				.extracting(PermisoDenegadoException::getCodigo).isEqualTo("RN-07");
	}

	@Test
	void rn04NoSeAsignaUnaOrdenAUnTecnicoInactivo() {
		OrdenTrabajo orden = ordenPendiente();

		assertThatExceptionOfType(PermisoDenegadoException.class)
				.isThrownBy(() -> ordenService.asignar(responsable.getId(), orden.getId(), tecnicoInactivo.getId()))
				.extracting(PermisoDenegadoException::getCodigo).isEqualTo("RN-04");
	}

	@Test
	void rn01SoloSeAsignaUnaOrdenPendiente() {
		OrdenTrabajo orden = ordenAsignada();

		assertThatExceptionOfType(ReglaNegocioException.class)
				.isThrownBy(() -> ordenService.asignar(responsable.getId(), orden.getId(), otroTecnico.getId()))
				.extracting(ReglaNegocioException::getCodigo).isEqualTo("RN-01");
	}

	@Test
	void rn23UnaOrdenCanceladaNoCambiaMas() {
		OrdenTrabajo orden = ordenPendiente();
		forzarEstado(orden, "CANCELADA");

		assertThatExceptionOfType(ReglaNegocioException.class)
				.isThrownBy(() -> ordenService.asignar(responsable.getId(), orden.getId(), tecnico.getId()))
				.extracting(ReglaNegocioException::getCodigo).isEqualTo("RN-23");
	}

	@Test
	void elResponsableReasignaUnaOrdenAsignadaYQuedaEnElHistorial() {
		OrdenTrabajo orden = ordenAsignada();

		ordenService.reasignar(responsable.getId(), orden.getId(), otroTecnico.getId());

		assertThat(orden.getEstado()).isEqualTo(EstadoOrden.ASIGNADA);
		assertThat(orden.getTecnico().getId()).isEqualTo(otroTecnico.getId());
		List<Historial> filas = historialService.consultar(TipoEntidad.ORDEN, orden.getId());
		assertThat(filas).hasSize(3);
		Historial reasignacion = filas.getLast();
		assertThat(reasignacion.getAccion()).isEqualTo(AccionHistorial.CAMBIO_TECNICO);
		assertThat(reasignacion.getValorAnterior()).isEqualTo(String.valueOf(tecnico.getId()));
		assertThat(reasignacion.getValorNuevo()).isEqualTo(String.valueOf(otroTecnico.getId()));
		assertThat(reasignacion.getOrigen()).isNull();
		assertThat(reasignacion.getUsuario().getId()).isEqualTo(responsable.getId());
	}

	@Test
	void rn05UnTecnicoNoPuedeReasignar() {
		OrdenTrabajo orden = ordenAsignada();

		assertThatExceptionOfType(PermisoDenegadoException.class)
				.isThrownBy(() -> ordenService.reasignar(tecnico.getId(), orden.getId(), otroTecnico.getId()))
				.extracting(PermisoDenegadoException::getCodigo).isEqualTo("RN-05");
	}

	@Test
	void rn24SoloSeReasignaMientrasLaOrdenEstaAsignada() {
		OrdenTrabajo orden = ordenAsignada();
		forzarEstado(orden, "EN_CURSO");

		assertThatExceptionOfType(ReglaNegocioException.class)
				.isThrownBy(() -> ordenService.reasignar(responsable.getId(), orden.getId(), otroTecnico.getId()))
				.extracting(ReglaNegocioException::getCodigo).isEqualTo("RN-24");
	}

	@Test
	void rn24ElNuevoTecnicoTieneQueSerDistintoDelActual() {
		OrdenTrabajo orden = ordenAsignada();

		assertThatExceptionOfType(ReglaNegocioException.class)
				.isThrownBy(() -> ordenService.reasignar(responsable.getId(), orden.getId(), tecnico.getId()))
				.extracting(ReglaNegocioException::getCodigo).isEqualTo("RN-24");
	}

	@Test
	void rn24NoSeReasignaUnaOrdenPendienteQueAunNoTieneTecnico() {
		OrdenTrabajo orden = ordenPendiente();

		assertThatExceptionOfType(ReglaNegocioException.class)
				.isThrownBy(() -> ordenService.reasignar(responsable.getId(), orden.getId(), tecnico.getId()))
				.extracting(ReglaNegocioException::getCodigo).isEqualTo("RN-24");
	}

	@Test
	void rn04NoSeReasignaAUnTecnicoInactivo() {
		OrdenTrabajo orden = ordenAsignada();

		assertThatExceptionOfType(PermisoDenegadoException.class)
				.isThrownBy(() -> ordenService.reasignar(responsable.getId(), orden.getId(), tecnicoInactivo.getId()))
				.extracting(PermisoDenegadoException::getCodigo).isEqualTo("RN-04");
	}

	@Test
	void rn23UnaOrdenCerradaNoSeReasigna() {
		OrdenTrabajo orden = ordenAsignada();
		forzarEstado(orden, "CERRADA");

		assertThatExceptionOfType(ReglaNegocioException.class)
				.isThrownBy(() -> ordenService.reasignar(responsable.getId(), orden.getId(), otroTecnico.getId()))
				.extracting(ReglaNegocioException::getCodigo).isEqualTo("RN-23");
	}

}
