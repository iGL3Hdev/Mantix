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
public class IncidenciaControllerHistorialTest {

	private static final Instant LUNES_9 = Instant.parse("2026-10-19T09:00:00Z");
	private static final Instant LUNES_10 = Instant.parse("2026-10-19T10:00:00Z");
	private static final Instant MARTES_9 = Instant.parse("2026-10-20T09:00:00Z");
	private static final Instant MARTES_10 = Instant.parse("2026-10-20T10:00:00Z");
	private static final Instant MARTES_11 = Instant.parse("2026-10-20T11:00:00Z");
	private static final Instant MIERCOLES = Instant.parse("2026-10-21T09:00:00Z");
	private static final Instant JUEVES = Instant.parse("2026-10-22T09:00:00Z");

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
	private Incidencia fuga;
	private Incidencia ruido;
	private OrdenTrabajo primeraOrden;
	private OrdenTrabajo segundaOrden;
	private OrdenTrabajo ordenDelRuido;

	@BeforeEach
	void preparar() {
		Ubicacion zonaA = ubicaciones.findAll(Sort.by("nombre")).get(0);
		responsable = usuarios.findAll().stream().filter(u -> u.getPerfil() == Perfil.RESPONSABLE).findFirst().get();
		tecnico = usuarios.findAll().stream().filter(u -> u.getPerfil() == Perfil.TECNICO && u.isActivo()).findFirst()
				.get();
		Equipo bomba = equipos.save(new Equipo("BOM-001", "Bomba de agua", zonaA, LocalDate.of(2026, 10, 18)));
		fuga = incidencias.save(new Incidencia(bomba, tecnico, "Fuga en el sello", "Gotea.", Prioridad.ALTA, LUNES_9));
		ruido = incidencias.save(new Incidencia(bomba, tecnico, "Ruido", "Suena raro.", Prioridad.BAJA, LUNES_9));
		primeraOrden = ordenes.save(new OrdenTrabajo(fuga, responsable, tecnico, "Primer intento", MARTES_9));
		segundaOrden = ordenes.save(new OrdenTrabajo(fuga, responsable, tecnico, "Segundo intento", JUEVES));
		ordenDelRuido = ordenes.save(new OrdenTrabajo(ruido, responsable, tecnico, "Revisar", MARTES_11));
	}

	@AfterEach
	void limpiar() {
		// Las entradas consecuencia apuntan a su origen: se borran primero las más recientes.
		historial.deleteAll(historial.findAll(Sort.by(Sort.Direction.DESC, "id")));
		ordenes.deleteAll();
		incidencias.deleteAll();
		equipos.deleteAll();
	}

	private Historial entrada(TipoEntidad tipo, Long entidadId, AccionHistorial accion, String anterior,
			String nuevo, Historial origen, Instant fecha) {
		return historial.save(new Historial(tipo, entidadId, accion, anterior, nuevo, origen, responsable, fecha, null));
	}

	// Historial completo del ciclo de "fuga". Se guarda desordenado a propósito: el orden lo pone la consulta.
	// Devuelve el alta de la primera orden, de la que depende la entrada consecuencia de la incidencia.
	private Historial rellenarElCicloDeLaFuga() {
		Historial altaOrden1 = entrada(TipoEntidad.ORDEN, primeraOrden.getId(), AccionHistorial.ALTA, null,
				"ASIGNADA", null, MARTES_9);
		entrada(TipoEntidad.ORDEN, segundaOrden.getId(), AccionHistorial.ALTA, null, "ASIGNADA", null, JUEVES);
		entrada(TipoEntidad.ORDEN, primeraOrden.getId(), AccionHistorial.CAMBIO_ESTADO, "ASIGNADA", "CANCELADA",
				null, MIERCOLES);
		entrada(TipoEntidad.INCIDENCIA, fuga.getId(), AccionHistorial.CAMBIO_ESTADO, "ABIERTA", "EN_CURSO",
				altaOrden1, MARTES_10);
		entrada(TipoEntidad.INCIDENCIA, fuga.getId(), AccionHistorial.ALTA, null, "ABIERTA", null, LUNES_9);
		return altaOrden1;
	}

	private void rellenarLoAjeno() {
		entrada(TipoEntidad.INCIDENCIA, ruido.getId(), AccionHistorial.ALTA, null, "ABIERTA", null, LUNES_10);
		entrada(TipoEntidad.ORDEN, ordenDelRuido.getId(), AccionHistorial.ALTA, null, "ASIGNADA", null, MARTES_11);
	}

	@Test
	void mezclaLasEntradasDeLaIncidenciaYDeSusOrdenesEnOrdenCronologico() throws Exception {
		rellenarElCicloDeLaFuga();
		rellenarLoAjeno();

		mvc.perform(get("/api/incidencias/{id}/historial", fuga.getId()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(5)))
				.andExpect(jsonPath("$[*].tipoEntidad",
						contains("INCIDENCIA", "ORDEN", "INCIDENCIA", "ORDEN", "ORDEN")))
				.andExpect(jsonPath("$[*].entidadId", contains(fuga.getId().intValue(),
						primeraOrden.getId().intValue(), fuga.getId().intValue(), primeraOrden.getId().intValue(),
						segundaOrden.getId().intValue())))
				.andExpect(jsonPath("$[*].fecha", contains("2026-10-19T09:00:00Z", "2026-10-20T09:00:00Z",
						"2026-10-20T10:00:00Z", "2026-10-21T09:00:00Z", "2026-10-22T09:00:00Z")));
	}

	@Test
	void cadaEntradaExponeSusDiezCamposConElUsuarioResumido() throws Exception {
		rellenarElCicloDeLaFuga();

		mvc.perform(get("/api/incidencias/{id}/historial", fuga.getId()))
				.andExpect(jsonPath("$[0].*", hasSize(10)))
				.andExpect(jsonPath("$[0].tipoEntidad").value("INCIDENCIA"))
				.andExpect(jsonPath("$[0].entidadId").value(fuga.getId()))
				.andExpect(jsonPath("$[0].accion").value("ALTA"))
				.andExpect(jsonPath("$[0].valorNuevo").value("ABIERTA"))
				.andExpect(jsonPath("$[0].usuario.id").value(responsable.getId()))
				.andExpect(jsonPath("$[0].usuario.nombre").value(responsable.getNombre()));
	}

	@Test
	void laEntradaConsecuenciaIndicaElIdDeSuOrigen() throws Exception {
		Historial altaOrden1 = rellenarElCicloDeLaFuga();

		mvc.perform(get("/api/incidencias/{id}/historial", fuga.getId()))
				.andExpect(jsonPath("$[0].origenId").value(nullValue()))
				.andExpect(jsonPath("$[2].accion").value("CAMBIO_ESTADO"))
				.andExpect(jsonPath("$[2].origenId").value(altaOrden1.getId()));
	}

	@Test
	void unaIncidenciaSoloIncluyeLasOrdenesQueSonSuyas() throws Exception {
		rellenarElCicloDeLaFuga();
		rellenarLoAjeno();

		mvc.perform(get("/api/incidencias/{id}/historial", ruido.getId()))
				.andExpect(jsonPath("$", hasSize(2)))
				.andExpect(jsonPath("$[0].tipoEntidad").value("INCIDENCIA"))
				.andExpect(jsonPath("$[0].entidadId").value(ruido.getId()))
				.andExpect(jsonPath("$[1].tipoEntidad").value("ORDEN"))
				.andExpect(jsonPath("$[1].entidadId").value(ordenDelRuido.getId()));
	}

	@Test
	void unaIncidenciaSinOrdenesDevuelveSoloLasSuyas() throws Exception {
		Incidencia sinOrdenes = incidencias.save(new Incidencia(fuga.getEquipo(), tecnico, "Otra", "Sin orden.",
				Prioridad.MEDIA, LUNES_10));
		entrada(TipoEntidad.INCIDENCIA, sinOrdenes.getId(), AccionHistorial.ALTA, null, "ABIERTA", null, LUNES_10);

		mvc.perform(get("/api/incidencias/{id}/historial", sinOrdenes.getId()))
				.andExpect(jsonPath("$", hasSize(1)))
				.andExpect(jsonPath("$[0].entidadId").value(sinOrdenes.getId()));
	}

	@Test
	void unaIncidenciaSinHistorialDevuelveUnaListaVacia() throws Exception {
		mvc.perform(get("/api/incidencias/{id}/historial", fuga.getId()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(0)));
	}

	@Test
	void unaIncidenciaQueNoExisteDevuelve404() throws Exception {
		mvc.perform(get("/api/incidencias/{id}/historial", 999999))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.codigo").value("NO_ENCONTRADO"))
				.andExpect(jsonPath("$.detail").value("La incidencia 999999 no existe."));
	}

}
