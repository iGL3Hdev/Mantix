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
// Cubre validar y rechazar: las dos las hace el responsable sobre una orden RESUELTA.
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
public class OrdenControllerCierreTest {

	private static final Instant LUNES = Instant.parse("2026-10-19T09:00:00Z");
	private static final Instant MARTES = Instant.parse("2026-10-20T09:00:00Z");
	private static final Instant MIERCOLES = Instant.parse("2026-10-21T09:00:00Z");
	private static final String MOTIVO = "{\"comentario\": \"El sello sigue goteando\"}";

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

	private OrdenTrabajo ordenEnCurso() {
		OrdenTrabajo orden = new OrdenTrabajo(fuga, responsable, ana, "Cambiar el sello", MARTES);
		orden.empezar(MARTES);
		return ordenes.save(orden);
	}

	private OrdenTrabajo ordenResuelta() {
		OrdenTrabajo orden = new OrdenTrabajo(fuga, responsable, ana, "Cambiar el sello", MARTES);
		orden.empezar(MARTES);
		orden.resolver("Sello cambiado y probado.", MIERCOLES);
		return ordenes.save(orden);
	}

	// Deja la fuga como crítica y la bomba averiada: el estado coherente que dejaría la API.
	private void hacerCriticaLaFuga() {
		fuga.cambiarPrioridad(Prioridad.CRITICA);
		incidencias.save(fuga);
		bomba.marcarAveriado();
		equipos.save(bomba);
	}

	private MockHttpServletRequestBuilder validar(Long ordenId) {
		return post("/api/ordenes/{id}/validar", ordenId);
	}

	private MockHttpServletRequestBuilder rechazar(Long ordenId, String json) {
		return post("/api/ordenes/{id}/rechazar", ordenId).contentType(MediaType.APPLICATION_JSON).content(json);
	}

	private MockHttpServletRequestBuilder resolver(Long ordenId, String resolucion) {
		return post("/api/ordenes/{id}/resolver", ordenId).contentType(MediaType.APPLICATION_JSON)
				.content("{\"resolucion\": \"" + resolucion + "\"}");
	}

	private List<Historial> historialDe(TipoEntidad tipo, Long id) {
		return historial.findByTipoEntidadAndEntidadIdOrderByFechaAscIdAsc(tipo, id);
	}

	// ---------- validar ----------

	@Test
	void validaLaOrdenYLaDejaCerrada() throws Exception {
		OrdenTrabajo orden = ordenResuelta();

		mvc.perform(validar(orden.getId()).header("X-Usuario-Id", responsable.getId()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.*", hasSize(11)))
				.andExpect(jsonPath("$.id").value(orden.getId()))
				.andExpect(jsonPath("$.estado").value("CERRADA"))
				.andExpect(jsonPath("$.resolucion").value("Sello cambiado y probado."))
				.andExpect(jsonPath("$.fechaFin").value("2026-10-21T09:00:00Z"));
	}

	@Test
	void validarCierraLaIncidenciaConSuFechaYEncadenaElHistorial() throws Exception {
		OrdenTrabajo orden = ordenResuelta();

		mvc.perform(validar(orden.getId()).header("X-Usuario-Id", responsable.getId()))
				.andExpect(status().isOk());

		mvc.perform(get("/api/incidencias/{id}", fuga.getId()))
				.andExpect(jsonPath("$.estado").value("CERRADA"))
				.andExpect(jsonPath("$.fechaCierre").isString())
				.andExpect(jsonPath("$.ordenActivaId").value(nullValue()));

		List<Historial> deLaOrden = historialDe(TipoEntidad.ORDEN, orden.getId());
		assertThat(deLaOrden).hasSize(1);
		assertThat(deLaOrden.get(0).getValorAnterior()).isEqualTo("RESUELTA");
		assertThat(deLaOrden.get(0).getValorNuevo()).isEqualTo("CERRADA");

		List<Historial> deLaIncidencia = historialDe(TipoEntidad.INCIDENCIA, fuga.getId());
		assertThat(deLaIncidencia).hasSize(1);
		assertThat(deLaIncidencia.get(0).getValorAnterior()).isEqualTo("EN_CURSO");
		assertThat(deLaIncidencia.get(0).getValorNuevo()).isEqualTo("CERRADA");
		assertThat(deLaIncidencia.get(0).getOrigen().getId()).isEqualTo(deLaOrden.get(0).getId());
	}

	@Test
	void validarLaUnicaCriticaReponeElEquipoYSuCambioApuntaALaValidacion() throws Exception {
		hacerCriticaLaFuga();
		OrdenTrabajo orden = ordenResuelta();

		mvc.perform(validar(orden.getId()).header("X-Usuario-Id", responsable.getId()))
				.andExpect(status().isOk());

		mvc.perform(get("/api/equipos/{id}", bomba.getId()))
				.andExpect(jsonPath("$.estado").value("OPERATIVO"));

		Long validacionId = historialDe(TipoEntidad.ORDEN, orden.getId()).get(0).getId();
		mvc.perform(get("/api/equipos/{id}/historial", bomba.getId()))
				.andExpect(jsonPath("$", hasSize(1)))
				.andExpect(jsonPath("$[0].valorAnterior").value("AVERIADO"))
				.andExpect(jsonPath("$[0].valorNuevo").value("OPERATIVO"))
				.andExpect(jsonPath("$[0].origenId").value(validacionId));
	}

	@Test
	void siQuedaOtraCriticaSinCerrarElEquipoSigueAveriado() throws Exception {
		hacerCriticaLaFuga();
		incidencias.save(new Incidencia(bomba, ana, "Vibración", "Vibra mucho.", Prioridad.CRITICA, LUNES));
		OrdenTrabajo orden = ordenResuelta();

		mvc.perform(validar(orden.getId()).header("X-Usuario-Id", responsable.getId()))
				.andExpect(status().isOk());

		mvc.perform(get("/api/equipos/{id}", bomba.getId()))
				.andExpect(jsonPath("$.estado").value("AVERIADO"));
	}

	@Test
	void validarSinCabeceraDeUsuarioDevuelve401() throws Exception {
		mvc.perform(validar(ordenResuelta().getId()))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.codigo").value("USUARIO_NO_IDENTIFICADO"));
	}

	@Test
	void elTecnicoNoPuedeValidarSuPropiaOrdenYDevuelve403() throws Exception {
		mvc.perform(validar(ordenResuelta().getId()).header("X-Usuario-Id", ana.getId()))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.codigo").value("RN-05"));
	}

	@Test
	void validarUnaOrdenQueNoExisteDevuelve404() throws Exception {
		mvc.perform(validar(999999L).header("X-Usuario-Id", responsable.getId()))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.detail").value("La orden 999999 no existe."));
	}

	@Test
	void validarUnaOrdenQueNoEstaResueltaDevuelve409() throws Exception {
		mvc.perform(validar(ordenEnCurso().getId()).header("X-Usuario-Id", responsable.getId()))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.codigo").value("RN-01"));
	}

	@Test
	void validarUnaOrdenYaCerradaDevuelve409() throws Exception {
		OrdenTrabajo cerrada = new OrdenTrabajo(fuga, responsable, ana, "Cerrada", MARTES);
		cerrada.validar();
		cerrada = ordenes.save(cerrada);

		mvc.perform(validar(cerrada.getId()).header("X-Usuario-Id", responsable.getId()))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.codigo").value("RN-23"));
	}

	// ---------- rechazar ----------

	@Test
	void rechazaLaOrdenYLaDevuelveAEnCursoSinFechaDeFin() throws Exception {
		OrdenTrabajo orden = ordenResuelta();

		mvc.perform(rechazar(orden.getId(), MOTIVO).header("X-Usuario-Id", responsable.getId()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.*", hasSize(11)))
				.andExpect(jsonPath("$.id").value(orden.getId()))
				.andExpect(jsonPath("$.estado").value("EN_CURSO"))
				.andExpect(jsonPath("$.fechaFin").value(nullValue()))
				.andExpect(jsonPath("$.fechaInicio").value("2026-10-20T09:00:00Z"));
	}

	@Test
	void elRechazoQuedaRegistradoConSuMotivoYLaIncidenciaNoCambia() throws Exception {
		OrdenTrabajo orden = ordenResuelta();

		mvc.perform(rechazar(orden.getId(), MOTIVO).header("X-Usuario-Id", responsable.getId()))
				.andExpect(status().isOk());

		List<Historial> entradas = historialDe(TipoEntidad.ORDEN, orden.getId());
		assertThat(entradas).hasSize(1);
		assertThat(entradas.get(0).getAccion()).isEqualTo(AccionHistorial.CAMBIO_ESTADO);
		assertThat(entradas.get(0).getValorAnterior()).isEqualTo("RESUELTA");
		assertThat(entradas.get(0).getValorNuevo()).isEqualTo("EN_CURSO");
		assertThat(entradas.get(0).getComentario()).isEqualTo("El sello sigue goteando");

		mvc.perform(get("/api/incidencias/{id}", fuga.getId()))
				.andExpect(jsonPath("$.estado").value("EN_CURSO"));
		assertThat(historialDe(TipoEntidad.INCIDENCIA, fuga.getId())).isEmpty();
	}

	@Test
	void trasUnRechazoElHistorialConservaLasDosResoluciones() throws Exception {
		OrdenTrabajo orden = ordenEnCurso();

		mvc.perform(resolver(orden.getId(), "Primer arreglo").header("X-Usuario-Id", ana.getId()))
				.andExpect(status().isOk());
		mvc.perform(rechazar(orden.getId(), MOTIVO).header("X-Usuario-Id", responsable.getId()))
				.andExpect(status().isOk());
		mvc.perform(resolver(orden.getId(), "Segundo arreglo").header("X-Usuario-Id", ana.getId()))
				.andExpect(status().isOk());

		mvc.perform(get("/api/ordenes/{id}", orden.getId()))
				.andExpect(jsonPath("$.estado").value("RESUELTA"))
				.andExpect(jsonPath("$.resolucion").value("Segundo arreglo"));

		List<String> comentarios = historialDe(TipoEntidad.ORDEN, orden.getId()).stream()
				.map(Historial::getComentario).toList();
		assertThat(comentarios).containsExactly("Primer arreglo", "El sello sigue goteando", "Segundo arreglo");
	}

	@Test
	void rechazarSinCabeceraDeUsuarioDevuelve401() throws Exception {
		mvc.perform(rechazar(ordenResuelta().getId(), MOTIVO))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.codigo").value("USUARIO_NO_IDENTIFICADO"));
	}

	@Test
	void elTecnicoNoPuedeRechazarSuPropiaResolucionYDevuelve403() throws Exception {
		mvc.perform(rechazar(ordenResuelta().getId(), MOTIVO).header("X-Usuario-Id", ana.getId()))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.codigo").value("RN-05"));
	}

	@Test
	void rechazarUnaOrdenQueNoExisteDevuelve404() throws Exception {
		mvc.perform(rechazar(999999L, MOTIVO).header("X-Usuario-Id", responsable.getId()))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.detail").value("La orden 999999 no existe."));
	}

	@Test
	void rechazarUnaOrdenQueNoEstaResueltaDevuelve409() throws Exception {
		mvc.perform(rechazar(ordenEnCurso().getId(), MOTIVO).header("X-Usuario-Id", responsable.getId()))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.codigo").value("RN-01"));
	}

	@Test
	void rechazarUnaOrdenCanceladaDevuelve409() throws Exception {
		OrdenTrabajo cancelada = new OrdenTrabajo(fuga, responsable, ana, "Cancelada", MARTES);
		cancelada.cancelar();
		cancelada = ordenes.save(cancelada);

		mvc.perform(rechazar(cancelada.getId(), MOTIVO).header("X-Usuario-Id", responsable.getId()))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.codigo").value("RN-23"));
	}

	@Test
	void sinMotivoDevuelve400ConElCampo() throws Exception {
		mvc.perform(rechazar(ordenResuelta().getId(), "{}").header("X-Usuario-Id", responsable.getId()))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.codigo").value("VALIDACION"))
				.andExpect(jsonPath("$.errores[*].campo", contains("comentario")))
				.andExpect(jsonPath("$.errores[0].mensaje").value("El comentario es obligatorio."));
	}

	@Test
	void unMotivoEnBlancoDevuelve400YNoRechazaLaOrden() throws Exception {
		OrdenTrabajo orden = ordenResuelta();

		mvc.perform(rechazar(orden.getId(), "{\"comentario\": \"   \"}").header("X-Usuario-Id",
				responsable.getId()))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errores[*].campo", contains("comentario")));

		mvc.perform(get("/api/ordenes/{id}", orden.getId()))
				.andExpect(jsonPath("$.estado").value("RESUELTA"));
	}

	@Test
	void unMotivoDeMasDeQuinientosCaracteresDevuelve400() throws Exception {
		String json = "{\"comentario\": \"" + "x".repeat(501) + "\"}";

		mvc.perform(rechazar(ordenResuelta().getId(), json).header("X-Usuario-Id", responsable.getId()))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errores[*].campo", contains("comentario")))
				.andExpect(jsonPath("$.errores[0].mensaje").value("El comentario admite como máximo 500 caracteres."));
	}

}