package com.mantix.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.matchesPattern;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.mantix.TestcontainersConfiguration;
import com.mantix.domain.Perfil;
import com.mantix.domain.Ubicacion;
import com.mantix.domain.Usuario;
import com.mantix.repository.EquipoRepository;
import com.mantix.repository.HistorialRepository;
import com.mantix.repository.UbicacionRepository;
import com.mantix.repository.UsuarioRepository;

// Sin @Transactional (igual que EquipoControllerTest): el servicio confirma de verdad
// y los datos se borran en @AfterEach.
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
public class EquipoControllerCrearTest {

	@Autowired
	private MockMvc mvc;

	@Autowired
	private EquipoRepository equipos;

	@Autowired
	private HistorialRepository historial;

	@Autowired
	private UbicacionRepository ubicaciones;

	@Autowired
	private UsuarioRepository usuarios;

	private Ubicacion zonaA;
	private Usuario responsable;
	private Usuario tecnico;

	@BeforeEach
	void preparar() {
		zonaA = ubicaciones.findAll(Sort.by("nombre")).get(0);
		responsable = usuarios.findAll().stream().filter(u -> u.getPerfil() == Perfil.RESPONSABLE).findFirst().get();
		tecnico = usuarios.findAll().stream().filter(u -> u.getPerfil() == Perfil.TECNICO && u.isActivo()).findFirst()
				.get();
	}

	@AfterEach
	void limpiar() {
		historial.deleteAll();
		equipos.deleteAll();
	}

	private String cuerpo(String codigo, Long ubicacionId) {
		return """
				{"codigo": "%s", "nombre": "Bomba de agua", "ubicacionId": %d}
				""".formatted(codigo, ubicacionId);
	}

	private MockHttpServletRequestBuilder crear(String json) {
		return post("/api/equipos").contentType(MediaType.APPLICATION_JSON).content(json);
	}

	@Test
	void creaElEquipoYDevuelve201ConSuUbicacion() throws Exception {
		mvc.perform(crear(cuerpo("BOM-001", zonaA.getId())).header("X-Usuario-Id", responsable.getId()))
				.andExpect(status().isCreated())
				.andExpect(header().string("Location", matchesPattern(".*/api/equipos/[0-9]+")))
				.andExpect(jsonPath("$.id").isNumber())
				.andExpect(jsonPath("$.codigo").value("BOM-001"))
				.andExpect(jsonPath("$.nombre").value("Bomba de agua"))
				.andExpect(jsonPath("$.estado").value("OPERATIVO"))
				.andExpect(jsonPath("$.fechaAlta").isString())
				.andExpect(jsonPath("$.ubicacion.id").value(zonaA.getId()))
				.andExpect(jsonPath("$.ubicacion.nombre").value(zonaA.getNombre()));
	}

	@Test
	void guardaElEquipoEnLaBaseDeDatos() throws Exception {
		mvc.perform(crear(cuerpo("BOM-001", zonaA.getId())).header("X-Usuario-Id", responsable.getId()))
				.andExpect(status().isCreated());

		assertThat(equipos.existsByCodigo("BOM-001")).isTrue();
	}

	@Test
	void respetaLaFechaDeAltaSiSeIndica() throws Exception {
		String json = """
				{"codigo": "BOM-001", "nombre": "Bomba de agua", "ubicacionId": %d, "fechaAlta": "2026-01-15"}
				""".formatted(zonaA.getId());

		mvc.perform(crear(json).header("X-Usuario-Id", responsable.getId()))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.fechaAlta").value("2026-01-15"));
	}

	@Test
	void sinCabeceraDeUsuarioDevuelve401() throws Exception {
		mvc.perform(crear(cuerpo("BOM-001", zonaA.getId())))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.codigo").value("USUARIO_NO_IDENTIFICADO"));
	}

	@Test
	void unUsuarioQueNoExisteDevuelve401() throws Exception {
		mvc.perform(crear(cuerpo("BOM-001", zonaA.getId())).header("X-Usuario-Id", 999999))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.codigo").value("USUARIO_NO_IDENTIFICADO"));
	}

	@Test
	void unTecnicoNoPuedeCrearEquiposYDevuelve403() throws Exception {
		mvc.perform(crear(cuerpo("BOM-001", zonaA.getId())).header("X-Usuario-Id", tecnico.getId()))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.codigo").value("RN-05"));
	}

	@Test
	void unCodigoRepetidoDevuelve409() throws Exception {
		mvc.perform(crear(cuerpo("BOM-001", zonaA.getId())).header("X-Usuario-Id", responsable.getId()))
				.andExpect(status().isCreated());

		mvc.perform(crear(cuerpo("BOM-001", zonaA.getId())).header("X-Usuario-Id", responsable.getId()))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.codigo").value("RN-09"));
	}

	@Test
	void unaUbicacionQueNoExisteDevuelve404() throws Exception {
		mvc.perform(crear(cuerpo("BOM-001", 999999L)).header("X-Usuario-Id", responsable.getId()))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.codigo").value("NO_ENCONTRADO"));
	}

		@Test
	void unCodigoVacioDevuelve400IndicandoElCampo() throws Exception {
		String json = """
				{"codigo": "", "nombre": "Bomba de agua", "ubicacionId": %d}
				""".formatted(zonaA.getId());

		mvc.perform(crear(json).header("X-Usuario-Id", responsable.getId()))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.codigo").value("VALIDACION"))
				.andExpect(jsonPath("$.errores", hasSize(1)))
				.andExpect(jsonPath("$.errores[0].*", hasSize(2)))
				.andExpect(jsonPath("$.errores[0].campo").value("codigo"))
				.andExpect(jsonPath("$.errores[0].mensaje").value("El código es obligatorio."));
	}

	@Test
	void sinCamposObligatoriosListaTodosLosErrores() throws Exception {
		mvc.perform(crear("{}").header("X-Usuario-Id", responsable.getId()))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.codigo").value("VALIDACION"))
				.andExpect(jsonPath("$.errores[*].campo", containsInAnyOrder("codigo", "nombre", "ubicacionId")));
	}

	@Test
	void textosDemasiadoLargosDevuelven400() throws Exception {
		String json = """
				{"codigo": "%s", "nombre": "%s", "ubicacionId": %d}
				""".formatted("C".repeat(21), "N".repeat(101), zonaA.getId());

		mvc.perform(crear(json).header("X-Usuario-Id", responsable.getId()))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errores[*].campo", containsInAnyOrder("codigo", "nombre")));
	}

	@Test
	void unCuerpoInvalidoNoCreaNingunEquipo() throws Exception {
		mvc.perform(crear("{}").header("X-Usuario-Id", responsable.getId()))
				.andExpect(status().isBadRequest());

		assertThat(equipos.count()).isZero();
	}


}

