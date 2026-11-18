package com.mantix.web;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
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
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.mantix.TestcontainersConfiguration;
import com.mantix.domain.Equipo;
import com.mantix.domain.Incidencia;
import com.mantix.domain.OrdenTrabajo;
import com.mantix.domain.Perfil;
import com.mantix.domain.Prioridad;
import com.mantix.domain.Ubicacion;
import com.mantix.domain.Usuario;
import com.mantix.repository.EquipoRepository;
import com.mantix.repository.IncidenciaRepository;
import com.mantix.repository.OrdenTrabajoRepository;
import com.mantix.repository.UbicacionRepository;
import com.mantix.repository.UsuarioRepository;

// Sin @Transactional (igual que los tests de equipos): los datos se borran en @AfterEach.
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
public class IncidenciaControllerTest {

	private static final Instant LUNES = Instant.parse("2026-10-19T09:00:00Z");
	private static final Instant MARTES = Instant.parse("2026-10-20T09:00:00Z");
	private static final Instant MIERCOLES = Instant.parse("2026-10-21T09:00:00Z");

	@Autowired
	private MockMvc mvc;

	@Autowired
	private EquipoRepository equipos;

	@Autowired
	private IncidenciaRepository incidencias;

	@Autowired
	private OrdenTrabajoRepository ordenes;

	@Autowired
	private UbicacionRepository ubicaciones;

	@Autowired
	private UsuarioRepository usuarios;

	private Usuario responsable;
	private Usuario tecnico;
	private Equipo bomba;
	private Equipo compresor;

	@BeforeEach
	void preparar() {
		Ubicacion zonaA = ubicaciones.findAll(Sort.by("nombre")).get(0);
		responsable = usuarios.findAll().stream().filter(u -> u.getPerfil() == Perfil.RESPONSABLE).findFirst().get();
		tecnico = usuarios.findAll().stream().filter(u -> u.getPerfil() == Perfil.TECNICO && u.isActivo()).findFirst()
				.get();
		bomba = equipos.save(new Equipo("BOM-001", "Bomba de agua", zonaA, LocalDate.of(2026, 10, 18)));
		compresor = equipos.save(new Equipo("COM-001", "Compresor", zonaA, LocalDate.of(2026, 10, 18)));
	}

	@AfterEach
	void limpiar() {
		ordenes.deleteAll();
		incidencias.deleteAll();
		equipos.deleteAll();
	}

	private Incidencia incidencia(Equipo equipo, String titulo, Prioridad prioridad, Instant apertura) {
		return incidencias.save(new Incidencia(equipo, tecnico, titulo, "Descripción de " + titulo, prioridad,
				apertura));
	}

	@Test
	void listaLasIncidenciasMasRecientesPrimero() throws Exception {
		incidencia(bomba, "Segunda", Prioridad.MEDIA, MARTES);
		incidencia(bomba, "Tercera", Prioridad.MEDIA, MIERCOLES);
		incidencia(bomba, "Primera", Prioridad.MEDIA, LUNES);

		mvc.perform(get("/api/incidencias"))
				.andExpect(status().isOk())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$[*].titulo", contains("Tercera", "Segunda", "Primera")));
	}

	@Test
	void cadaIncidenciaExponeSusDiezCamposConElEquipoYElUsuarioResumidos() throws Exception {
		Incidencia fuga = incidencia(bomba, "Fuga en el sello", Prioridad.ALTA, LUNES);

		mvc.perform(get("/api/incidencias"))
				.andExpect(jsonPath("$[0].*", hasSize(10)))
				.andExpect(jsonPath("$[0].id").value(fuga.getId()))
				.andExpect(jsonPath("$[0].titulo").value("Fuga en el sello"))
				.andExpect(jsonPath("$[0].descripcion").value("Descripción de Fuga en el sello"))
				.andExpect(jsonPath("$[0].prioridad").value("ALTA"))
				.andExpect(jsonPath("$[0].estado").value("ABIERTA"))
				.andExpect(jsonPath("$[0].fechaApertura").value("2026-10-19T09:00:00Z"))
				.andExpect(jsonPath("$[0].fechaCierre").value(nullValue()))
				.andExpect(jsonPath("$[0].ordenActivaId").value(nullValue()))
				.andExpect(jsonPath("$[0].equipo.*", hasSize(3)))
				.andExpect(jsonPath("$[0].equipo.id").value(bomba.getId()))
				.andExpect(jsonPath("$[0].equipo.codigo").value("BOM-001"))
				.andExpect(jsonPath("$[0].equipo.nombre").value("Bomba de agua"))
				.andExpect(jsonPath("$[0].reportadaPor.*", hasSize(2)))
				.andExpect(jsonPath("$[0].reportadaPor.id").value(tecnico.getId()))
				.andExpect(jsonPath("$[0].reportadaPor.nombre").value(tecnico.getNombre()));
	}

	@Test
	void filtraPorEstado() throws Exception {
		incidencia(bomba, "Abierta", Prioridad.MEDIA, LUNES);
		Incidencia descartada = new Incidencia(bomba, tecnico, "Descartada", "Falsa alarma", Prioridad.BAJA, MARTES);
		descartada.descartar();
		incidencias.save(descartada);

		mvc.perform(get("/api/incidencias").param("estado", "DESCARTADA"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(1)))
				.andExpect(jsonPath("$[*].estado", everyItem(is("DESCARTADA"))))
				.andExpect(jsonPath("$[0].titulo").value("Descartada"));
	}

	@Test
	void filtraPorPrioridad() throws Exception {
		incidencia(bomba, "Leve", Prioridad.BAJA, LUNES);
		incidencia(bomba, "Grave", Prioridad.CRITICA, MARTES);

		mvc.perform(get("/api/incidencias").param("prioridad", "CRITICA"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(1)))
				.andExpect(jsonPath("$[0].titulo").value("Grave"));
	}

	@Test
	void filtraPorEquipo() throws Exception {
		incidencia(bomba, "De la bomba", Prioridad.MEDIA, LUNES);
		incidencia(compresor, "Del compresor", Prioridad.MEDIA, MARTES);

		mvc.perform(get("/api/incidencias").param("equipoId", compresor.getId().toString()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(1)))
				.andExpect(jsonPath("$[0].titulo").value("Del compresor"));
	}

	@Test
	void sinFiltrosNoDescartaNinguna() throws Exception {
		incidencia(bomba, "Una", Prioridad.MEDIA, LUNES);
		incidencia(compresor, "Otra", Prioridad.CRITICA, MARTES);

		mvc.perform(get("/api/incidencias"))
				.andExpect(jsonPath("$", hasSize(2)));
	}

	@Test
	void ordenActivaIdEsLaOrdenSinCerrarDeLaIncidencia() throws Exception {
		Incidencia fuga = incidencia(bomba, "Fuga", Prioridad.ALTA, LUNES);
		OrdenTrabajo orden = ordenes.save(new OrdenTrabajo(fuga, responsable, tecnico, "Cambiar el sello", MARTES));

		mvc.perform(get("/api/incidencias"))
				.andExpect(jsonPath("$[0].ordenActivaId").value(orden.getId()));
	}

	@Test
	void unaOrdenCanceladaNoCuentaComoActiva() throws Exception {
		Incidencia fuga = incidencia(bomba, "Fuga", Prioridad.ALTA, LUNES);
		OrdenTrabajo cancelada = new OrdenTrabajo(fuga, responsable, tecnico, "Cambiar el sello", MARTES);
		cancelada.cancelar();
		ordenes.save(cancelada);

		mvc.perform(get("/api/incidencias"))
				.andExpect(jsonPath("$[0].ordenActivaId").value(nullValue()));
	}

	@Test
	void unEstadoInvalidoDevuelve400() throws Exception {
		mvc.perform(get("/api/incidencias").param("estado", "ROTA"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.codigo").value("VALIDACION"));
	}

		@Test
	void elDetalleAnadeElCampoOrdenesALosDelListado() throws Exception {
		Incidencia fuga = incidencia(bomba, "Fuga en el sello", Prioridad.ALTA, LUNES);

		mvc.perform(get("/api/incidencias/{id}", fuga.getId()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.*", hasSize(11)))
				.andExpect(jsonPath("$.id").value(fuga.getId()))
				.andExpect(jsonPath("$.titulo").value("Fuga en el sello"))
				.andExpect(jsonPath("$.equipo.codigo").value("BOM-001"))
				.andExpect(jsonPath("$.reportadaPor.nombre").value(tecnico.getNombre()))
				.andExpect(jsonPath("$.ordenActivaId").value(nullValue()))
				.andExpect(jsonPath("$.ordenes", hasSize(0)));
	}

	@Test
	void lasOrdenesVanEnOrdenCronologicoYSoloConSusCuatroCampos() throws Exception {
		Incidencia fuga = incidencia(bomba, "Fuga", Prioridad.ALTA, LUNES);
		// Se guardan al revés a propósito: el orden lo pone la consulta.
		OrdenTrabajo nueva = ordenes.save(new OrdenTrabajo(fuga, responsable, null, "Segundo intento", MIERCOLES));
		OrdenTrabajo cancelada = new OrdenTrabajo(fuga, responsable, tecnico, "Primer intento", MARTES);
		cancelada.cancelar();
		cancelada = ordenes.save(cancelada);

		mvc.perform(get("/api/incidencias/{id}", fuga.getId()))
				.andExpect(jsonPath("$.ordenes", hasSize(2)))
				.andExpect(jsonPath("$.ordenes[*].id", contains(cancelada.getId().intValue(), nueva.getId().intValue())))
				.andExpect(jsonPath("$.ordenes[0].*", hasSize(4)))
				.andExpect(jsonPath("$.ordenes[0].estado").value("CANCELADA"))
				.andExpect(jsonPath("$.ordenes[0].tecnico.id").value(tecnico.getId()))
				.andExpect(jsonPath("$.ordenes[0].tecnico.nombre").value(tecnico.getNombre()))
				.andExpect(jsonPath("$.ordenes[0].fechaCreacion").value("2026-10-20T09:00:00Z"))
				.andExpect(jsonPath("$.ordenes[1].estado").value("PENDIENTE"))
				.andExpect(jsonPath("$.ordenes[1].tecnico").value(nullValue()))
				.andExpect(jsonPath("$.ordenActivaId").value(nueva.getId()));
	}

	@Test
	void soloIncluyeLasOrdenesDeEsaIncidencia() throws Exception {
		Incidencia fuga = incidencia(bomba, "Fuga", Prioridad.ALTA, LUNES);
		Incidencia ruido = incidencia(compresor, "Ruido", Prioridad.BAJA, MARTES);
		OrdenTrabajo deLaFuga = ordenes.save(new OrdenTrabajo(fuga, responsable, tecnico, "Sello", MARTES));
		ordenes.save(new OrdenTrabajo(ruido, responsable, tecnico, "Rodamiento", MIERCOLES));

		mvc.perform(get("/api/incidencias/{id}", fuga.getId()))
				.andExpect(jsonPath("$.ordenes", hasSize(1)))
				.andExpect(jsonPath("$.ordenes[0].id").value(deLaFuga.getId()));
	}

	@Test
	void unaIncidenciaQueNoExisteDevuelve404() throws Exception {
		mvc.perform(get("/api/incidencias/{id}", 999999))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.codigo").value("NO_ENCONTRADO"))
				.andExpect(jsonPath("$.detail").value("La incidencia 999999 no existe."));
	}


}

 