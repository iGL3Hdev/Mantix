package com.mantix.web;

import static org.hamcrest.Matchers.containsInAnyOrder;

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
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.mantix.TestcontainersConfiguration;
import com.mantix.domain.Equipo;
import com.mantix.domain.Perfil;
import com.mantix.domain.Ubicacion;
import com.mantix.domain.Usuario;
import com.mantix.repository.EquipoRepository;
import com.mantix.repository.HistorialRepository;
import com.mantix.repository.UbicacionRepository;
import com.mantix.repository.UsuarioRepository;

// Sin @Transactional (igual que EquipoControllerCrearTest): los datos se borran en @AfterEach.
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
public class EquipoControllerEditarTest {

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
	private Ubicacion zonaB;
	private Usuario responsable;
	private Usuario tecnico;
	private Equipo bomba;

	@BeforeEach
	void preparar() {
		var todas = ubicaciones.findAll(Sort.by("nombre"));
		zonaA = todas.get(0);
		zonaB = todas.get(1);
		responsable = usuarios.findAll().stream().filter(u -> u.getPerfil() == Perfil.RESPONSABLE).findFirst().get();
		tecnico = usuarios.findAll().stream().filter(u -> u.getPerfil() == Perfil.TECNICO && u.isActivo()).findFirst()
				.get();
		bomba = equipos.save(new Equipo("BOM-001", "Bomba de agua", zonaA, LocalDate.of(2026, 10, 18)));
	}

	@AfterEach
	void limpiar() {
		historial.deleteAll();
		equipos.deleteAll();
	}

	private String cuerpo(String nombre, Long ubicacionId) {
		return """
				{"nombre": "%s", "ubicacionId": %d, "fabricante": "Grundfos"}
				""".formatted(nombre, ubicacionId);
	}

	private MockHttpServletRequestBuilder editar(Long equipoId, String json) {
		return put("/api/equipos/{id}", equipoId).contentType(MediaType.APPLICATION_JSON).content(json);
	}

	@Test
	void editaElEquipoYDevuelveLosNuevosDatos() throws Exception {
		mvc.perform(editar(bomba.getId(), cuerpo("Bomba principal", zonaB.getId())).header("X-Usuario-Id",
				responsable.getId()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(bomba.getId()))
				.andExpect(jsonPath("$.codigo").value("BOM-001"))
				.andExpect(jsonPath("$.nombre").value("Bomba principal"))
				.andExpect(jsonPath("$.fabricante").value("Grundfos"))
				.andExpect(jsonPath("$.ubicacion.id").value(zonaB.getId()))
				.andExpect(jsonPath("$.ubicacion.nombre").value(zonaB.getNombre()));
	}

	@Test
	void loEditadoQuedaGuardado() throws Exception {
		mvc.perform(editar(bomba.getId(), cuerpo("Bomba principal", zonaB.getId())).header("X-Usuario-Id",
				responsable.getId()))
				.andExpect(status().isOk());

		mvc.perform(get("/api/equipos/{id}", bomba.getId()))
				.andExpect(jsonPath("$.nombre").value("Bomba principal"))
				.andExpect(jsonPath("$.ubicacion.id").value(zonaB.getId()));
	}

	@Test
	void sinCabeceraDeUsuarioDevuelve401() throws Exception {
		mvc.perform(editar(bomba.getId(), cuerpo("Bomba principal", zonaB.getId())))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.codigo").value("USUARIO_NO_IDENTIFICADO"));
	}

	@Test
	void unTecnicoNoPuedeEditarYDevuelve403() throws Exception {
		mvc.perform(editar(bomba.getId(), cuerpo("Bomba principal", zonaB.getId())).header("X-Usuario-Id",
				tecnico.getId()))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.codigo").value("RN-05"));
	}

	@Test
	void unEquipoQueNoExisteDevuelve404() throws Exception {
		mvc.perform(editar(999999L, cuerpo("Bomba principal", zonaB.getId())).header("X-Usuario-Id",
				responsable.getId()))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.codigo").value("NO_ENCONTRADO"))
				.andExpect(jsonPath("$.detail").value("El equipo 999999 no existe."));
	}

	@Test
	void unaUbicacionQueNoExisteDevuelve404() throws Exception {
		mvc.perform(editar(bomba.getId(), cuerpo("Bomba principal", 999999L)).header("X-Usuario-Id",
				responsable.getId()))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.codigo").value("NO_ENCONTRADO"))
				.andExpect(jsonPath("$.detail").value("La ubicación 999999 no existe."));
	}

	@Test
	void unEquipoDeBajaNoSeEditaYDevuelve409() throws Exception {
		bomba.darDeBaja();
		equipos.save(bomba);

		mvc.perform(editar(bomba.getId(), cuerpo("Bomba principal", zonaB.getId())).header("X-Usuario-Id",
				responsable.getId()))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.codigo").value("RN-14"));
	}

	@Test
	void sinNombreNiUbicacionDevuelve400ConLosCampos() throws Exception {
		mvc.perform(editar(bomba.getId(), "{}").header("X-Usuario-Id", responsable.getId()))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.codigo").value("VALIDACION"))
				.andExpect(jsonPath("$.errores[*].campo", containsInAnyOrder("nombre", "ubicacionId")));
	}

}
