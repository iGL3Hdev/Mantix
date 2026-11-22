package com.mantix.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
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
// Cubre empezar y resolver: las dos las hace el técnico asignado a la orden (RN-06).
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
public class OrdenControllerEjecucionTest {

	private static final Instant LUNES = Instant.parse("2026-10-19T09:00:00Z");
	private static final Instant MARTES = Instant.parse("2026-10-20T09:00:00Z");
	private static final String RESOLUCION = "{\"resolucion\": \"Sello cambiado y probado.\"}";

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
	private Usuario pablo;
	private Usuario inactivo;
	private Incidencia fuga;

	@BeforeEach
	void preparar() {
		Ubicacion zonaA = ubicaciones.findAll(Sort.by("nombre")).get(0);
		responsable = usuarios.findAll().stream().filter(u -> u.getPerfil() == Perfil.RESPONSABLE).findFirst().get();
		List<Usuario> tecnicos = usuarios.findAll(Sort.by("nombre")).stream()
				.filter(u -> u.getPerfil() == Perfil.TECNICO && u.isActivo()).toList();
		ana = tecnicos.get(0);
		pablo = tecnicos.get(1);
		inactivo = usuarios.findAll().stream().filter(u -> !u.isActivo()).findFirst().get();
		Equipo bomba = equipos.save(new Equipo("BOM-001", "Bomba de agua", zonaA, LocalDate.of(2026, 10, 18)));
		fuga = new Incidencia(bomba, ana, "Fuga en el sello", "Gotea.", Prioridad.ALTA, LUNES);
		fuga.ponerEnCurso();
		fuga = incidencias.save(fuga);
	}

	@AfterEach
	void limpiar() {
		// Las entradas consecuencia apuntan a su origen: se borran primero las más recientes.
		historial.deleteAll(historial.findAll(Sort.by(Sort.Direction.DESC, "id")));
		ordenes.deleteAll();
		incidencias.deleteAll();
		equipos.deleteAll();
	}

	private OrdenTrabajo ordenAsignadaA(Usuario tecnico) {
		return ordenes.save(new OrdenTrabajo(fuga, responsable, tecnico, "Cambiar el sello", MARTES));
	}

	private OrdenTrabajo ordenEnCursoDe(Usuario tecnico) {
		OrdenTrabajo orden = new OrdenTrabajo(fuga, responsable, tecnico, "Cambiar el sello", MARTES);
		orden.empezar(MARTES);
		return ordenes.save(orden);
	}

	private MockHttpServletRequestBuilder empezar(Long ordenId) {
		return post("/api/ordenes/{id}/empezar", ordenId);
	}

	private MockHttpServletRequestBuilder resolver(Long ordenId, String json) {
		return post("/api/ordenes/{id}/resolver", ordenId).contentType(MediaType.APPLICATION_JSON).content(json);
	}

	private List<Historial> historialDe(Long ordenId) {
		return historial.findByTipoEntidadAndEntidadIdOrderByFechaAscIdAsc(TipoEntidad.ORDEN, ordenId);
	}

	// ---------- empezar ----------

	@Test
	void empiezaLaOrdenYRellenaLaFechaDeInicio() throws Exception {
		OrdenTrabajo orden = ordenAsignadaA(ana);

		mvc.perform(empezar(orden.getId()).header("X-Usuario-Id", ana.getId()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.*", hasSize(11)))
				.andExpect(jsonPath("$.id").value(orden.getId()))
				.andExpect(jsonPath("$.estado").value("EN_CURSO"))
				.andExpect(jsonPath("$.fechaInicio").isString())
				.andExpect(jsonPath("$.fechaFin").value(nullValue()));
	}

	@Test
	void tambienAceptaUnCuerpoVacio() throws Exception {
		OrdenTrabajo orden = ordenAsignadaA(ana);

		mvc.perform(empezar(orden.getId()).contentType(MediaType.APPLICATION_JSON).content("{}")
				.header("X-Usuario-Id", ana.getId()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.estado").value("EN_CURSO"));
	}

	@Test
	void elInicioQuedaGuardadoYRegistradoEnElHistorial() throws Exception {
		OrdenTrabajo orden = ordenAsignadaA(ana);

		mvc.perform(empezar(orden.getId()).header("X-Usuario-Id", ana.getId()))
				.andExpect(status().isOk());

		mvc.perform(get("/api/ordenes/{id}", orden.getId()))
				.andExpect(jsonPath("$.estado").value("EN_CURSO"))
				.andExpect(jsonPath("$.fechaInicio").isString());

		List<Historial> entradas = historialDe(orden.getId());
		assertThat(entradas).hasSize(1);
		assertThat(entradas.get(0).getAccion()).isEqualTo(AccionHistorial.CAMBIO_ESTADO);
		assertThat(entradas.get(0).getValorAnterior()).isEqualTo("ASIGNADA");
		assertThat(entradas.get(0).getValorNuevo()).isEqualTo("EN_CURSO");
		assertThat(entradas.get(0).getUsuario().getId()).isEqualTo(ana.getId());
	}

	@Test
	void empezarSinCabeceraDeUsuarioDevuelve401() throws Exception {
		mvc.perform(empezar(ordenAsignadaA(ana).getId()))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.codigo").value("USUARIO_NO_IDENTIFICADO"));
	}

	@Test
	void empezarUnaOrdenQueNoExisteDevuelve404() throws Exception {
		mvc.perform(empezar(999999L).header("X-Usuario-Id", ana.getId()))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.codigo").value("NO_ENCONTRADO"))
				.andExpect(jsonPath("$.detail").value("La orden 999999 no existe."));
	}

	@Test
	void otroTecnicoNoPuedeEmpezarLaOrdenYDevuelve403() throws Exception {
		mvc.perform(empezar(ordenAsignadaA(ana).getId()).header("X-Usuario-Id", pablo.getId()))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.codigo").value("RN-06"));
	}

	@Test
	void elResponsableTampocoPuedeEmpezarLaOrdenYDevuelve403() throws Exception {
		mvc.perform(empezar(ordenAsignadaA(ana).getId()).header("X-Usuario-Id", responsable.getId()))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.codigo").value("RN-06"));
	}

	@Test
	void unUsuarioInactivoDevuelve403() throws Exception {
		mvc.perform(empezar(ordenAsignadaA(ana).getId()).header("X-Usuario-Id", inactivo.getId()))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.codigo").value("RN-04"));
	}

	@Test
	void empezarUnaOrdenYaEnCursoDevuelve409() throws Exception {
		mvc.perform(empezar(ordenEnCursoDe(ana).getId()).header("X-Usuario-Id", ana.getId()))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.codigo").value("RN-01"));
	}

	@Test
	void empezarUnaOrdenCanceladaDevuelve409() throws Exception {
		OrdenTrabajo cancelada = new OrdenTrabajo(fuga, responsable, ana, "Cancelada", MARTES);
		cancelada.cancelar();
		cancelada = ordenes.save(cancelada);

		mvc.perform(empezar(cancelada.getId()).header("X-Usuario-Id", ana.getId()))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.codigo").value("RN-23"));
	}

	// ---------- resolver ----------

	@Test
	void resuelveLaOrdenConSuResolucionYRellenaLaFechaDeFin() throws Exception {
		OrdenTrabajo orden = ordenEnCursoDe(ana);

		mvc.perform(resolver(orden.getId(), RESOLUCION).header("X-Usuario-Id", ana.getId()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.*", hasSize(11)))
				.andExpect(jsonPath("$.id").value(orden.getId()))
				.andExpect(jsonPath("$.estado").value("RESUELTA"))
				.andExpect(jsonPath("$.resolucion").value("Sello cambiado y probado."))
				.andExpect(jsonPath("$.fechaInicio").value("2026-10-20T09:00:00Z"))
				.andExpect(jsonPath("$.fechaFin").isString());
	}

	@Test
	void laResolucionQuedaGuardadaYSuTextoVaAlComentarioDelHistorial() throws Exception {
		OrdenTrabajo orden = ordenEnCursoDe(ana);

		mvc.perform(resolver(orden.getId(), RESOLUCION).header("X-Usuario-Id", ana.getId()))
				.andExpect(status().isOk());

		mvc.perform(get("/api/ordenes/{id}", orden.getId()))
				.andExpect(jsonPath("$.estado").value("RESUELTA"))
				.andExpect(jsonPath("$.resolucion").value("Sello cambiado y probado."));

		List<Historial> entradas = historialDe(orden.getId());
		assertThat(entradas).hasSize(1);
		assertThat(entradas.get(0).getAccion()).isEqualTo(AccionHistorial.CAMBIO_ESTADO);
		assertThat(entradas.get(0).getValorAnterior()).isEqualTo("EN_CURSO");
		assertThat(entradas.get(0).getValorNuevo()).isEqualTo("RESUELTA");
		assertThat(entradas.get(0).getComentario()).isEqualTo("Sello cambiado y probado.");
	}

	@Test
	void resolverNoCambiaElEstadoDeLaIncidencia() throws Exception {
		OrdenTrabajo orden = ordenEnCursoDe(ana);

		mvc.perform(resolver(orden.getId(), RESOLUCION).header("X-Usuario-Id", ana.getId()))
				.andExpect(status().isOk());

		mvc.perform(get("/api/incidencias/{id}", fuga.getId()))
				.andExpect(jsonPath("$.estado").value("EN_CURSO"))
				.andExpect(jsonPath("$.ordenActivaId").value(orden.getId()));
	}

	@Test
	void resolverSinCabeceraDeUsuarioDevuelve401() throws Exception {
		mvc.perform(resolver(ordenEnCursoDe(ana).getId(), RESOLUCION))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.codigo").value("USUARIO_NO_IDENTIFICADO"));
	}

	@Test
	void resolverUnaOrdenQueNoExisteDevuelve404() throws Exception {
		mvc.perform(resolver(999999L, RESOLUCION).header("X-Usuario-Id", ana.getId()))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.detail").value("La orden 999999 no existe."));
	}

	@Test
	void otroTecnicoNoPuedeResolverLaOrdenYDevuelve403() throws Exception {
		mvc.perform(resolver(ordenEnCursoDe(ana).getId(), RESOLUCION).header("X-Usuario-Id", pablo.getId()))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.codigo").value("RN-06"));
	}

	@Test
	void resolverUnaOrdenQueNoEstaEnCursoDevuelve409() throws Exception {
		mvc.perform(resolver(ordenAsignadaA(ana).getId(), RESOLUCION).header("X-Usuario-Id", ana.getId()))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.codigo").value("RN-01"));
	}

	@Test
	void resolverUnaOrdenCerradaDevuelve409() throws Exception {
		OrdenTrabajo cerrada = new OrdenTrabajo(fuga, responsable, ana, "Cerrada", MARTES);
		cerrada.validar();
		cerrada = ordenes.save(cerrada);

		mvc.perform(resolver(cerrada.getId(), RESOLUCION).header("X-Usuario-Id", ana.getId()))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.codigo").value("RN-23"));
	}

	@Test
	void sinResolucionDevuelve400ConElCampo() throws Exception {
		OrdenTrabajo orden = ordenEnCursoDe(ana);

		mvc.perform(resolver(orden.getId(), "{}").header("X-Usuario-Id", ana.getId()))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.codigo").value("VALIDACION"))
				.andExpect(jsonPath("$.errores[*].campo", contains("resolucion")))
				.andExpect(jsonPath("$.errores[0].mensaje").value("La resolución es obligatoria."));
	}

	@Test
	void unaResolucionEnBlancoDevuelve400YNoResuelveLaOrden() throws Exception {
		OrdenTrabajo orden = ordenEnCursoDe(ana);

		mvc.perform(resolver(orden.getId(), "{\"resolucion\": \"   \"}").header("X-Usuario-Id", ana.getId()))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errores[*].campo", contains("resolucion")));

		mvc.perform(get("/api/ordenes/{id}", orden.getId()))
				.andExpect(jsonPath("$.estado").value("EN_CURSO"))
				.andExpect(jsonPath("$.resolucion").value(nullValue()));
	}

	@Test
	void unaResolucionDeMasDeCincoMilCaracteresDevuelve400() throws Exception {
		String json = "{\"resolucion\": \"" + "x".repeat(5001) + "\"}";

		mvc.perform(resolver(ordenEnCursoDe(ana).getId(), json).header("X-Usuario-Id", ana.getId()))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errores[*].campo", contains("resolucion")))
				.andExpect(jsonPath("$.errores[0].mensaje").value("La resolución admite como máximo 5000 caracteres."));
	}

}
