package com.mantix.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.nullValue;

import java.time.LocalDate;
import java.util.List;

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
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.mantix.TestcontainersConfiguration;
import com.mantix.domain.AccionHistorial;
import com.mantix.domain.Equipo;
import com.mantix.domain.Historial;
import com.mantix.domain.Incidencia;
import com.mantix.domain.Perfil;
import com.mantix.domain.TipoEntidad;
import com.mantix.domain.Ubicacion;
import com.mantix.domain.Usuario;
import com.mantix.repository.EquipoRepository;
import com.mantix.repository.HistorialRepository;
import com.mantix.repository.IncidenciaRepository;
import com.mantix.repository.UbicacionRepository;
import com.mantix.repository.UsuarioRepository;

// Sin @Transactional (igual que los demás tests de controladores): los datos se borran en @AfterEach.
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
public class IncidenciaControllerCrearTest {

	@Autowired
	private MockMvc mvc;

	@Autowired
	private EquipoRepository equipos;

	@Autowired
	private IncidenciaRepository incidencias;

	@Autowired
	private HistorialRepository historial;

	@Autowired
	private UbicacionRepository ubicaciones;

	@Autowired
	private UsuarioRepository usuarios;

	private Usuario tecnico;
	private Usuario inactivo;
	private Equipo bomba;

	@BeforeEach
	void preparar() {
		Ubicacion zonaA = ubicaciones.findAll(Sort.by("nombre")).get(0);
		tecnico = usuarios.findAll().stream().filter(u -> u.getPerfil() == Perfil.TECNICO && u.isActivo()).findFirst()
				.get();
		inactivo = usuarios.findAll().stream().filter(u -> !u.isActivo()).findFirst().get();
		bomba = equipos.save(new Equipo("BOM-001", "Bomba de agua", zonaA, LocalDate.of(2026, 10, 18)));
	}

	@AfterEach
	void limpiar() {
		// Las entradas consecuencia apuntan a su origen: se borran primero las más recientes.
		historial.deleteAll(historial.findAll(Sort.by(Sort.Direction.DESC, "id")));
		incidencias.deleteAll();
		equipos.deleteAll();
	}

	private String cuerpo(Long equipoId, String prioridad) {
		return """
				{"equipoId": %d, "titulo": "Fuga en el sello", "descripcion": "Gotea agua por el eje.", "prioridad": "%s"}
				""".formatted(equipoId, prioridad);
	}

	private MockHttpServletRequestBuilder crear(String json) {
		return post("/api/incidencias").contentType(MediaType.APPLICATION_JSON).content(json);
	}

	private List<Historial> historialDe(TipoEntidad tipo, Long id) {
		return historial.findByTipoEntidadAndEntidadIdOrderByFechaAscIdAsc(tipo, id);
	}

	@Test
	void creaLaIncidenciaYDevuelve201() throws Exception {
		mvc.perform(crear(cuerpo(bomba.getId(), "ALTA")).header("X-Usuario-Id", tecnico.getId()))
				.andExpect(status().isCreated())
				.andExpect(header().string("Location", matchesPattern(".*/api/incidencias/[0-9]+")))
				.andExpect(jsonPath("$.id").isNumber())
				.andExpect(jsonPath("$.titulo").value("Fuga en el sello"))
				.andExpect(jsonPath("$.descripcion").value("Gotea agua por el eje."))
				.andExpect(jsonPath("$.prioridad").value("ALTA"))
				.andExpect(jsonPath("$.estado").value("ABIERTA"))
				.andExpect(jsonPath("$.equipo.id").value(bomba.getId()))
				.andExpect(jsonPath("$.equipo.codigo").value("BOM-001"))
				.andExpect(jsonPath("$.reportadaPor.id").value(tecnico.getId()))
				.andExpect(jsonPath("$.fechaApertura").isString())
				.andExpect(jsonPath("$.fechaCierre").value(nullValue()))
				.andExpect(jsonPath("$.ordenActivaId").value(nullValue()));
	}

	@Test
	void laIncidenciaQuedaGuardadaConSuAltaEnElHistorial() throws Exception {
		mvc.perform(crear(cuerpo(bomba.getId(), "ALTA")).header("X-Usuario-Id", tecnico.getId()))
				.andExpect(status().isCreated());

		List<Incidencia> guardadas = incidencias.findAll();
		assertThat(guardadas).hasSize(1);
		List<Historial> entradas = historialDe(TipoEntidad.INCIDENCIA, guardadas.get(0).getId());
		assertThat(entradas).hasSize(1);
		assertThat(entradas.get(0).getAccion()).isEqualTo(AccionHistorial.ALTA);
		assertThat(entradas.get(0).getValorNuevo()).isEqualTo("ABIERTA");
	}

	@Test
	void unaIncidenciaCriticaAveriaElEquipoYSuCambioApuntaAlAltaDeLaIncidencia() throws Exception {
		mvc.perform(crear(cuerpo(bomba.getId(), "CRITICA")).header("X-Usuario-Id", tecnico.getId()))
				.andExpect(status().isCreated());

		mvc.perform(get("/api/equipos/{id}", bomba.getId()))
				.andExpect(jsonPath("$.estado").value("AVERIADO"));

		Long incidenciaId = incidencias.findAll().get(0).getId();
		Long altaId = historialDe(TipoEntidad.INCIDENCIA, incidenciaId).get(0).getId();
		mvc.perform(get("/api/equipos/{id}/historial", bomba.getId()))
				.andExpect(jsonPath("$", hasSize(1)))
				.andExpect(jsonPath("$[0].accion").value("CAMBIO_ESTADO"))
				.andExpect(jsonPath("$[0].valorAnterior").value("OPERATIVO"))
				.andExpect(jsonPath("$[0].valorNuevo").value("AVERIADO"))
				.andExpect(jsonPath("$[0].origenId").value(altaId));
	}

	@Test
	void unaIncidenciaNoCriticaDejaElEquipoOperativo() throws Exception {
		mvc.perform(crear(cuerpo(bomba.getId(), "ALTA")).header("X-Usuario-Id", tecnico.getId()))
				.andExpect(status().isCreated());

		mvc.perform(get("/api/equipos/{id}", bomba.getId()))
				.andExpect(jsonPath("$.estado").value("OPERATIVO"));
	}

	@Test
	void sinCabeceraDeUsuarioDevuelve401() throws Exception {
		mvc.perform(crear(cuerpo(bomba.getId(), "ALTA")))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.codigo").value("USUARIO_NO_IDENTIFICADO"));
	}

	@Test
	void unUsuarioInactivoDevuelve403() throws Exception {
		mvc.perform(crear(cuerpo(bomba.getId(), "ALTA")).header("X-Usuario-Id", inactivo.getId()))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.codigo").value("RN-04"));
	}

	@Test
	void unEquipoQueNoExisteDevuelve404() throws Exception {
		mvc.perform(crear(cuerpo(999999L, "ALTA")).header("X-Usuario-Id", tecnico.getId()))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.codigo").value("NO_ENCONTRADO"))
				.andExpect(jsonPath("$.detail").value("El equipo 999999 no existe."));
	}

	@Test
	void unEquipoDeBajaDevuelve409() throws Exception {
		bomba.darDeBaja();
		equipos.save(bomba);

		mvc.perform(crear(cuerpo(bomba.getId(), "ALTA")).header("X-Usuario-Id", tecnico.getId()))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.codigo").value("RN-14"));
	}

	@Test
	void sinCamposObligatoriosDevuelve400ConTodosLosErrores() throws Exception {
		mvc.perform(crear("{}").header("X-Usuario-Id", tecnico.getId()))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.codigo").value("VALIDACION"))
				.andExpect(jsonPath("$.errores[*].campo",
						containsInAnyOrder("equipoId", "titulo", "descripcion", "prioridad")));

		assertThat(incidencias.count()).isZero();
	}

	@Test
	void unTituloDemasiadoLargoDevuelve400() throws Exception {
		String json = """
				{"equipoId": %d, "titulo": "%s", "descripcion": "Gotea", "prioridad": "ALTA"}
				""".formatted(bomba.getId(), "T".repeat(151));

		mvc.perform(crear(json).header("X-Usuario-Id", tecnico.getId()))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errores[*].campo", containsInAnyOrder("titulo")));
	}

	@Test
	void unaPrioridadQueNoExisteDevuelve400() throws Exception {
		mvc.perform(crear(cuerpo(bomba.getId(), "URGENTE")).header("X-Usuario-Id", tecnico.getId()))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.codigo").value("VALIDACION"));

		assertThat(incidencias.count()).isZero();
	}

	@Test
	void unaDescripcionDeMasDeCincoMilCaracteresDevuelve400() throws Exception {
		String json = """
				{"equipoId": %d, "titulo": "Fuga", "descripcion": "%s", "prioridad": "ALTA"}
				""".formatted(bomba.getId(), "x".repeat(5001));

		mvc.perform(crear(json).header("X-Usuario-Id", tecnico.getId()))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errores[*].campo", containsInAnyOrder("descripcion")))
				.andExpect(jsonPath("$.errores[0].mensaje").value("La descripción admite como máximo 5000 caracteres."));

		assertThat(incidencias.count()).isZero();
	}

}

