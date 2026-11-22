package com.mantix.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;

import java.time.Instant;
import java.time.LocalDate;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.mantix.TestcontainersConfiguration;
import com.mantix.domain.Equipo;
import com.mantix.domain.Incidencia;
import com.mantix.domain.OrdenTrabajo;
import com.mantix.domain.Perfil;
import com.mantix.domain.Prioridad;
import com.mantix.domain.Ubicacion;
import com.mantix.domain.Usuario;
import com.mantix.repository.EquipoRepository;
import com.mantix.repository.HistorialRepository;
import com.mantix.repository.IncidenciaRepository;
import com.mantix.repository.OrdenTrabajoRepository;
import com.mantix.repository.UbicacionRepository;
import com.mantix.repository.UsuarioRepository;

// Cláusulas de los criterios de aceptación de docs/backend.md (apartado 6) que los demás tests no
// comprueban tal cual. Cada test lleva el número del criterio. Sin @Transactional, como los demás.
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
public class CriteriosDeAceptacionTest {

	private static final Instant LUNES = Instant.parse("2026-10-19T09:00:00Z");
	private static final Instant MARTES = Instant.parse("2026-10-20T09:00:00Z");

	@Autowired
	private MockMvc mvc;

	@Autowired
	private EquipoRepository equipos;

	@Autowired
	private IncidenciaRepository incidencias;

	@Autowired
	private OrdenTrabajoRepository ordenes;

	@Autowired
	private HistorialRepository historial;

	@Autowired
	private UbicacionRepository ubicaciones;

	@Autowired
	private UsuarioRepository usuarios;

	private Ubicacion zonaA;
	private Usuario responsable;
	private Usuario ana;
	private Usuario pablo;
	private Equipo bomba;

	@BeforeEach
	void preparar() {
		zonaA = ubicaciones.findAll(Sort.by("nombre")).get(0);
		responsable = usuarios.findAll().stream().filter(u -> u.getPerfil() == Perfil.RESPONSABLE).findFirst().get();
		var tecnicos = usuarios.findAll(Sort.by("nombre")).stream()
				.filter(u -> u.getPerfil() == Perfil.TECNICO && u.isActivo()).toList();
		ana = tecnicos.get(0);
		pablo = tecnicos.get(1);
		bomba = equipos.save(new Equipo("BOM-001", "Bomba de agua", zonaA, LocalDate.of(2026, 10, 18)));
	}

	@AfterEach
	void limpiar() {
		historial.deleteAll(historial.findAll(Sort.by(Sort.Direction.DESC, "id")));
		ordenes.deleteAll();
		incidencias.deleteAll();
		equipos.deleteAll();
	}

	@Test
	void ca02_elAltaDeUnEquipoDejaUnaFilaAltaEnSuHistorial() throws Exception {
		String cuerpo = "{\"codigo\": \"COM-001\", \"nombre\": \"Compresor\", \"ubicacionId\": " + zonaA.getId() + "}";
		mvc.perform(post("/api/equipos").contentType(MediaType.APPLICATION_JSON).content(cuerpo)
				.header("X-Usuario-Id", responsable.getId()))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.estado").value("OPERATIVO"));

		Long equipoId = equipos.findAll().stream().filter(e -> e.getCodigo().equals("COM-001")).findFirst().get()
				.getId();
		mvc.perform(get("/api/equipos/{id}/historial", equipoId))
				.andExpect(jsonPath("$", hasSize(1)))
				.andExpect(jsonPath("$[0].accion").value("ALTA"))
				.andExpect(jsonPath("$[0].valorNuevo").value("OPERATIVO"))
				.andExpect(jsonPath("$[0].usuario.id").value(responsable.getId()));
	}

	@Test
	void ca05_editarUnEquipoNoCambiaSuCodigoNiEscribeHistorial() throws Exception {
		String cuerpo = "{\"nombre\": \"Bomba principal\", \"ubicacionId\": " + zonaA.getId() + "}";
		mvc.perform(put("/api/equipos/{id}", bomba.getId()).contentType(MediaType.APPLICATION_JSON).content(cuerpo)
				.header("X-Usuario-Id", responsable.getId()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.nombre").value("Bomba principal"))
				.andExpect(jsonPath("$.codigo").value("BOM-001"));

		assertThat(historial.count()).isZero();
	}

	@Test
	void ca10_unResponsableTambienPuedeAbrirUnaIncidencia() throws Exception {
		String cuerpo = "{\"equipoId\": " + bomba.getId()
				+ ", \"titulo\": \"Ruido\", \"descripcion\": \"Suena raro.\", \"prioridad\": \"BAJA\"}";
		mvc.perform(post("/api/incidencias").contentType(MediaType.APPLICATION_JSON).content(cuerpo)
				.header("X-Usuario-Id", responsable.getId()))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.estado").value("ABIERTA"))
				.andExpect(jsonPath("$.reportadaPor.id").value(responsable.getId()));
	}

	@Test
	void ca14_cambiarLaPrioridadDeUnaIncidenciaCerradaDevuelve409() throws Exception {
		Incidencia cerrada = new Incidencia(bomba, ana, "Fuga", "Gotea.", Prioridad.MEDIA, LUNES);
		cerrada.cerrar(MARTES);
		cerrada = incidencias.save(cerrada);

		mvc.perform(post("/api/incidencias/{id}/prioridad", cerrada.getId()).contentType(MediaType.APPLICATION_JSON)
				.content("{\"prioridad\": \"ALTA\"}").header("X-Usuario-Id", responsable.getId()))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.codigo").value("RN-15"));
	}

	@Test
	void ca18_unaOrdenSinTecnicoTambienPoneLaIncidenciaEnCurso() throws Exception {
		Incidencia fuga = incidencias.save(new Incidencia(bomba, ana, "Fuga", "Gotea.", Prioridad.ALTA, LUNES));

		String cuerpo = "{\"incidenciaId\": " + fuga.getId() + ", \"descripcion\": \"Cambiar el sello\"}";
		mvc.perform(post("/api/ordenes").contentType(MediaType.APPLICATION_JSON).content(cuerpo)
				.header("X-Usuario-Id", responsable.getId()))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.estado").value("PENDIENTE"));

		mvc.perform(get("/api/incidencias/{id}", fuga.getId()))
				.andExpect(jsonPath("$.estado").value("EN_CURSO"));
	}

	@Test
	void ca27_reasignarUnaOrdenEnCursoDevuelve409() throws Exception {
		Incidencia fuga = new Incidencia(bomba, ana, "Fuga", "Gotea.", Prioridad.ALTA, LUNES);
		fuga.ponerEnCurso();
		fuga = incidencias.save(fuga);
		OrdenTrabajo enCurso = new OrdenTrabajo(fuga, responsable, ana, "Cambiar el sello", MARTES);
		enCurso.empezar(MARTES);
		enCurso = ordenes.save(enCurso);

		mvc.perform(post("/api/ordenes/{id}/reasignar", enCurso.getId()).contentType(MediaType.APPLICATION_JSON)
				.content("{\"tecnicoId\": " + pablo.getId() + "}").header("X-Usuario-Id", responsable.getId()))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.codigo").value("RN-24"));
	}

}
