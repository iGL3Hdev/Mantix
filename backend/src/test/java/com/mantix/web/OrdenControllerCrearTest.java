package com.mantix.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.nullValue;

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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
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
public class OrdenControllerCrearTest {

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
	private Usuario ana;
	private Usuario inactivo;
	private Incidencia fuga;

	@BeforeEach
	void preparar() {
		Ubicacion zonaA = ubicaciones.findAll(Sort.by("nombre")).get(0);
		responsable = usuarios.findAll().stream().filter(u -> u.getPerfil() == Perfil.RESPONSABLE).findFirst().get();
		ana = usuarios.findAll().stream().filter(u -> u.getPerfil() == Perfil.TECNICO && u.isActivo()).findFirst()
				.get();
		inactivo = usuarios.findAll().stream().filter(u -> !u.isActivo()).findFirst().get();
		Equipo bomba = equipos.save(new Equipo("BOM-001", "Bomba de agua", zonaA, LocalDate.of(2026, 10, 18)));
		fuga = incidencias.save(new Incidencia(bomba, ana, "Fuga en el sello", "Gotea.", Prioridad.ALTA, LUNES));
	}

	@AfterEach
	void limpiar() {
		// Las entradas consecuencia apuntan a su origen: se borran primero las más recientes.
		historial.deleteAll(historial.findAll(Sort.by(Sort.Direction.DESC, "id")));
		ordenes.deleteAll();
		incidencias.deleteAll();
		equipos.deleteAll();
	}

	private String cuerpo(Long incidenciaId, Long tecnicoId) {
		return """
				{"incidenciaId": %d, "tecnicoId": %s, "descripcion": "Cambiar el sello mecánico."}
				""".formatted(incidenciaId, tecnicoId);
	}

	private MockHttpServletRequestBuilder crear(String json) {
		return post("/api/ordenes").contentType(MediaType.APPLICATION_JSON).content(json);
	}

	private List<Historial> historialDe(TipoEntidad tipo, Long id) {
		return historial.findByTipoEntidadAndEntidadIdOrderByFechaAscIdAsc(tipo, id);
	}

	@Test
	void creaLaOrdenConTecnicoYDevuelve201() throws Exception {
		mvc.perform(crear(cuerpo(fuga.getId(), ana.getId())).header("X-Usuario-Id", responsable.getId()))
				.andExpect(status().isCreated())
				.andExpect(header().string("Location", matchesPattern(".*/api/ordenes/[0-9]+")))
				.andExpect(jsonPath("$.*", hasSize(11)))
				.andExpect(jsonPath("$.id").isNumber())
				.andExpect(jsonPath("$.descripcion").value("Cambiar el sello mecánico."))
				.andExpect(jsonPath("$.estado").value("ASIGNADA"))
				.andExpect(jsonPath("$.resolucion").value(nullValue()))
				.andExpect(jsonPath("$.incidencia.id").value(fuga.getId()))
				.andExpect(jsonPath("$.incidencia.titulo").value("Fuga en el sello"))
				.andExpect(jsonPath("$.equipo.codigo").value("BOM-001"))
				.andExpect(jsonPath("$.creadaPor.id").value(responsable.getId()))
				.andExpect(jsonPath("$.tecnico.id").value(ana.getId()))
				.andExpect(jsonPath("$.tecnico.nombre").value(ana.getNombre()))
				.andExpect(jsonPath("$.fechaCreacion").isString())
				.andExpect(jsonPath("$.fechaInicio").value(nullValue()))
				.andExpect(jsonPath("$.fechaFin").value(nullValue()));
	}

	@Test
	void sinTecnicoLaOrdenQuedaPendiente() throws Exception {
		mvc.perform(crear(cuerpo(fuga.getId(), null)).header("X-Usuario-Id", responsable.getId()))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.estado").value("PENDIENTE"))
				.andExpect(jsonPath("$.tecnico").value(nullValue()));
	}

	@Test
	void laIncidenciaPasaAEnCursoYApuntaASuNuevaOrden() throws Exception {
		mvc.perform(crear(cuerpo(fuga.getId(), ana.getId())).header("X-Usuario-Id", responsable.getId()))
				.andExpect(status().isCreated());

		Long ordenId = ordenes.findAll().get(0).getId();
		mvc.perform(get("/api/incidencias/{id}", fuga.getId()))
				.andExpect(jsonPath("$.estado").value("EN_CURSO"))
				.andExpect(jsonPath("$.ordenActivaId").value(ordenId))
				.andExpect(jsonPath("$.ordenes", hasSize(1)));
	}

	@Test
	void elHistorialRegistraElAltaElTecnicoYElCambioDeLaIncidenciaEncadenados() throws Exception {
		mvc.perform(crear(cuerpo(fuga.getId(), ana.getId())).header("X-Usuario-Id", responsable.getId()))
				.andExpect(status().isCreated());

		Long ordenId = ordenes.findAll().get(0).getId();
		List<Historial> deLaOrden = historialDe(TipoEntidad.ORDEN, ordenId);
		assertThat(deLaOrden).hasSize(2);
		Historial alta = deLaOrden.get(0);
		assertThat(alta.getAccion()).isEqualTo(AccionHistorial.ALTA);
		assertThat(alta.getValorNuevo()).isEqualTo("ASIGNADA");
		assertThat(deLaOrden.get(1).getAccion()).isEqualTo(AccionHistorial.CAMBIO_TECNICO);
		assertThat(deLaOrden.get(1).getValorNuevo()).isEqualTo(String.valueOf(ana.getId()));

		List<Historial> deLaIncidencia = historialDe(TipoEntidad.INCIDENCIA, fuga.getId());
		assertThat(deLaIncidencia).hasSize(1);
		assertThat(deLaIncidencia.get(0).getValorAnterior()).isEqualTo("ABIERTA");
		assertThat(deLaIncidencia.get(0).getValorNuevo()).isEqualTo("EN_CURSO");
		assertThat(deLaIncidencia.get(0).getOrigen().getId()).isEqualTo(alta.getId());
	}

	@Test
	void sinCabeceraDeUsuarioDevuelve401() throws Exception {
		mvc.perform(crear(cuerpo(fuga.getId(), ana.getId())))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.codigo").value("USUARIO_NO_IDENTIFICADO"));
	}

	@Test
	void unTecnicoNoPuedeCrearOrdenesYDevuelve403() throws Exception {
		mvc.perform(crear(cuerpo(fuga.getId(), ana.getId())).header("X-Usuario-Id", ana.getId()))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.codigo").value("RN-05"));
	}

	@Test
	void unaIncidenciaQueNoExisteDevuelve404() throws Exception {
		mvc.perform(crear(cuerpo(999999L, ana.getId())).header("X-Usuario-Id", responsable.getId()))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.codigo").value("NO_ENCONTRADO"))
				.andExpect(jsonPath("$.detail").value("La incidencia 999999 no existe."));
	}

	@Test
	void unTecnicoQueNoExisteDevuelve404() throws Exception {
		mvc.perform(crear(cuerpo(fuga.getId(), 999999L)).header("X-Usuario-Id", responsable.getId()))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.codigo").value("NO_ENCONTRADO"))
				.andExpect(jsonPath("$.detail").value("El usuario 999999 no existe."));
	}

	@Test
	void asignarLaOrdenAUnResponsableDevuelve403() throws Exception {
		mvc.perform(crear(cuerpo(fuga.getId(), responsable.getId())).header("X-Usuario-Id", responsable.getId()))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.codigo").value("RN-07"));
	}

	@Test
	void asignarLaOrdenAUnTecnicoInactivoDevuelve403() throws Exception {
		mvc.perform(crear(cuerpo(fuga.getId(), inactivo.getId())).header("X-Usuario-Id", responsable.getId()))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.codigo").value("RN-04"));
	}

	@Test
	void unaIncidenciaQueNoEstaAbiertaDevuelve409() throws Exception {
		fuga.ponerEnCurso();
		incidencias.save(fuga);

		mvc.perform(crear(cuerpo(fuga.getId(), ana.getId())).header("X-Usuario-Id", responsable.getId()))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.codigo").value("RN-19"));
	}

	@Test
	void unaIncidenciaAbiertaQueYaTieneUnaOrdenSinCerrarDevuelve409() throws Exception {
		// Estado incoherente a propósito (abierta pero con orden): comprueba RN-20 por separado de RN-19.
		ordenes.save(new OrdenTrabajo(fuga, responsable, ana, "Ya existente", LUNES));

		mvc.perform(crear(cuerpo(fuga.getId(), ana.getId())).header("X-Usuario-Id", responsable.getId()))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.codigo").value("RN-20"));
	}

	@Test
	void sinIncidenciaNiDescripcionDevuelve400ConTodosLosErrores() throws Exception {
		mvc.perform(crear("{}").header("X-Usuario-Id", responsable.getId()))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.codigo").value("VALIDACION"))
				.andExpect(jsonPath("$.errores[*].campo", containsInAnyOrder("incidenciaId", "descripcion")));

		assertThat(ordenes.count()).isZero();
	}

	@Test
	void unaDescripcionDeMasDeCincoMilCaracteresDevuelve400YNoCreaNada() throws Exception {
		String json = """
				{"incidenciaId": %d, "descripcion": "%s"}
				""".formatted(fuga.getId(), "x".repeat(5001));

		mvc.perform(crear(json).header("X-Usuario-Id", responsable.getId()))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errores[*].campo", containsInAnyOrder("descripcion")))
				.andExpect(jsonPath("$.errores[0].mensaje").value("La descripción admite como máximo 5000 caracteres."));

		assertThat(ordenes.count()).isZero();
		mvc.perform(get("/api/incidencias/{id}", fuga.getId()))
				.andExpect(jsonPath("$.estado").value("ABIERTA"));
	}

}
