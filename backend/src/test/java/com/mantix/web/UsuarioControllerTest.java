package com.mantix.web;

import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
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
import com.mantix.domain.Usuario;
import com.mantix.repository.UsuarioRepository;


@Import (TestcontainersConfiguration.class)
@SpringBootTest 
@AutoConfigureMockMvc 
public class UsuarioControllerTest {

    @Autowired 
    private MockMvc mvc;

    @Autowired 
    private UsuarioRepository usuarios;

    @Test
    void listaTodosLosUsuariosOrdenadosPorNombre() throws Exception {
        mvc.perform(get("/api/usuarios"))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$", hasSize(4)))
            .andExpect(jsonPath("$[*].nombre").value(
						org.hamcrest.Matchers.contains("Ana Ruiz", "Luis Gómez", "Marta Vidal", "Pablo Serra")));
    }

    @Test 
    void cadaUsuarioExponeSoloSusCincoCampos() throws Exception {
        mvc.perform(get("/api/usuarios"))
            .andExpect(jsonPath("$[0].*", hasSize(5)))
			.andExpect(jsonPath("$[0].id").isNumber())
			.andExpect(jsonPath("$[0].nombre").isString())
			.andExpect(jsonPath("$[0].email").isString())
			.andExpect(jsonPath("$[0].perfil").isString())
			.andExpect(jsonPath("$[0].activo").isBoolean());
    }

    @Test
	void filtraPorPerfil() throws Exception {
		mvc.perform(get("/api/usuarios").param("perfil", "TECNICO"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(3)))
				.andExpect(jsonPath("$[*].perfil", everyItem(is("TECNICO"))));
    
    }

    @Test
	void filtraPorActivoYPerfilALaVez() throws Exception {
		mvc.perform(get("/api/usuarios").param("perfil", "TECNICO").param("activo", "true"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(2)))
				.andExpect(jsonPath("$[*].activo", everyItem(is(true))));
	}

    @Test
	void devuelveUnUsuarioPorSuId() throws Exception {
		Usuario ana = usuarios.findAll().stream().filter(u -> u.getNombre().equals("Ana Ruiz")).findFirst()
				.orElseThrow();

		mvc.perform(get("/api/usuarios/{id}", ana.getId()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(ana.getId()))
				.andExpect(jsonPath("$.nombre").value("Ana Ruiz"))
				.andExpect(jsonPath("$.email").value("ana@mantix.local"))
				.andExpect(jsonPath("$.perfil").value("TECNICO"))
				.andExpect(jsonPath("$.activo").value(true));
	}

    @Test
	void unUsuarioQueNoExisteDevuelve404() throws Exception {
		mvc.perform(get("/api/usuarios/{id}", 999_999))
				.andExpect(status().isNotFound())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.codigo").value("NO_ENCONTRADO"))
				.andExpect(jsonPath("$.detail").value("El usuario 999999 no existe."));
	}

    @Test
	void unPerfilQueNoExisteDevuelve400() throws Exception {
		mvc.perform(get("/api/usuarios").param("perfil", "JEFE"))
				.andExpect(status().isBadRequest())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.codigo").value("VALIDACION"));
	}

    @Test
	void unIdQueNoEsUnNumeroDevuelve400() throws Exception {
		mvc.perform(get("/api/usuarios/abc"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.codigo").value("VALIDACION"));
	}
}
