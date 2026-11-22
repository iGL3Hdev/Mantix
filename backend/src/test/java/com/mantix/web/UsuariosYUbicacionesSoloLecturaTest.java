package com.mantix.web;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.mantix.TestcontainersConfiguration;

// RN-08: usuarios y ubicaciones se cargan con la migración de datos iniciales; en el MVP la API solo los lee.
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
public class UsuariosYUbicacionesSoloLecturaTest {

	private static final List<String> RUTAS = List.of("/api/usuarios", "/api/usuarios/1", "/api/ubicaciones");

	@Autowired
	private MockMvc mvc;

	private void exigir405(HttpMethod metodo) throws Exception {
		for (String ruta : RUTAS) {
			mvc.perform(request(metodo, ruta).contentType("application/json").content("{}"))
					.andExpect(status().isMethodNotAllowed())
					.andExpect(jsonPath("$.codigo").value("METODO_NO_PERMITIDO"));
		}
	}

	@Test
	void noSePuedeCrearNingunUsuarioNiUbicacion() throws Exception {
		exigir405(HttpMethod.POST);
	}

	@Test
	void noSePuedeModificarNingunUsuarioNiUbicacion() throws Exception {
		exigir405(HttpMethod.PUT);
		exigir405(HttpMethod.PATCH);
	}

	@Test
	void noSePuedeBorrarNingunUsuarioNiUbicacion() throws Exception {
		exigir405(HttpMethod.DELETE);
	}

}