package com.mantix.web;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mantix.service.PermisoDenegadoException;
import com.mantix.service.RecursoNoEncontradoException;
import com.mantix.service.ReglaNegocioException;
import com.mantix.service.UsuarioNoIdentificadoException;

// Prueba el manejador de errores solo, sin base de datos: un controlador de prueba lanza cada error
// y se comprueba la respuesta que construye GlobalExceptionHandler.
@WebMvcTest(GlobalExceptionHandlerTest.ControladorDePrueba.class)
@Import({ GlobalExceptionHandler.class, GlobalExceptionHandlerTest.ControladorDePrueba.class })
class GlobalExceptionHandlerTest {

	@RestController
	@RequestMapping("/prueba")
	static class ControladorDePrueba {

		@GetMapping("/regla")
		void regla() {
			throw new ReglaNegocioException("RN-20", "La incidencia 5 ya tiene una orden sin cerrar.");
		}

		@GetMapping("/permiso")
		void permiso() {
			throw new PermisoDenegadoException("RN-05", "Esta acción está reservada al responsable.");
		}

		@GetMapping("/no-encontrado")
		void noEncontrado() {
			throw new RecursoNoEncontradoException("El equipo 9 no existe.");
		}

		@GetMapping("/sin-usuario")
		void sinUsuario() {
			throw new UsuarioNoIdentificadoException("Falta indicar el usuario que hace la acción.");
		}

		@GetMapping("/inesperado")
		void inesperado() {
			throw new IllegalStateException("clave de la base de datos: secreto");
		}
	}

	@Autowired
	private MockMvc mvc;

	@Test
	void unaReglaDeNegocioIncumplidaDevuelve409ConSuCodigo() throws Exception {
		mvc.perform(get("/prueba/regla"))
				.andExpect(status().isConflict())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.status").value(409))
				.andExpect(jsonPath("$.title").value("Regla de negocio incumplida"))
				.andExpect(jsonPath("$.detail").value("La incidencia 5 ya tiene una orden sin cerrar."))
				.andExpect(jsonPath("$.codigo").value("RN-20"))
				.andExpect(jsonPath("$.instance").value("/prueba/regla"));
	}

	@Test
	void unPermisoDenegadoDevuelve403ConSuCodigo() throws Exception {
		mvc.perform(get("/prueba/permiso"))
				.andExpect(status().isForbidden())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.codigo").value("RN-05"))
				.andExpect(jsonPath("$.detail").value("Esta acción está reservada al responsable."));
	}

	@Test
	void unRecursoQueNoExisteDevuelve404() throws Exception {
		mvc.perform(get("/prueba/no-encontrado"))
				.andExpect(status().isNotFound())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.codigo").value("NO_ENCONTRADO"))
				.andExpect(jsonPath("$.detail").value("El equipo 9 no existe."));
	}

	@Test
	void unUsuarioSinIdentificarDevuelve401() throws Exception {
		mvc.perform(get("/prueba/sin-usuario"))
				.andExpect(status().isUnauthorized())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.codigo").value("USUARIO_NO_IDENTIFICADO"));
	}

	@Test
	void unErrorInesperadoDevuelve500SinContarElDetalle() throws Exception {
		mvc.perform(get("/prueba/inesperado"))
				.andExpect(status().isInternalServerError())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.codigo").value("ERROR_INTERNO"))
				.andExpect(content().string(not(containsString("secreto"))));
	}

	@Test
	void unaRutaQueNoExisteDevuelve404ConCodigo() throws Exception {
		mvc.perform(get("/prueba/no-existe"))
				.andExpect(status().isNotFound())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.codigo").value("NO_ENCONTRADO"));
	}

	@Test
	void unMetodoNoPermitidoDevuelve405ConCodigo() throws Exception {
		mvc.perform(post("/prueba/regla"))
				.andExpect(status().isMethodNotAllowed())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.codigo").value("METODO_NO_PERMITIDO"));
	}

}
