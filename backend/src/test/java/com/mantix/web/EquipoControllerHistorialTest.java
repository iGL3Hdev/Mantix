package com.mantix.web;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;

import java.time.Instant;
import java.time.LocalDate;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Sort;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
public class EquipoControllerHistorialTest {

	private static final Instant LUNES = Instant.parse("2026-10-18T10:00:00Z");
	private static final Instant MARTES = Instant.parse("2026-10-19T10:00:00Z");

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
	private Equipo bomba;
	private Equipo compresor;

	@BeforeEach
	void preparar() {
		Ubicacion zonaA = ubicaciones.findAll(Sort.by("nombre")).get(0);
		responsable = usuarios.findAll().stream().filter(u -> u.getPerfil() == Perfil.RESPONSABLE).findFirst().get();
		bomba = equipos.save(new Equipo("BOM-001", "Bomba de agua", zonaA, LocalDate.of(2026, 10, 18)));
		compresor = equipos.save(new Equipo("COM-001", "Compresor", zonaA, LocalDate.of(2026, 10, 18)));
	}

	@AfterEach
	void limpiar() {
		// Las entradas consecuencia apuntan a su origen: se borran primero las más recientes.
		historial.deleteAll(historial.findAll(Sort.by(Sort.Direction.DESC, "id")));
		equipos.deleteAll();
	}

	private Historial alta(Equipo equipo, Instant fecha) {
		return historial.save(new Historial(TipoEntidad.EQUIPO, equipo.getId(), AccionHistorial.ALTA, null,
				"OPERATIVO", null, responsable, fecha, null));
	}

	@Test
	void devuelveLasEntradasEnOrdenCronologico() throws Exception {
		// Se guardan al revés a propósito: el orden lo pone la consulta, no el orden de inserción.
		historial.save(new Historial(TipoEntidad.EQUIPO, bomba.getId(), AccionHistorial.CAMBIO_ESTADO, "OPERATIVO",
				"BAJA", null, responsable, MARTES, "Retirada"));
		alta(bomba, LUNES);

		mvc.perform(get("/api/equipos/{id}/historial", bomba.getId()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[*].accion", contains("ALTA", "CAMBIO_ESTADO")))
				.andExpect(jsonPath("$[*].fecha", contains("2026-10-18T10:00:00Z", "2026-10-19T10:00:00Z")));
	}

	@Test
	void cadaEntradaExponeSusDiezCamposConElUsuarioResumido() throws Exception {
		Historial entrada = alta(bomba, LUNES);

		mvc.perform(get("/api/equipos/{id}/historial", bomba.getId()))
				.andExpect(jsonPath("$[0].*", hasSize(10)))
				.andExpect(jsonPath("$[0].id").value(entrada.getId()))
				.andExpect(jsonPath("$[0].tipoEntidad").value("EQUIPO"))
				.andExpect(jsonPath("$[0].entidadId").value(bomba.getId()))
				.andExpect(jsonPath("$[0].accion").value("ALTA"))
				.andExpect(jsonPath("$[0].valorAnterior").value(nullValue()))
				.andExpect(jsonPath("$[0].valorNuevo").value("OPERATIVO"))
				.andExpect(jsonPath("$[0].origenId").value(nullValue()))
				.andExpect(jsonPath("$[0].comentario").value(nullValue()))
				.andExpect(jsonPath("$[0].usuario.*", hasSize(2)))
				.andExpect(jsonPath("$[0].usuario.id").value(responsable.getId()))
				.andExpect(jsonPath("$[0].usuario.nombre").value(responsable.getNombre()));
	}

	@Test
	void unaEntradaConsecuenciaIndicaElIdDeSuOrigen() throws Exception {
		Historial principal = alta(bomba, LUNES);
		historial.save(new Historial(TipoEntidad.EQUIPO, bomba.getId(), AccionHistorial.CAMBIO_ESTADO, "OPERATIVO",
				"AVERIADO", principal, responsable, MARTES, null));

		mvc.perform(get("/api/equipos/{id}/historial", bomba.getId()))
				.andExpect(jsonPath("$[0].origenId").value(nullValue()))
				.andExpect(jsonPath("$[1].origenId").value(principal.getId()));
	}

	@Test
	void soloDevuelveElHistorialDeEseEquipo() throws Exception {
		alta(bomba, LUNES);
		alta(compresor, MARTES);

		mvc.perform(get("/api/equipos/{id}/historial", bomba.getId()))
				.andExpect(jsonPath("$", hasSize(1)))
				.andExpect(jsonPath("$[0].entidadId").value(bomba.getId()));
	}

	@Test
	void unEquipoSinHistorialDevuelveUnaListaVacia() throws Exception {
		mvc.perform(get("/api/equipos/{id}/historial", bomba.getId()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(0)));
	}

	@Test
	void unEquipoQueNoExisteDevuelve404() throws Exception {
		mvc.perform(get("/api/equipos/{id}/historial", 999999))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.codigo").value("NO_ENCONTRADO"))
				.andExpect(jsonPath("$.detail").value("El equipo 999999 no existe."));
	}

}
