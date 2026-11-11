package com.mantix.web;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.mantix.TestcontainersConfiguration;
import com.mantix.domain.Equipo;
import com.mantix.domain.Ubicacion;
import com.mantix.repository.EquipoRepository;
import com.mantix.repository.UbicacionRepository;

// Sin @Transactional a propósito: la petición debe cargar los datos ella sola,
// como en producción (open-in-view=false), y los datos se borran en @AfterEach.
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
public class EquipoControllerTest {

	@Autowired
	private MockMvc mvc;

	@Autowired
	private EquipoRepository equipos;

	@Autowired
	private UbicacionRepository ubicaciones;

	private Ubicacion zonaA;
	private Ubicacion zonaB;

	@BeforeEach
	void prepararUbicaciones() {
		var todas = ubicaciones.findAll(Sort.by("nombre"));
		zonaA = todas.get(0);
		zonaB = todas.get(1);
	}

	@AfterEach
	void limpiar() {
		equipos.deleteAll();
	}

	private Equipo equipo(String codigo, String nombre, Ubicacion ubicacion) {
		return equipos.save(new Equipo(codigo, nombre, ubicacion, LocalDate.of(2026, 10, 18)));
	}

	@Test
	void listaLosEquiposOrdenadosPorCodigo() throws Exception {
		equipo("MOT-001", "Motor de cinta", zonaA);
		equipo("BOM-001", "Bomba de agua", zonaA);
		equipo("COM-001", "Compresor", zonaB);

		mvc.perform(get("/api/equipos"))
				.andExpect(status().isOk())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$[*].codigo", contains("BOM-001", "COM-001", "MOT-001")));
	}

	@Test
	void cadaEquipoExponeSusNueveCamposConLaUbicacionResumida() throws Exception {
		equipo("BOM-001", "Bomba de agua", zonaA);

		mvc.perform(get("/api/equipos"))
				.andExpect(jsonPath("$[0].*", hasSize(9)))
				.andExpect(jsonPath("$[0].id").isNumber())
				.andExpect(jsonPath("$[0].codigo").value("BOM-001"))
				.andExpect(jsonPath("$[0].nombre").value("Bomba de agua"))
				.andExpect(jsonPath("$[0].estado").value("OPERATIVO"))
				.andExpect(jsonPath("$[0].fechaAlta").value("2026-10-18"))
				.andExpect(jsonPath("$[0].ubicacion.*", hasSize(2)))
				.andExpect(jsonPath("$[0].ubicacion.id").value(zonaA.getId()))
				.andExpect(jsonPath("$[0].ubicacion.nombre").value(zonaA.getNombre()));
	}

	@Test
	void filtraPorEstado() throws Exception {
		equipo("BOM-001", "Bomba de agua", zonaA);
		Equipo averiado = new Equipo("COM-001", "Compresor", zonaA, LocalDate.of(2026, 10, 18));
		averiado.marcarAveriado();
		equipos.save(averiado);

		mvc.perform(get("/api/equipos").param("estado", "AVERIADO"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(1)))
				.andExpect(jsonPath("$[*].estado", everyItem(is("AVERIADO"))))
				.andExpect(jsonPath("$[0].codigo").value("COM-001"));
	}

	@Test
	void filtraPorUbicacion() throws Exception {
		equipo("BOM-001", "Bomba de agua", zonaA);
		equipo("COM-001", "Compresor", zonaB);

		mvc.perform(get("/api/equipos").param("ubicacionId", zonaB.getId().toString()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(1)))
				.andExpect(jsonPath("$[0].codigo").value("COM-001"));
	}

	@Test
	void buscaPorTextoEnCodigoYNombreSinDistinguirMayusculas() throws Exception {
		equipo("BOM-001", "Bomba de agua", zonaA);
		equipo("COM-001", "Compresor", zonaA);
		equipo("MOT-001", "Motor de la bomba auxiliar", zonaA);

		mvc.perform(get("/api/equipos").param("q", "bomba"))
				.andExpect(jsonPath("$[*].codigo", contains("BOM-001", "MOT-001")));

		mvc.perform(get("/api/equipos").param("q", "com-0"))
				.andExpect(jsonPath("$[*].codigo", contains("COM-001")));
	}

	@Test
	void sinFiltrosNoDescartaNingunEquipo() throws Exception {
		equipo("BOM-001", "Bomba de agua", zonaA);
		equipo("COM-001", "Compresor", zonaB);

		mvc.perform(get("/api/equipos"))
				.andExpect(jsonPath("$", hasSize(2)));
	}

	@Test
	void devuelveUnEquipoPorId() throws Exception {
		Equipo bomba = equipo("BOM-001", "Bomba de agua", zonaA);

		mvc.perform(get("/api/equipos/{id}", bomba.getId()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(bomba.getId()))
				.andExpect(jsonPath("$.codigo").value("BOM-001"))
				.andExpect(jsonPath("$.ubicacion.nombre").value(zonaA.getNombre()));
	}

	@Test
	void unEquipoQueNoExisteDevuelve404() throws Exception {
		mvc.perform(get("/api/equipos/{id}", 999999))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.codigo").value("NO_ENCONTRADO"))
				.andExpect(jsonPath("$.detail").value("El equipo 999999 no existe."));
	}

	@Test
	void unEstadoInvalidoDevuelve400() throws Exception {
		mvc.perform(get("/api/equipos").param("estado", "ROTO"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.codigo").value("VALIDACION"));
	}

}
