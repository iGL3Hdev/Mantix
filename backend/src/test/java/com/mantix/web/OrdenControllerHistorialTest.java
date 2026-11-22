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
public class OrdenControllerHistorialTest {

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
	private HistorialRepository historial;

	@Autowired
	private UbicacionRepository ubicaciones;

	@Autowired
	private UsuarioRepository usuarios;

	private Usuario responsable;
	private Usuario ana;
	private OrdenTrabajo primera;
	private OrdenTrabajo segunda;

	@BeforeEach
	void preparar() {
		Ubicacion zonaA = ubicaciones.findAll(Sort.by("nombre")).get(0);
		responsable = usuarios.findAll().stream().filter(u -> u.getPerfil() == Perfil.RESPONSABLE).findFirst().get();
		ana = usuarios.findAll().stream().filter(u -> u.getPerfil() == Perfil.TECNICO && u.isActivo()).findFirst()
				.get();
		Equipo bomba = equipos.save(new Equipo("BOM-001", "Bomba de agua", zonaA, LocalDate.of(2026, 10, 18)));
		Incidencia fuga = incidencias
				.save(new Incidencia(bomba, ana, "Fuga en el sello", "Gotea.", Prioridad.ALTA, LUNES));
		primera = ordenes.save(new OrdenTrabajo(fuga, responsable, ana, "Primer intento", MARTES));
		segunda = ordenes.save(new OrdenTrabajo(fuga, responsable, ana, "Segundo intento", MIERCOLES));
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

	@Test
	void devuelveLasEntradasEnOrdenCronologico() throws Exception {
		// Se guardan al revés a propósito: el orden lo pone la consulta, no el orden de inserción.
		entrada(TipoEntidad.ORDEN, primera.getId(), AccionHistorial.CAMBIO_ESTADO, "ASIGNADA", "EN_CURSO", null,
				MIERCOLES);
		entrada(TipoEntidad.ORDEN, primera.getId(), AccionHistorial.ALTA, null, "ASIGNADA", null, MARTES);

		mvc.perform(get("/api/ordenes/{id}/historial", primera.getId()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[*].accion", contains("ALTA", "CAMBIO_ESTADO")))
				.andExpect(jsonPath("$[*].fecha", contains("2026-10-20T09:00:00Z", "2026-10-21T09:00:00Z")));
	}

	@Test
	void cadaEntradaExponeSusDiezCamposConElUsuarioResumido() throws Exception {
		Historial alta = entrada(TipoEntidad.ORDEN, primera.getId(), AccionHistorial.ALTA, null, "ASIGNADA", null,
				MARTES);

		mvc.perform(get("/api/ordenes/{id}/historial", primera.getId()))
				.andExpect(jsonPath("$[0].*", hasSize(10)))
				.andExpect(jsonPath("$[0].id").value(alta.getId()))
				.andExpect(jsonPath("$[0].tipoEntidad").value("ORDEN"))
				.andExpect(jsonPath("$[0].entidadId").value(primera.getId()))
				.andExpect(jsonPath("$[0].accion").value("ALTA"))
				.andExpect(jsonPath("$[0].valorAnterior").value(nullValue()))
				.andExpect(jsonPath("$[0].valorNuevo").value("ASIGNADA"))
				.andExpect(jsonPath("$[0].origenId").value(nullValue()))
				.andExpect(jsonPath("$[0].comentario").value(nullValue()))
				.andExpect(jsonPath("$[0].usuario.*", hasSize(2)))
				.andExpect(jsonPath("$[0].usuario.id").value(responsable.getId()))
				.andExpect(jsonPath("$[0].usuario.nombre").value(responsable.getNombre()));
	}

	@Test
	void unaEntradaConsecuenciaIndicaElIdDeSuOrigen() throws Exception {
		Historial alta = entrada(TipoEntidad.ORDEN, primera.getId(), AccionHistorial.ALTA, null, "ASIGNADA", null,
				MARTES);
		entrada(TipoEntidad.ORDEN, primera.getId(), AccionHistorial.CAMBIO_TECNICO, null,
				String.valueOf(ana.getId()), alta, MARTES);

		mvc.perform(get("/api/ordenes/{id}/historial", primera.getId()))
				.andExpect(jsonPath("$[0].origenId").value(nullValue()))
				.andExpect(jsonPath("$[1].accion").value("CAMBIO_TECNICO"))
				.andExpect(jsonPath("$[1].origenId").value(alta.getId()));
	}

	@Test
	void soloDevuelveLasEntradasDeEsaOrdenYNoLasDeOtraEntidadConElMismoId() throws Exception {
		entrada(TipoEntidad.ORDEN, primera.getId(), AccionHistorial.ALTA, null, "ASIGNADA", null, MARTES);
		entrada(TipoEntidad.ORDEN, segunda.getId(), AccionHistorial.ALTA, null, "PENDIENTE", null, MIERCOLES);
		// El historial es polimórfico: una incidencia puede tener el mismo número de id que la orden.
		entrada(TipoEntidad.INCIDENCIA, primera.getId(), AccionHistorial.ALTA, null, "ABIERTA", null, LUNES);

		mvc.perform(get("/api/ordenes/{id}/historial", primera.getId()))
				.andExpect(jsonPath("$", hasSize(1)))
				.andExpect(jsonPath("$[0].tipoEntidad").value("ORDEN"))
				.andExpect(jsonPath("$[0].entidadId").value(primera.getId()))
				.andExpect(jsonPath("$[0].valorNuevo").value("ASIGNADA"));
	}

	@Test
	void unaOrdenSinHistorialDevuelveUnaListaVacia() throws Exception {
		mvc.perform(get("/api/ordenes/{id}/historial", primera.getId()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(0)));
	}

	@Test
	void unaOrdenQueNoExisteDevuelve404() throws Exception {
		mvc.perform(get("/api/ordenes/{id}/historial", 999999))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.codigo").value("NO_ENCONTRADO"))
				.andExpect(jsonPath("$.detail").value("La orden 999999 no existe."));
	}

}