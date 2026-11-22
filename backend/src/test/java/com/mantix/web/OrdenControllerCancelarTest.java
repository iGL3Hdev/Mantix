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
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
public class OrdenControllerCancelarTest {

	private static final Instant LUNES = Instant.parse("2026-10-19T09:00:00Z");
	private static final Instant MARTES = Instant.parse("2026-10-20T09:00:00Z");
	private static final Instant MIERCOLES = Instant.parse("2026-10-21T09:00:00Z");
	private static final String MOTIVO = "{\"comentario\": \"Ya no hace falta\"}";

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
	private Equipo bomba;
	private Incidencia fuga;

	@BeforeEach
	void preparar() {
		Ubicacion zonaA = ubicaciones.findAll(Sort.by("nombre")).get(0);
		responsable = usuarios.findAll().stream().filter(u -> u.getPerfil() == Perfil.RESPONSABLE).findFirst().get();
		ana = usuarios.findAll().stream().filter(u -> u.getPerfil() == Perfil.TECNICO && u.isActivo()).findFirst()
				.get();
		bomba = equipos.save(new Equipo("BOM-001", "Bomba de agua", zonaA, LocalDate.of(2026, 10, 18)));
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

	private OrdenTrabajo ordenAsignada() {
		return ordenes.save(new OrdenTrabajo(fuga, responsable, ana, "Cambiar el sello", MARTES));
	}

	private OrdenTrabajo ordenEnCurso() {
		OrdenTrabajo orden = new OrdenTrabajo(fuga, responsable, ana, "Cambiar el sello", MARTES);
		orden.empezar(MARTES);
		return ordenes.save(orden);
	}

	private OrdenTrabajo ordenResuelta() {
		OrdenTrabajo orden = new OrdenTrabajo(fuga, responsable, ana, "Cambiar el sello", MARTES);
		orden.empezar(MARTES);
		orden.resolver("Sello cambiado.", MIERCOLES);
		return ordenes.save(orden);
	}

	private MockHttpServletRequestBuilder cancelar(Long ordenId, String json) {
		return post("/api/ordenes/{id}/cancelar", ordenId).contentType(MediaType.APPLICATION_JSON).content(json);
	}

	private List<Historial> historialDe(TipoEntidad tipo, Long id) {
		return historial.findByTipoEntidadAndEntidadIdOrderByFechaAscIdAsc(tipo, id);
	}

	@Test
	void cancelaUnaOrdenAsignadaYDevuelveLaOrdenActualizada() throws Exception {
		OrdenTrabajo orden = ordenAsignada();

		mvc.perform(cancelar(orden.getId(), MOTIVO).header("X-Usuario-Id", responsable.getId()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.*", hasSize(11)))
				.andExpect(jsonPath("$.id").value(orden.getId()))
				.andExpect(jsonPath("$.estado").value("CANCELADA"));
	}

	@Test
	void tambienSePuedeCancelarUnaOrdenPendiente() throws Exception {
		mvc.perform(cancelar(ordenPendiente().getId(), MOTIVO).header("X-Usuario-Id", responsable.getId()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.estado").value("CANCELADA"))
				.andExpect(jsonPath("$.tecnico").value(nullValue()));
	}

	@Test
	void tambienSePuedeCancelarUnaOrdenEnCurso() throws Exception {
		mvc.perform(cancelar(ordenEnCurso().getId(), MOTIVO).header("X-Usuario-Id", responsable.getId()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.estado").value("CANCELADA"));
	}

	@Test
	void laCancelacionQuedaRegistradaConSuMotivoYEncadenadaALaIncidencia() throws Exception {
		OrdenTrabajo orden = ordenAsignada();

		mvc.perform(cancelar(orden.getId(), MOTIVO).header("X-Usuario-Id", responsable.getId()))
				.andExpect(status().isOk());

		List<Historial> deLaOrden = historialDe(TipoEntidad.ORDEN, orden.getId());
		assertThat(deLaOrden).hasSize(1);
		assertThat(deLaOrden.get(0).getAccion()).isEqualTo(AccionHistorial.CAMBIO_ESTADO);
		assertThat(deLaOrden.get(0).getValorAnterior()).isEqualTo("ASIGNADA");
		assertThat(deLaOrden.get(0).getValorNuevo()).isEqualTo("CANCELADA");
		assertThat(deLaOrden.get(0).getComentario()).isEqualTo("Ya no hace falta");

		List<Historial> deLaIncidencia = historialDe(TipoEntidad.INCIDENCIA, fuga.getId());
		assertThat(deLaIncidencia).hasSize(1);
		assertThat(deLaIncidencia.get(0).getValorAnterior()).isEqualTo("EN_CURSO");
		assertThat(deLaIncidencia.get(0).getValorNuevo()).isEqualTo("ABIERTA");
		assertThat(deLaIncidencia.get(0).getOrigen().getId()).isEqualTo(deLaOrden.get(0).getId());
	}

	@Test
	void laIncidenciaVuelveAAbiertaYYaNoTieneOrdenActiva() throws Exception {
		OrdenTrabajo orden = ordenAsignada();

		mvc.perform(cancelar(orden.getId(), MOTIVO).header("X-Usuario-Id", responsable.getId()))
				.andExpect(status().isOk());

		mvc.perform(get("/api/incidencias/{id}", fuga.getId()))
				.andExpect(jsonPath("$.estado").value("ABIERTA"))
				.andExpect(jsonPath("$.ordenActivaId").value(nullValue()))
				.andExpect(jsonPath("$.ordenes", hasSize(1)))
				.andExpect(jsonPath("$.ordenes[0].estado").value("CANCELADA"));
	}

	@Test
	void trasCancelarSePuedeCrearUnaOrdenNuevaParaLaMismaIncidencia() throws Exception {
		OrdenTrabajo orden = ordenAsignada();
		mvc.perform(cancelar(orden.getId(), MOTIVO).header("X-Usuario-Id", responsable.getId()))
				.andExpect(status().isOk());

		String nueva = "{\"incidenciaId\": " + fuga.getId() + ", \"descripcion\": \"Segundo intento\"}";
		mvc.perform(post("/api/ordenes").contentType(MediaType.APPLICATION_JSON).content(nueva)
				.header("X-Usuario-Id", responsable.getId()))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.estado").value("PENDIENTE"));

		mvc.perform(get("/api/incidencias/{id}", fuga.getId()))
				.andExpect(jsonPath("$.estado").value("EN_CURSO"))
				.andExpect(jsonPath("$.ordenes", hasSize(2)));
	}

	@Test
	void cancelarUnaOrdenNoCambiaElEstadoDelEquipo() throws Exception {
		fuga.cambiarPrioridad(Prioridad.CRITICA);
		incidencias.save(fuga);
		bomba.marcarAveriado();
		equipos.save(bomba);
		OrdenTrabajo orden = ordenAsignada();

		mvc.perform(cancelar(orden.getId(), MOTIVO).header("X-Usuario-Id", responsable.getId()))
				.andExpect(status().isOk());

		mvc.perform(get("/api/equipos/{id}", bomba.getId()))
				.andExpect(jsonPath("$.estado").value("AVERIADO"));
	}

	@Test
	void sinCabeceraDeUsuarioDevuelve401() throws Exception {
		mvc.perform(cancelar(ordenAsignada().getId(), MOTIVO))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.codigo").value("USUARIO_NO_IDENTIFICADO"));
	}

	@Test
	void unTecnicoNoPuedeCancelarYDevuelve403() throws Exception {
		mvc.perform(cancelar(ordenAsignada().getId(), MOTIVO).header("X-Usuario-Id", ana.getId()))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.codigo").value("RN-05"));
	}

	@Test
	void unaOrdenQueNoExisteDevuelve404() throws Exception {
		mvc.perform(cancelar(999999L, MOTIVO).header("X-Usuario-Id", responsable.getId()))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.codigo").value("NO_ENCONTRADO"))
				.andExpect(jsonPath("$.detail").value("La orden 999999 no existe."));
	}

	@Test
	void unaOrdenResueltaNoSeCancelaYDevuelve409() throws Exception {
		mvc.perform(cancelar(ordenResuelta().getId(), MOTIVO).header("X-Usuario-Id", responsable.getId()))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.codigo").value("RN-01"));
	}

	@Test
	void unaOrdenCerradaDevuelve409() throws Exception {
		OrdenTrabajo cerrada = new OrdenTrabajo(fuga, responsable, ana, "Cerrada", MARTES);
		cerrada.validar();
		cerrada = ordenes.save(cerrada);

		mvc.perform(cancelar(cerrada.getId(), MOTIVO).header("X-Usuario-Id", responsable.getId()))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.codigo").value("RN-23"));
	}

	@Test
	void unaOrdenYaCanceladaDevuelve409() throws Exception {
		OrdenTrabajo orden = ordenAsignada();
		mvc.perform(cancelar(orden.getId(), MOTIVO).header("X-Usuario-Id", responsable.getId()))
				.andExpect(status().isOk());

		mvc.perform(cancelar(orden.getId(), MOTIVO).header("X-Usuario-Id", responsable.getId()))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.codigo").value("RN-23"));
	}

	@Test
	void sinMotivoDevuelve400ConElCampo() throws Exception {
		mvc.perform(cancelar(ordenAsignada().getId(), "{}").header("X-Usuario-Id", responsable.getId()))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.codigo").value("VALIDACION"))
				.andExpect(jsonPath("$.errores[*].campo", contains("comentario")))
				.andExpect(jsonPath("$.errores[0].mensaje").value("El comentario es obligatorio."));
	}

	@Test
	void unMotivoEnBlancoDevuelve400YNoCancelaLaOrden() throws Exception {
		OrdenTrabajo orden = ordenAsignada();

		mvc.perform(cancelar(orden.getId(), "{\"comentario\": \"   \"}").header("X-Usuario-Id",
				responsable.getId()))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errores[*].campo", contains("comentario")));

		mvc.perform(get("/api/ordenes/{id}", orden.getId()))
				.andExpect(jsonPath("$.estado").value("ASIGNADA"));
		mvc.perform(get("/api/incidencias/{id}", fuga.getId()))
				.andExpect(jsonPath("$.estado").value("EN_CURSO"));
	}

	@Test
	void unMotivoDeMasDeQuinientosCaracteresDevuelve400() throws Exception {
		String json = "{\"comentario\": \"" + "x".repeat(501) + "\"}";

		mvc.perform(cancelar(ordenAsignada().getId(), json).header("X-Usuario-Id", responsable.getId()))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errores[*].campo", contains("comentario")))
				.andExpect(jsonPath("$.errores[0].mensaje").value("El comentario admite como máximo 500 caracteres."));
	}

}