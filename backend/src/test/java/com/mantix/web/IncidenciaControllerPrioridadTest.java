package com.mantix.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;

import java.time.Instant;
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
import com.mantix.domain.Incidencia;
import com.mantix.domain.OrdenTrabajo;
import com.mantix.domain.Perfil;
import com.mantix.domain.Prioridad;
import com.mantix.domain.TipoEntidad;
import com.mantix.domain.Ubicacion;
import com.mantix.domain.Usuario;
import com.mantix.repository.EquipoRepository;
import com.mantix.repository.HistorialRepository;
import com.mantix.repository.IncidenciaRepository;
import com.mantix.repository.OrdenTrabajoRepository;
import com.mantix.repository.UbicacionRepository;
import com.mantix.repository.UsuarioRepository;

// Sin @Transactional (igual que los demás tests de controladores): los datos se borran en @AfterEach.
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
public class IncidenciaControllerPrioridadTest {

	private static final Instant LUNES = Instant.parse("2026-10-19T09:00:00Z");

	@Autowired
	private MockMvc mvc;

	@Autowired
	private EquipoRepository equipos;

	@Autowired
	private IncidenciaRepository incidencias;

	@Autowired
	private OrdenTrabajoRepository ordenes;

	@Autowired
	private HistorialRepository historial;

	@Autowired
	private UbicacionRepository ubicaciones;

	@Autowired
	private UsuarioRepository usuarios;

	private Usuario responsable;
	private Usuario tecnico;
	private Equipo bomba;
	private Incidencia fuga;

	@BeforeEach
	void preparar() {
		Ubicacion zonaA = ubicaciones.findAll(Sort.by("nombre")).get(0);
		responsable = usuarios.findAll().stream().filter(u -> u.getPerfil() == Perfil.RESPONSABLE).findFirst().get();
		tecnico = usuarios.findAll().stream().filter(u -> u.getPerfil() == Perfil.TECNICO && u.isActivo()).findFirst()
				.get();
		bomba = equipos.save(new Equipo("BOM-001", "Bomba de agua", zonaA, LocalDate.of(2026, 10, 18)));
		fuga = incidencias.save(new Incidencia(bomba, tecnico, "Fuga en el sello", "Gotea agua por el eje.",
				Prioridad.MEDIA, LUNES));
	}

	@AfterEach
	void limpiar() {
		// Las entradas consecuencia apuntan a su origen: se borran primero las más recientes.
		historial.deleteAll(historial.findAll(Sort.by(Sort.Direction.DESC, "id")));
		ordenes.deleteAll();
		incidencias.deleteAll();
		equipos.deleteAll();
	}

	private String cuerpo(String prioridad) {
		return "{\"prioridad\": \"" + prioridad + "\", \"comentario\": \"Sube por riesgo de paro\"}";
	}

	private MockHttpServletRequestBuilder cambiar(Long incidenciaId, String json) {
		return post("/api/incidencias/{id}/prioridad", incidenciaId).contentType(MediaType.APPLICATION_JSON)
				.content(json);
	}

	private List<Historial> historialDe(TipoEntidad tipo, Long id) {
		return historial.findByTipoEntidadAndEntidadIdOrderByFechaAscIdAsc(tipo, id);
	}

	@Test
	void cambiaLaPrioridadYDevuelveLaIncidenciaActualizada() throws Exception {
		mvc.perform(cambiar(fuga.getId(), cuerpo("ALTA")).header("X-Usuario-Id", responsable.getId()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.*", hasSize(10)))
				.andExpect(jsonPath("$.id").value(fuga.getId()))
				.andExpect(jsonPath("$.prioridad").value("ALTA"))
				.andExpect(jsonPath("$.estado").value("ABIERTA"))
				.andExpect(jsonPath("$.equipo.codigo").value("BOM-001"));
	}

	@Test
	void elCambioQuedaGuardadoYRegistradoEnElHistorialConSuComentario() throws Exception {
		mvc.perform(cambiar(fuga.getId(), cuerpo("ALTA")).header("X-Usuario-Id", responsable.getId()))
				.andExpect(status().isOk());

		mvc.perform(get("/api/incidencias/{id}", fuga.getId()))
				.andExpect(jsonPath("$.prioridad").value("ALTA"));

		List<Historial> entradas = historialDe(TipoEntidad.INCIDENCIA, fuga.getId());
		assertThat(entradas).hasSize(1);
		assertThat(entradas.get(0).getAccion()).isEqualTo(AccionHistorial.CAMBIO_PRIORIDAD);
		assertThat(entradas.get(0).getValorAnterior()).isEqualTo("MEDIA");
		assertThat(entradas.get(0).getValorNuevo()).isEqualTo("ALTA");
		assertThat(entradas.get(0).getComentario()).isEqualTo("Sube por riesgo de paro");
	}

	@Test
	void elComentarioEsOpcional() throws Exception {
		mvc.perform(cambiar(fuga.getId(), "{\"prioridad\": \"ALTA\"}").header("X-Usuario-Id", responsable.getId()))
				.andExpect(status().isOk());

		assertThat(historialDe(TipoEntidad.INCIDENCIA, fuga.getId()).get(0).getComentario()).isNull();
	}

	@Test
	void subirALaCriticaAveriaElEquipoYSuCambioApuntaAlCambioDePrioridad() throws Exception {
		mvc.perform(cambiar(fuga.getId(), cuerpo("CRITICA")).header("X-Usuario-Id", responsable.getId()))
				.andExpect(status().isOk());

		mvc.perform(get("/api/equipos/{id}", bomba.getId()))
				.andExpect(jsonPath("$.estado").value("AVERIADO"));

		Long cambioId = historialDe(TipoEntidad.INCIDENCIA, fuga.getId()).get(0).getId();
		mvc.perform(get("/api/equipos/{id}/historial", bomba.getId()))
				.andExpect(jsonPath("$", hasSize(1)))
				.andExpect(jsonPath("$[0].valorNuevo").value("AVERIADO"))
				.andExpect(jsonPath("$[0].origenId").value(cambioId));
	}

	@Test
	void bajarLaUnicaCriticaDevuelveElEquipoAOperativo() throws Exception {
		fuga.cambiarPrioridad(Prioridad.CRITICA);
		incidencias.save(fuga);
		bomba.marcarAveriado();
		equipos.save(bomba);

		mvc.perform(cambiar(fuga.getId(), cuerpo("ALTA")).header("X-Usuario-Id", responsable.getId()))
				.andExpect(status().isOk());

		mvc.perform(get("/api/equipos/{id}", bomba.getId()))
				.andExpect(jsonPath("$.estado").value("OPERATIVO"));
	}

	@Test
	void conLaMismaPrioridadNoCambiaNadaNiEscribeHistorial() throws Exception {
		mvc.perform(cambiar(fuga.getId(), cuerpo("MEDIA")).header("X-Usuario-Id", responsable.getId()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.prioridad").value("MEDIA"));

		assertThat(historialDe(TipoEntidad.INCIDENCIA, fuga.getId())).isEmpty();
	}

	@Test
	void laRespuestaIncluyeLaOrdenActivaDeLaIncidencia() throws Exception {
		OrdenTrabajo orden = ordenes.save(new OrdenTrabajo(fuga, responsable, tecnico, "Cambiar el sello", LUNES));

		mvc.perform(cambiar(fuga.getId(), cuerpo("ALTA")).header("X-Usuario-Id", responsable.getId()))
				.andExpect(jsonPath("$.ordenActivaId").value(orden.getId()));
	}

	@Test
	void sinCabeceraDeUsuarioDevuelve401() throws Exception {
		mvc.perform(cambiar(fuga.getId(), cuerpo("ALTA")))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.codigo").value("USUARIO_NO_IDENTIFICADO"));
	}

	@Test
	void unTecnicoNoPuedeCambiarLaPrioridadYDevuelve403() throws Exception {
		mvc.perform(cambiar(fuga.getId(), cuerpo("ALTA")).header("X-Usuario-Id", tecnico.getId()))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.codigo").value("RN-05"));
	}

	@Test
	void unaIncidenciaQueNoExisteDevuelve404() throws Exception {
		mvc.perform(cambiar(999999L, cuerpo("ALTA")).header("X-Usuario-Id", responsable.getId()))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.codigo").value("NO_ENCONTRADO"))
				.andExpect(jsonPath("$.detail").value("La incidencia 999999 no existe."));
	}

	@Test
	void unaIncidenciaDescartadaDevuelve409() throws Exception {
		fuga.descartar();
		incidencias.save(fuga);

		mvc.perform(cambiar(fuga.getId(), cuerpo("ALTA")).header("X-Usuario-Id", responsable.getId()))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.codigo").value("RN-15"));
	}

	@Test
	void sinPrioridadDevuelve400ConElCampo() throws Exception {
		mvc.perform(cambiar(fuga.getId(), "{}").header("X-Usuario-Id", responsable.getId()))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.codigo").value("VALIDACION"))
				.andExpect(jsonPath("$.errores[*].campo", contains("prioridad")))
				.andExpect(jsonPath("$.errores[0].mensaje").value("La prioridad es obligatoria."));
	}

	@Test
	void unaPrioridadQueNoExisteDevuelve400YNoCambiaNada() throws Exception {
		mvc.perform(cambiar(fuga.getId(), cuerpo("URGENTE")).header("X-Usuario-Id", responsable.getId()))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.codigo").value("VALIDACION"));

		mvc.perform(get("/api/incidencias/{id}", fuga.getId()))
				.andExpect(jsonPath("$.prioridad").value("MEDIA"));
	}

}
