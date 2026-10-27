package com.mantix.service;

import java.time.Instant;
import java.util.List;

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
import com.mantix.domain.Historial;
import com.mantix.domain.Incidencia;
import com.mantix.domain.Perfil;
import com.mantix.domain.Prioridad;
import com.mantix.domain.TipoEntidad;
import com.mantix.domain.Usuario;
import com.mantix.repository.UbicacionRepository;
import com.mantix.repository.UsuarioRepository;

@Import(TestcontainersConfiguration.class)
@SpringBootTest 
@Transactional 
public class IncidenciaServiceCrearTest {

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
    private Usuario tecnicoInactivo;
    private Equipo equipo;

    @BeforeEach 
    void prepararDatos() {
        responsable = usuarios.findAll().stream()
            .filter(u -> u.getPerfil() == Perfil.RESPONSABLE).findFirst().orElseThrow();
        tecnico = usuarios.findAll().stream()   
            .filter(u -> u.getPerfil() == Perfil.TECNICO && u.isActivo()).findFirst().orElseThrow();
        tecnicoInactivo = usuarios.findAll().stream()
            .filter(u -> u.getPerfil() == Perfil.TECNICO && !u.isActivo()).findFirst().orElseThrow();
        equipo = equipoService.crear(responsable.getId(), new DatosEquipo("BOM-001", "Bomba principal", null,
				null, null, ubicaciones.findAll().getFirst().getId(), null));
    }

    private DatosIncidencia datos(Prioridad prioridad) {
        return new DatosIncidencia(equipo.getId(), "Fuga en el sello", "Gota de agua por el eje.", prioridad);
    }

    @Test
	void cualquierUsuarioActivoAbreUnaIncidenciaAbierta() {
		Instant antes = Instant.now();

		Incidencia incidencia = incidenciaService.crear(tecnico.getId(), datos(Prioridad.ALTA));

		assertThat(incidencia.getId()).isNotNull();
		assertThat(incidencia.getEstado()).isEqualTo(EstadoIncidencia.ABIERTA);
		assertThat(incidencia.getReportadaPor().getId()).isEqualTo(tecnico.getId());
		assertThat(incidencia.getPrioridad()).isEqualTo(Prioridad.ALTA);
		assertThat(incidencia.getFechaApertura()).isBetween(antes, Instant.now());
	}

    @Test
	void laAperturaDejaUnaFilaAltaEnElHistorial() {
		Incidencia incidencia = incidenciaService.crear(tecnico.getId(), datos(Prioridad.ALTA));

		List<Historial> filas = historialService.consultar(TipoEntidad.INCIDENCIA, incidencia.getId());

		assertThat(filas).hasSize(1);
		assertThat(filas.getFirst().getAccion()).isEqualTo(AccionHistorial.ALTA);
		assertThat(filas.getFirst().getValorAnterior()).isNull();
		assertThat(filas.getFirst().getValorNuevo()).isEqualTo("ABIERTA");
		assertThat(filas.getFirst().getUsuario().getId()).isEqualTo(tecnico.getId());
	}

	@Test
	void rn04UnUsuarioInactivoNoPuedeAbrirIncidencias() {
		assertThatExceptionOfType(PermisoDenegadoException.class)
				.isThrownBy(() -> incidenciaService.crear(tecnicoInactivo.getId(), datos(Prioridad.ALTA)))
				.extracting(PermisoDenegadoException::getCodigo).isEqualTo("RN-04");
	}

	@Test
	void rn14NoSePuedeAbrirUnaIncidenciaSobreUnEquipoDeBaja() {
		equipoService.darDeBaja(responsable.getId(), equipo.getId(), "Retirada de la planta");

		assertThatExceptionOfType(ReglaNegocioException.class)
				.isThrownBy(() -> incidenciaService.crear(tecnico.getId(), datos(Prioridad.ALTA)))
				.extracting(ReglaNegocioException::getCodigo).isEqualTo("RN-14");
	}

	@Test
	void unEquipoQueNoExisteDevuelveNoEncontrado() {
		DatosIncidencia sinEquipo = new DatosIncidencia(999_999L, "Fuga", "Gotea.", Prioridad.ALTA);

		assertThatExceptionOfType(RecursoNoEncontradoException.class)
				.isThrownBy(() -> incidenciaService.crear(tecnico.getId(), sinEquipo))
				.extracting(RecursoNoEncontradoException::getCodigo).isEqualTo("NO_ENCONTRADO");
	}

	@Test
	void rn10UnaIncidenciaCriticaDejaElEquipoAveriadoYLoRegistra() {
		Incidencia incidencia = incidenciaService.crear(tecnico.getId(), datos(Prioridad.CRITICA));

		assertThat(equipo.getEstado()).isEqualTo(EstadoEquipo.AVERIADO);
		Historial altaIncidencia = historialService.consultar(TipoEntidad.INCIDENCIA, incidencia.getId()).getFirst();
		Historial cambioEquipo = historialService.consultar(TipoEntidad.EQUIPO, equipo.getId()).getLast();
		assertThat(cambioEquipo.getAccion()).isEqualTo(AccionHistorial.CAMBIO_ESTADO);
		assertThat(cambioEquipo.getValorAnterior()).isEqualTo("OPERATIVO");
		assertThat(cambioEquipo.getValorNuevo()).isEqualTo("AVERIADO");
		assertThat(cambioEquipo.getOrigen().getId()).isEqualTo(altaIncidencia.getId());
	}

	@Test
	void unaIncidenciaNoCriticaNoCambiaElEstadoDelEquipo() {
		incidenciaService.crear(tecnico.getId(), datos(Prioridad.ALTA));

		assertThat(equipo.getEstado()).isEqualTo(EstadoEquipo.OPERATIVO);
		assertThat(historialService.consultar(TipoEntidad.EQUIPO, equipo.getId())).hasSize(1);
	}

	@Test
	void unaSegundaCriticaNoVuelveAMarcarElEquipoComoAveriado() {
		incidenciaService.crear(tecnico.getId(), datos(Prioridad.CRITICA));

		incidenciaService.crear(tecnico.getId(), datos(Prioridad.CRITICA));

		assertThat(equipo.getEstado()).isEqualTo(EstadoEquipo.AVERIADO);
		assertThat(historialService.consultar(TipoEntidad.EQUIPO, equipo.getId())).hasSize(2);
	}
    
}
