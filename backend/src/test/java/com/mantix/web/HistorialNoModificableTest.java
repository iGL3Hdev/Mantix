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

// CA-33: no hay ningún endpoint para modificar ni borrar el historial.
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
public class HistorialNoModificableTest {

	private static final List<String> RECURSOS = List.of("equipos", "incidencias", "ordenes");

	@Autowired
	private MockMvc mvc;

	private void exigir405(HttpMethod metodo) throws Exception {
		for (String recurso : RECURSOS) {
			mvc.perform(request(metodo, "/api/{recurso}/1/historial", recurso))
					.andExpect(status().isMethodNotAllowed())
					.andExpect(jsonPath("$.codigo").value("METODO_NO_PERMITIDO"));
		}
	}

	@Test
	void putSobreElHistorialDevuelve405() throws Exception {
		exigir405(HttpMethod.PUT);
	}

	@Test
	void deleteSobreElHistorialDevuelve405() throws Exception {
		exigir405(HttpMethod.DELETE);
	}

	@Test
	void postSobreElHistorialDevuelve405() throws Exception {
		exigir405(HttpMethod.POST);
	}

	@Test
	void patchSobreElHistorialDevuelve405() throws Exception {
		exigir405(HttpMethod.PATCH);
	}

}
