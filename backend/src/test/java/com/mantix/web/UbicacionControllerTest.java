package com.mantix.web;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.mantix.TestcontainersConfiguration;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
public class UbicacionControllerTest {

	@Autowired
	private MockMvc mvc;

	@Test
	void listaTodasLasUbicacionesOrdenadasPorNombre() throws Exception {
		mvc.perform(get("/api/ubicaciones"))
				.andExpect(status().isOk())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$", hasSize(3)))
				.andExpect(jsonPath("$[*].nombre",
						contains("Planta 1 – Zona A", "Planta 1 – Zona B", "Sala de máquinas")));
	}

	@Test
	void cadaUbicacionExponeSoloSusTresCampos() throws Exception {
		mvc.perform(get("/api/ubicaciones"))
				.andExpect(jsonPath("$[0].*", hasSize(3)))
				.andExpect(jsonPath("$[0].id").isNumber())
				.andExpect(jsonPath("$[0].nombre").isString())
				.andExpect(jsonPath("$[0].descripcion").isString());
	}

}