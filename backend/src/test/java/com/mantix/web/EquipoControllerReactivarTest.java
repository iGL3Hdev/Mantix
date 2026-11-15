package com.mantix.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;

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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.mantix.TestcontainersConfiguration;
import com.mantix.domain.AccionHistorial;
import com.mantix.domain.Equipo;
import com.mantix.domain.Historial;
import com.mantix.domain.Perfil;
import com.mantix.domain.TipoEntidad;
import com.mantix.domain.Ubicacion;
import com.mantix.domain.Usuario;
import com.mantix.repository.EquipoRepository;
import com.mantix.repository.HistorialRepository;
import com.mantix.repository.UbicacionRepository;
import com.mantix.repository.UsuarioRepository;

// Sin @Transactional (igual que los demás tests de equipos): los datos se borran en @AfterEach.
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
public class EquipoControllerReactivarTest {

	private static final String MOTIVO = "{\"comentario\": \"Reparado y revisado\"}";

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

	private Usuario responsable;
	private Usuario tecnico;
	private Equipo bomba;

	@BeforeEach
	void preparar() {
		Ubicacion zonaA = ubicaciones.findAll(Sort.by("nombre")).get(0);
		responsable = usuarios.findAll().stream().filter(u -> u.getPerfil() == Perfil.RESPONSABLE).findFirst().get();
		tecnico = usuarios.findAll().stream().filter(u -> u.getPerfil() == Perfil.TECNICO && u.isActivo()).findFirst()
				.get();
		bomba = new Equipo("BOM-001", "Bomba de agua", zonaA, LocalDate.of(2026, 10, 18));
		bomba.darDeBaja();
		bomba = equipos.save(bomba);
	}

	@AfterEach
	void limpiar() {
		historial.deleteAll();
		equipos.deleteAll();
	}

	private MockHttpServletRequestBuilder reactivar(Long equipoId, String json) {
		return post("/api/equipos/{id}/reactivar", equipoId).contentType(MediaType.APPLICATION_JSON).content(json);
	}

	@Test
	void reactivaElEquipoYDevuelveElEquipoActualizado() throws Exception {
		mvc.perform(reactivar(bomba.getId(), MOTIVO).header("X-Usuario-Id", responsable.getId()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(bomba.getId()))
				.andExpect(jsonPath("$.estado").value("OPERATIVO"));
	}

	@Test
	void laReactivacionQuedaGuardadaYRegistradaEnElHistorialConSuMotivo() throws Exception {
		mvc.perform(reactivar(bomba.getId(), MOTIVO).header("X-Usuario-Id", responsable.getId()))
				.andExpect(status().isOk());

		mvc.perform(get("/api/equipos/{id}", bomba.getId()))
				.andExpect(jsonPath("$.estado").value("OPERATIVO"));

		List<Historial> entradas = historial.findByTipoEntidadAndEntidadIdOrderByFechaAscIdAsc(TipoEntidad.EQUIPO,
				bomba.getId());
		assertThat(entradas).hasSize(1);
		assertThat(entradas.get(0).getAccion()).isEqualTo(AccionHistorial.CAMBIO_ESTADO);
		assertThat(entradas.get(0).getValorAnterior()).isEqualTo("BAJA");
		assertThat(entradas.get(0).getValorNuevo()).isEqualTo("OPERATIVO");
		assertThat(entradas.get(0).getComentario()).isEqualTo("Reparado y revisado");
	}

	@Test
	void sinCabeceraDeUsuarioDevuelve401() throws Exception {
		mvc.perform(reactivar(bomba.getId(), MOTIVO))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.codigo").value("USUARIO_NO_IDENTIFICADO"));
	}

	@Test
	void unTecnicoNoPuedeReactivarYDevuelve403() throws Exception {
		mvc.perform(reactivar(bomba.getId(), MOTIVO).header("X-Usuario-Id", tecnico.getId()))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.codigo").value("RN-05"));
	}

	@Test
	void unEquipoQueNoExisteDevuelve404() throws Exception {
		mvc.perform(reactivar(999999L, MOTIVO).header("X-Usuario-Id", responsable.getId()))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.codigo").value("NO_ENCONTRADO"))
				.andExpect(jsonPath("$.detail").value("El equipo 999999 no existe."));
	}

	@Test
	void unEquipoQueNoEstaDeBajaDevuelve409() throws Exception {
		bomba.reactivar();
		equipos.save(bomba);

		mvc.perform(reactivar(bomba.getId(), MOTIVO).header("X-Usuario-Id", responsable.getId()))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.codigo").value("RN-01"));
	}

	@Test
	void sinComentarioDevuelve400YNoReactivaElEquipo() throws Exception {
		mvc.perform(reactivar(bomba.getId(), "{}").header("X-Usuario-Id", responsable.getId()))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.codigo").value("VALIDACION"))
				.andExpect(jsonPath("$.errores[*].campo", contains("comentario")));

		mvc.perform(get("/api/equipos/{id}", bomba.getId()))
				.andExpect(jsonPath("$.estado").value("BAJA"));
	}

}
