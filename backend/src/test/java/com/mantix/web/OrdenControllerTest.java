package com.mantix.web;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
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

// Sin @Transactional (igual que los demás tests de controladores): los datos se borran en @AfterEach.
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
public class OrdenControllerTest {

	private static final Instant LUNES = Instant.parse("2026-10-19T09:00:00Z");
	private static final Instant MARTES = Instant.parse("2026-10-20T09:00:00Z");
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
	private UbicacionRepository ubicaciones;

	@Autowired
	private UsuarioRepository usuarios;

	private Usuario responsable;
	private Usuario ana;
	private Usuario pablo;
	private Incidencia fuga;
	private Incidencia ruido;

	@BeforeEach
	void preparar() {
		Ubicacion zonaA = ubicaciones.findAll(Sort.by("nombre")).get(0);
		responsable = usuarios.findAll().stream().filter(u -> u.getPerfil() == Perfil.RESPONSABLE).findFirst().get();
		List<Usuario> tecnicos = usuarios.findAll(Sort.by("nombre")).stream()
				.filter(u -> u.getPerfil() == Perfil.TECNICO && u.isActivo()).toList();
		ana = tecnicos.get(0);
		pablo = tecnicos.get(1);
		Equipo bomba = equipos.save(new Equipo("BOM-001", "Bomba de agua", zonaA, LocalDate.of(2026, 10, 18)));
		fuga = incidencias.save(new Incidencia(bomba, ana, "Fuga en el sello", "Gotea.", Prioridad.ALTA, LUNES));
		ruido = incidencias.save(new Incidencia(bomba, ana, "Ruido", "Suena raro.", Prioridad.BAJA, LUNES));
	}

	@AfterEach
	void limpiar() {
		ordenes.deleteAll();
		incidencias.deleteAll();
		equipos.deleteAll();
	}

	private OrdenTrabajo orden(Incidencia incidencia, Usuario tecnico, String descripcion, Instant creacion) {
		return ordenes.save(new OrdenTrabajo(incidencia, responsable, tecnico, descripcion, creacion));
	}

	@Test
	void listaLasOrdenesMasRecientesPrimero() throws Exception {
		orden(fuga, ana, "Segunda", MARTES);
		orden(fuga, ana, "Tercera", MIERCOLES);
		orden(fuga, ana, "Primera", LUNES);

		mvc.perform(get("/api/ordenes"))
				.andExpect(status().isOk())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$[*].descripcion", contains("Tercera", "Segunda", "Primera")));
	}

	@Test
	void cadaOrdenExponeSusOnceCamposConLosResumenes() throws Exception {
		OrdenTrabajo cambio = orden(fuga, ana, "Cambiar el sello", MARTES);

		mvc.perform(get("/api/ordenes"))
				.andExpect(jsonPath("$[0].*", hasSize(11)))
				.andExpect(jsonPath("$[0].id").value(cambio.getId()))
				.andExpect(jsonPath("$[0].descripcion").value("Cambiar el sello"))
				.andExpect(jsonPath("$[0].estado").value("ASIGNADA"))
				.andExpect(jsonPath("$[0].resolucion").value(nullValue()))
				.andExpect(jsonPath("$[0].fechaCreacion").value("2026-10-20T09:00:00Z"))
				.andExpect(jsonPath("$[0].fechaInicio").value(nullValue()))
				.andExpect(jsonPath("$[0].fechaFin").value(nullValue()))
				.andExpect(jsonPath("$[0].incidencia.*", hasSize(2)))
				.andExpect(jsonPath("$[0].incidencia.id").value(fuga.getId()))
				.andExpect(jsonPath("$[0].incidencia.titulo").value("Fuga en el sello"))
				.andExpect(jsonPath("$[0].equipo.*", hasSize(3)))
				.andExpect(jsonPath("$[0].equipo.codigo").value("BOM-001"))
				.andExpect(jsonPath("$[0].equipo.nombre").value("Bomba de agua"))
				.andExpect(jsonPath("$[0].creadaPor.*", hasSize(2)))
				.andExpect(jsonPath("$[0].creadaPor.id").value(responsable.getId()))
				.andExpect(jsonPath("$[0].creadaPor.nombre").value(responsable.getNombre()))
				.andExpect(jsonPath("$[0].tecnico.*", hasSize(2)))
				.andExpect(jsonPath("$[0].tecnico.id").value(ana.getId()))
				.andExpect(jsonPath("$[0].tecnico.nombre").value(ana.getNombre()));
	}

	@Test
	void unaOrdenPendienteNoTieneTecnico() throws Exception {
		orden(fuga, null, "Sin asignar", MARTES);

		mvc.perform(get("/api/ordenes"))
				.andExpect(jsonPath("$[0].estado").value("PENDIENTE"))
				.andExpect(jsonPath("$[0].tecnico").value(nullValue()));
	}

	@Test
	void unaOrdenResueltaMuestraSuResolucionYSusFechas() throws Exception {
		OrdenTrabajo resuelta = new OrdenTrabajo(fuga, responsable, ana, "Cambiar el sello", MARTES);
		resuelta.empezar(MIERCOLES);
		resuelta.resolver("Sello cambiado", JUEVES);
		ordenes.save(resuelta);

		mvc.perform(get("/api/ordenes"))
				.andExpect(jsonPath("$[0].estado").value("RESUELTA"))
				.andExpect(jsonPath("$[0].resolucion").value("Sello cambiado"))
				.andExpect(jsonPath("$[0].fechaInicio").value("2026-10-21T09:00:00Z"))
				.andExpect(jsonPath("$[0].fechaFin").value("2026-10-22T09:00:00Z"));
	}

	@Test
	void filtraPorEstado() throws Exception {
		orden(fuga, ana, "Asignada", MARTES);
		OrdenTrabajo cancelada = new OrdenTrabajo(fuga, responsable, ana, "Cancelada", MIERCOLES);
		cancelada.cancelar();
		ordenes.save(cancelada);

		mvc.perform(get("/api/ordenes").param("estado", "CANCELADA"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(1)))
				.andExpect(jsonPath("$[*].estado", everyItem(is("CANCELADA"))))
				.andExpect(jsonPath("$[0].descripcion").value("Cancelada"));
	}

	@Test
	void filtraPorTecnico() throws Exception {
		orden(fuga, ana, "De Ana", MARTES);
		orden(fuga, pablo, "De Pablo", MIERCOLES);

		mvc.perform(get("/api/ordenes").param("tecnicoId", pablo.getId().toString()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(1)))
				.andExpect(jsonPath("$[0].descripcion").value("De Pablo"));
	}

	@Test
	void filtraPorIncidencia() throws Exception {
		orden(fuga, ana, "De la fuga", MARTES);
		orden(ruido, ana, "Del ruido", MIERCOLES);

		mvc.perform(get("/api/ordenes").param("incidenciaId", ruido.getId().toString()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(1)))
				.andExpect(jsonPath("$[0].descripcion").value("Del ruido"));
	}

	@Test
	void sinFiltrosNoDescartaNinguna() throws Exception {
		orden(fuga, ana, "Una", MARTES);
		orden(ruido, pablo, "Otra", MIERCOLES);

		mvc.perform(get("/api/ordenes"))
				.andExpect(jsonPath("$", hasSize(2)));
	}

	@Test
	void unEstadoInvalidoDevuelve400() throws Exception {
		mvc.perform(get("/api/ordenes").param("estado", "ROTA"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.codigo").value("VALIDACION"));
	}

	@Test
	void devuelveUnaOrdenPorId() throws Exception {
		OrdenTrabajo cambio = orden(fuga, ana, "Cambiar el sello", MARTES);

		mvc.perform(get("/api/ordenes/{id}", cambio.getId()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.*", hasSize(11)))
				.andExpect(jsonPath("$.id").value(cambio.getId()))
				.andExpect(jsonPath("$.descripcion").value("Cambiar el sello"))
				.andExpect(jsonPath("$.incidencia.titulo").value("Fuga en el sello"))
				.andExpect(jsonPath("$.equipo.codigo").value("BOM-001"))
				.andExpect(jsonPath("$.tecnico.nombre").value(ana.getNombre()));
	}

	@Test
	void unaOrdenQueNoExisteDevuelve404() throws Exception {
		mvc.perform(get("/api/ordenes/{id}", 999999))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.codigo").value("NO_ENCONTRADO"))
				.andExpect(jsonPath("$.detail").value("La orden 999999 no existe."));
	}

}
