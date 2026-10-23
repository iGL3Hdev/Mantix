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
import com.mantix.domain.Historial;
import com.mantix.domain.Incidencia;
import com.mantix.domain.Perfil;
import com.mantix.domain.Prioridad;
import com.mantix.domain.TipoEntidad;
import com.mantix.domain.Ubicacion;
import com.mantix.domain.Usuario;
import com.mantix.repository.IncidenciaRepository;
import com.mantix.repository.UbicacionRepository;
import com.mantix.repository.UsuarioRepository;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@Transactional
class EquipoCicloDeVidaServiceTest {

	@Autowired
	private EquipoService equipoService;

	@Autowired
	private HistorialService historialService;

	@Autowired
	private IncidenciaRepository incidencias;

	@Autowired
	private UbicacionRepository ubicaciones;

	@Autowired
	private UsuarioRepository usuarios;

	private Usuario responsable;
	private Usuario tecnico;
	private Ubicacion ubicacion;
	private Equipo equipo;

	@BeforeEach
	void prepararDatos() {
		responsable = usuarios.findAll().stream()
				.filter(u -> u.getPerfil() == Perfil.RESPONSABLE).findFirst().orElseThrow();
		tecnico = usuarios.findAll().stream()
				.filter(u -> u.getPerfil() == Perfil.TECNICO && u.isActivo()).findFirst().orElseThrow();
		ubicacion = ubicaciones.findAll().getFirst();
		equipo = equipoService.crear(responsable.getId(),
				new DatosEquipo("BOM-001", "Bomba principal", "Grundfos", "CR 10", "A1234", ubicacion.getId(), null));
	}

	private CambiosEquipo cambios(String nombre) {
		return new CambiosEquipo(nombre, "Grundfos", "CR 15", "B5678", ubicacion.getId());
	}

	// Editar

	@Test
	void elResponsableEditaLosDatosPeroNoElCodigoYNoEscribeHistorial() {
		Equipo editado = equipoService.editar(responsable.getId(), equipo.getId(), cambios("Bomba de refuerzo"));

		assertThat(editado.getNombre()).isEqualTo("Bomba de refuerzo");
		assertThat(editado.getModelo()).isEqualTo("CR 15");
		assertThat(editado.getCodigo()).isEqualTo("BOM-001");
		assertThat(historialService.consultar(TipoEntidad.EQUIPO, equipo.getId())).hasSize(1);
	}

	@Test
	void rn05UnTecnicoNoPuedeEditarUnEquipo() {
		assertThatExceptionOfType(PermisoDenegadoException.class)
				.isThrownBy(() -> equipoService.editar(tecnico.getId(), equipo.getId(), cambios("Otro nombre")))
				.extracting(PermisoDenegadoException::getCodigo).isEqualTo("RN-05");
	}

	@Test
	void rn14UnEquipoDeBajaNoSeEdita() {
		equipoService.darDeBaja(responsable.getId(), equipo.getId(), "Retirada de la planta");

		assertThatExceptionOfType(ReglaNegocioException.class)
				.isThrownBy(() -> equipoService.editar(responsable.getId(), equipo.getId(), cambios("Otro nombre")))
				.extracting(ReglaNegocioException::getCodigo).isEqualTo("RN-14");
	}

	// Dar de baja

	@Test
	void darDeBajaUnEquipoSinIncidenciasLoDejaEnBajaYLoRegistra() {
		Equipo dado = equipoService.darDeBaja(responsable.getId(), equipo.getId(), "Retirada de la planta");

		assertThat(dado.getEstado()).isEqualTo(EstadoEquipo.BAJA);
		List<Historial> filas = historialService.consultar(TipoEntidad.EQUIPO, equipo.getId());
		assertThat(filas).hasSize(2);
		Historial ultima = filas.getLast();
		assertThat(ultima.getAccion()).isEqualTo(AccionHistorial.CAMBIO_ESTADO);
		assertThat(ultima.getValorAnterior()).isEqualTo("OPERATIVO");
		assertThat(ultima.getValorNuevo()).isEqualTo("BAJA");
		assertThat(ultima.getComentario()).isEqualTo("Retirada de la planta");
	}

	@Test
	void rn05UnTecnicoNoPuedeDarDeBajaUnEquipo() {
		assertThatExceptionOfType(PermisoDenegadoException.class)
				.isThrownBy(() -> equipoService.darDeBaja(tecnico.getId(), equipo.getId(), "Motivo"))
				.extracting(PermisoDenegadoException::getCodigo).isEqualTo("RN-05");
	}

	@Test
	void rn12NoSePuedeDarDeBajaUnEquipoConIncidenciasSinCerrar() {
		incidencias.saveAndFlush(new Incidencia(equipo, responsable, "Fuga en el sello", "Gotea agua.",
				Prioridad.MEDIA, Instant.parse("2026-10-23T09:00:00Z")));

		assertThatExceptionOfType(ReglaNegocioException.class)
				.isThrownBy(() -> equipoService.darDeBaja(responsable.getId(), equipo.getId(), "Motivo"))
				.extracting(ReglaNegocioException::getCodigo).isEqualTo("RN-12");
		assertThat(equipo.getEstado()).isEqualTo(EstadoEquipo.OPERATIVO);
	}

	@Test
	void darDeBajaUnEquipoYaDeBajaDevuelveTransicionNoPermitida() {
		equipoService.darDeBaja(responsable.getId(), equipo.getId(), "Retirada de la planta");

		assertThatExceptionOfType(ReglaNegocioException.class)
				.isThrownBy(() -> equipoService.darDeBaja(responsable.getId(), equipo.getId(), "Otra vez"))
				.extracting(ReglaNegocioException::getCodigo).isEqualTo("RN-01");
	}

	@Test
	void darDeBajaUnEquipoQueNoExisteDevuelveNoEncontrado() {
		assertThatExceptionOfType(RecursoNoEncontradoException.class)
				.isThrownBy(() -> equipoService.darDeBaja(responsable.getId(), 999_999L, "Motivo"))
				.extracting(RecursoNoEncontradoException::getCodigo).isEqualTo("NO_ENCONTRADO");
	}

	// Reactivar

	@Test
	void rn13ReactivarUnEquipoDeBajaLoDevuelveAOperativoYLoRegistra() {
		equipoService.darDeBaja(responsable.getId(), equipo.getId(), "Retirada de la planta");

		Equipo reactivado = equipoService.reactivar(responsable.getId(), equipo.getId(), "Retirada por error");

		assertThat(reactivado.getEstado()).isEqualTo(EstadoEquipo.OPERATIVO);
		Historial ultima = historialService.consultar(TipoEntidad.EQUIPO, equipo.getId()).getLast();
		assertThat(ultima.getValorAnterior()).isEqualTo("BAJA");
		assertThat(ultima.getValorNuevo()).isEqualTo("OPERATIVO");
		assertThat(ultima.getComentario()).isEqualTo("Retirada por error");
	}

	@Test
	void reactivarUnEquipoQueNoEstaDeBajaDevuelveTransicionNoPermitida() {
		assertThatExceptionOfType(ReglaNegocioException.class)
				.isThrownBy(() -> equipoService.reactivar(responsable.getId(), equipo.getId(), "Motivo"))
				.extracting(ReglaNegocioException::getCodigo).isEqualTo("RN-01");
	}

}

