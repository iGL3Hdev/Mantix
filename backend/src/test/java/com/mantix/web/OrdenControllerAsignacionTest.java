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
// Cubre asignar y reasignar: las dos aceptan solo { "tecnicoId": ... }.
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
public class OrdenControllerAsignacionTest {

	private static final Instant LUNES = Instant.parse("2026-10-19T09:00:00Z");
	private static final Instant MARTES = Instant.parse("2026-10-20T09:00:00Z");

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

	private OrdenTrabajo ordenPendiente() {
		return ordenes.save(new OrdenTrabajo(fuga, responsable, null, "Cambiar el sello", MARTES));
	}

	private OrdenTrabajo ordenAsignadaA(Usuario tecnico) {
		return ordenes.save(new OrdenTrabajo(fuga, responsable, tecnico, "Cambiar el sello", MARTES));
	}

	private String cuerpo(Long tecnicoId) {
		return "{\"tecnicoId\": " + tecnicoId + "}";
	}

	private MockHttpServletRequestBuilder asignar(Long ordenId, String json) {
		return post("/api/ordenes/{id}/asignar", ordenId).contentType(MediaType.APPLICATION_JSON).content(json);
	}

	private MockHttpServletRequestBuilder reasignar(Long ordenId, String json) {
		return post("/api/ordenes/{id}/reasignar", ordenId).contentType(MediaType.APPLICATION_JSON).content(json);
	}

	private List<Historial> historialDe(Long ordenId) {
		return historial.findByTipoEntidadAndEntidadIdOrderByFechaAscIdAsc(TipoEntidad.ORDEN, ordenId);
	}

	// ---------- asignar ----------

	@Test
	void asignaLaOrdenYDevuelveLaOrdenActualizada() throws Exception {
		OrdenTrabajo orden = ordenPendiente();

		mvc.perform(asignar(orden.getId(), cuerpo(ana.getId())).header("X-Usuario-Id", responsable.getId()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.*", hasSize(11)))
				.andExpect(jsonPath("$.id").value(orden.getId()))
				.andExpect(jsonPath("$.estado").value("ASIGNADA"))
				.andExpect(jsonPath("$.tecnico.id").value(ana.getId()))
				.andExpect(jsonPath("$.tecnico.nombre").value(ana.getNombre()));
	}

	@Test
	void laAsignacionQuedaGuardadaYRegistradaConElCambioDeTecnicoEncadenado() throws Exception {
		OrdenTrabajo orden = ordenPendiente();

		mvc.perform(asignar(orden.getId(), cuerpo(ana.getId())).header("X-Usuario-Id", responsable.getId()))
				.andExpect(status().isOk());

		mvc.perform(get("/api/ordenes/{id}", orden.getId()))
				.andExpect(jsonPath("$.estado").value("ASIGNADA"))
				.andExpect(jsonPath("$.tecnico.id").value(ana.getId()));

		List<Historial> entradas = historialDe(orden.getId());
		assertThat(entradas).hasSize(2);
		assertThat(entradas.get(0).getAccion()).isEqualTo(AccionHistorial.CAMBIO_ESTADO);
		assertThat(entradas.get(0).getValorAnterior()).isEqualTo("PENDIENTE");
		assertThat(entradas.get(0).getValorNuevo()).isEqualTo("ASIGNADA");
		assertThat(entradas.get(1).getAccion()).isEqualTo(AccionHistorial.CAMBIO_TECNICO);
		assertThat(entradas.get(1).getValorNuevo()).isEqualTo(String.valueOf(ana.getId()));
		assertThat(entradas.get(1).getOrigen().getId()).isEqualTo(entradas.get(0).getId());
	}

	@Test
	void asignarSinCabeceraDeUsuarioDevuelve401() throws Exception {
		mvc.perform(asignar(ordenPendiente().getId(), cuerpo(ana.getId())))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.codigo").value("USUARIO_NO_IDENTIFICADO"));
	}

	@Test
	void unTecnicoNoPuedeAsignarYDevuelve403() throws Exception {
		mvc.perform(asignar(ordenPendiente().getId(), cuerpo(ana.getId())).header("X-Usuario-Id", ana.getId()))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.codigo").value("RN-05"));
	}

	@Test
	void asignarUnaOrdenQueNoExisteDevuelve404() throws Exception {
		mvc.perform(asignar(999999L, cuerpo(ana.getId())).header("X-Usuario-Id", responsable.getId()))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.codigo").value("NO_ENCONTRADO"))
				.andExpect(jsonPath("$.detail").value("La orden 999999 no existe."));
	}

	@Test
	void asignarAUnTecnicoQueNoExisteDevuelve404() throws Exception {
		mvc.perform(asignar(ordenPendiente().getId(), cuerpo(999999L)).header("X-Usuario-Id", responsable.getId()))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.codigo").value("NO_ENCONTRADO"))
				.andExpect(jsonPath("$.detail").value("El usuario 999999 no existe."));
	}

	@Test
	void asignarAUnResponsableDevuelve403() throws Exception {
		mvc.perform(asignar(ordenPendiente().getId(), cuerpo(responsable.getId())).header("X-Usuario-Id",
				responsable.getId()))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.codigo").value("RN-07"));
	}

	@Test
	void asignarAUnTecnicoInactivoDevuelve403() throws Exception {
		mvc.perform(asignar(ordenPendiente().getId(), cuerpo(inactivo.getId())).header("X-Usuario-Id",
				responsable.getId()))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.codigo").value("RN-04"));
	}

	@Test
	void asignarUnaOrdenQueYaEstaAsignadaDevuelve409() throws Exception {
		mvc.perform(asignar(ordenAsignadaA(ana).getId(), cuerpo(pablo.getId())).header("X-Usuario-Id",
				responsable.getId()))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.codigo").value("RN-01"));
	}

	@Test
	void asignarUnaOrdenCanceladaDevuelve409() throws Exception {
		OrdenTrabajo cancelada = new OrdenTrabajo(fuga, responsable, null, "Cancelada", MARTES);
		cancelada.cancelar();
		cancelada = ordenes.save(cancelada);

		mvc.perform(asignar(cancelada.getId(), cuerpo(ana.getId())).header("X-Usuario-Id", responsable.getId()))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.codigo").value("RN-23"));
	}

	@Test
	void asignarSinTecnicoDevuelve400ConElCampo() throws Exception {
		OrdenTrabajo orden = ordenPendiente();

		mvc.perform(asignar(orden.getId(), "{}").header("X-Usuario-Id", responsable.getId()))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.codigo").value("VALIDACION"))
				.andExpect(jsonPath("$.errores[*].campo", contains("tecnicoId")))
				.andExpect(jsonPath("$.errores[0].mensaje").value("El técnico es obligatorio."));

		mvc.perform(get("/api/ordenes/{id}", orden.getId()))
				.andExpect(jsonPath("$.estado").value("PENDIENTE"));
	}

	// ---------- reasignar ----------

	@Test
	void reasignaLaOrdenYDevuelveLaOrdenActualizada() throws Exception {
		OrdenTrabajo orden = ordenAsignadaA(ana);

		mvc.perform(reasignar(orden.getId(), cuerpo(pablo.getId())).header("X-Usuario-Id", responsable.getId()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.*", hasSize(11)))
				.andExpect(jsonPath("$.id").value(orden.getId()))
				.andExpect(jsonPath("$.estado").value("ASIGNADA"))
				.andExpect(jsonPath("$.tecnico.id").value(pablo.getId()))
				.andExpect(jsonPath("$.tecnico.nombre").value(pablo.getNombre()));
	}

	@Test
	void laReasignacionQuedaGuardadaYSoloRegistraElCambioDeTecnico() throws Exception {
		OrdenTrabajo orden = ordenAsignadaA(ana);

		mvc.perform(reasignar(orden.getId(), cuerpo(pablo.getId())).header("X-Usuario-Id", responsable.getId()))
				.andExpect(status().isOk());

		mvc.perform(get("/api/ordenes/{id}", orden.getId()))
				.andExpect(jsonPath("$.tecnico.id").value(pablo.getId()));

		List<Historial> entradas = historialDe(orden.getId());
		assertThat(entradas).hasSize(1);
		assertThat(entradas.get(0).getAccion()).isEqualTo(AccionHistorial.CAMBIO_TECNICO);
		assertThat(entradas.get(0).getValorAnterior()).isEqualTo(String.valueOf(ana.getId()));
		assertThat(entradas.get(0).getValorNuevo()).isEqualTo(String.valueOf(pablo.getId()));
	}

	@Test
	void reasignarSinCabeceraDeUsuarioDevuelve401() throws Exception {
		mvc.perform(reasignar(ordenAsignadaA(ana).getId(), cuerpo(pablo.getId())))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.codigo").value("USUARIO_NO_IDENTIFICADO"));
	}

	@Test
	void unTecnicoNoPuedeReasignarYDevuelve403() throws Exception {
		mvc.perform(reasignar(ordenAsignadaA(ana).getId(), cuerpo(pablo.getId())).header("X-Usuario-Id", ana.getId()))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.codigo").value("RN-05"));
	}

	@Test
	void reasignarUnaOrdenQueNoExisteDevuelve404() throws Exception {
		mvc.perform(reasignar(999999L, cuerpo(pablo.getId())).header("X-Usuario-Id", responsable.getId()))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.codigo").value("NO_ENCONTRADO"))
				.andExpect(jsonPath("$.detail").value("La orden 999999 no existe."));
	}

	@Test
	void reasignarAUnTecnicoQueNoExisteDevuelve404() throws Exception {
		mvc.perform(reasignar(ordenAsignadaA(ana).getId(), cuerpo(999999L)).header("X-Usuario-Id",
				responsable.getId()))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.detail").value("El usuario 999999 no existe."));
	}

	@Test
	void reasignarUnaOrdenPendienteDevuelve409() throws Exception {
		mvc.perform(reasignar(ordenPendiente().getId(), cuerpo(pablo.getId())).header("X-Usuario-Id",
				responsable.getId()))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.codigo").value("RN-24"));
	}

	@Test
	void reasignarAlMismoTecnicoDevuelve409() throws Exception {
		mvc.perform(reasignar(ordenAsignadaA(ana).getId(), cuerpo(ana.getId())).header("X-Usuario-Id",
				responsable.getId()))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.codigo").value("RN-24"));
	}

	@Test
	void reasignarUnaOrdenCerradaDevuelve409() throws Exception {
		OrdenTrabajo cerrada = new OrdenTrabajo(fuga, responsable, ana, "Cerrada", MARTES);
		cerrada.validar();
		cerrada = ordenes.save(cerrada);

		mvc.perform(reasignar(cerrada.getId(), cuerpo(pablo.getId())).header("X-Usuario-Id", responsable.getId()))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.codigo").value("RN-23"));
	}

	@Test
	void reasignarSinTecnicoDevuelve400ConElCampo() throws Exception {
		OrdenTrabajo orden = ordenAsignadaA(ana);

		mvc.perform(reasignar(orden.getId(), "{}").header("X-Usuario-Id", responsable.getId()))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errores[*].campo", contains("tecnicoId")));

		mvc.perform(get("/api/ordenes/{id}", orden.getId()))
				.andExpect(jsonPath("$.tecnico.id").value(ana.getId()));
	}

}
