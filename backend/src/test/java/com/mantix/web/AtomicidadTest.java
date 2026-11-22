package com.mantix.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doAnswer;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.mantix.TestcontainersConfiguration;
import com.mantix.domain.Equipo;
import com.mantix.domain.Incidencia;
import com.mantix.domain.Perfil;
import com.mantix.domain.Prioridad;
import com.mantix.domain.Ubicacion;
import com.mantix.domain.Usuario;
import com.mantix.repository.EquipoRepository;
import com.mantix.repository.HistorialRepository;
import com.mantix.repository.IncidenciaRepository;
import com.mantix.repository.OrdenTrabajoRepository;
import com.mantix.repository.UbicacionRepository;
import com.mantix.repository.UsuarioRepository;

// CA-32 (RN-02): una acción rechazada no deja cambios en ninguna tabla, tampoco en el historial.
// Sin @Transactional: el rollback tiene que hacerlo el servicio, no el test.
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
public class AtomicidadTest {

	private static final Instant LUNES = Instant.parse("2026-10-19T09:00:00Z");
	private static final List<String> TABLAS = List.of("usuario", "ubicacion", "equipo", "incidencia",
			"orden_trabajo", "historial");

	@Autowired
	private MockMvc mvc;

	@Autowired
	private JdbcTemplate jdbc;

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

	// Espía del reloj real: permite hacer fallar una llamada concreta a mitad de una acción.
	@MockitoSpyBean
	private Clock reloj;

	private Usuario responsable;
	private Usuario ana;
	private Usuario inactivo;
	private Equipo bomba;
	private Incidencia fuga;

	@BeforeEach
	void preparar() {
		Ubicacion zonaA = ubicaciones.findAll(Sort.by("nombre")).get(0);
		responsable = usuarios.findAll().stream().filter(u -> u.getPerfil() == Perfil.RESPONSABLE).findFirst().get();
		ana = usuarios.findAll().stream().filter(u -> u.getPerfil() == Perfil.TECNICO && u.isActivo()).findFirst()
				.get();
		inactivo = usuarios.findAll().stream().filter(u -> !u.isActivo()).findFirst().get();
		bomba = equipos.save(new Equipo("BOM-001", "Bomba de agua", zonaA, LocalDate.of(2026, 10, 18)));
		fuga = incidencias.save(new Incidencia(bomba, ana, "Fuga en el sello", "Gotea.", Prioridad.ALTA, LUNES));
	}

	@AfterEach
	void limpiar() {
		historial.deleteAll(historial.findAll(Sort.by(Sort.Direction.DESC, "id")));
		ordenes.deleteAll();
		incidencias.deleteAll();
		equipos.deleteAll();
	}

	// Una foto del contenido completo de todas las tablas.
	private Map<String, List<Map<String, Object>>> foto() {
		Map<String, List<Map<String, Object>>> foto = new java.util.LinkedHashMap<>();
		for (String tabla : TABLAS) {
			foto.put(tabla, jdbc.queryForList("select * from " + tabla + " order by id"));
		}
		return foto;
	}

	@Test
	void unFalloAMitadDeCrearUnaOrdenDeshaceTodoLoQueYaSeHabiaEscrito() throws Exception {
		Map<String, List<Map<String, Object>>> antes = foto();
		AtomicInteger llamadas = new AtomicInteger();
		// Crear una orden con técnico pide la hora cuatro veces (la orden y tres entradas de historial).
		// La cuarta falla: para entonces ya se han escrito la orden y dos filas de historial.
		doAnswer(invocacion -> {
			if (llamadas.incrementAndGet() == 4) {
				throw new IllegalStateException("fallo simulado");
			}
			return invocacion.callRealMethod();
		}).when(reloj).instant();

		String cuerpo = "{\"incidenciaId\": " + fuga.getId() + ", \"tecnicoId\": " + ana.getId()
				+ ", \"descripcion\": \"Cambiar el sello\"}";
		mvc.perform(post("/api/ordenes").contentType(MediaType.APPLICATION_JSON).content(cuerpo)
				.header("X-Usuario-Id", responsable.getId()))
				.andExpect(status().isInternalServerError())
				.andExpect(jsonPath("$.codigo").value("ERROR_INTERNO"))
				.andExpect(jsonPath("$.detail").value("Se ha producido un error inesperado."));

		assertThat(llamadas.get()).isEqualTo(4);
		assertThat(foto()).isEqualTo(antes);
		mvc.perform(get("/api/incidencias/{id}", fuga.getId()))
				.andExpect(jsonPath("$.estado").value("ABIERTA"))
				.andExpect(jsonPath("$.ordenActivaId").value(org.hamcrest.Matchers.nullValue()));
	}

	@Test
	void lasAccionesRechazadasNoDejanCambiosEnNingunaTabla() throws Exception {
		Map<String, List<Map<String, Object>>> antes = foto();

		// 403: técnico inactivo (RN-04)
		String conInactivo = "{\"incidenciaId\": " + fuga.getId() + ", \"tecnicoId\": " + inactivo.getId()
				+ ", \"descripcion\": \"Cambiar el sello\"}";
		mvc.perform(post("/api/ordenes").contentType(MediaType.APPLICATION_JSON).content(conInactivo)
				.header("X-Usuario-Id", responsable.getId()))
				.andExpect(status().isForbidden());

		// 409: dar de baja un equipo con incidencias sin cerrar (RN-12)
		mvc.perform(post("/api/equipos/{id}/baja", bomba.getId()).contentType(MediaType.APPLICATION_JSON)
				.content("{\"comentario\": \"Retirada\"}").header("X-Usuario-Id", responsable.getId()))
				.andExpect(status().isConflict());

		// 409: reabrir una incidencia que no está cerrada (RN-17)
		mvc.perform(post("/api/incidencias/{id}/reabrir", fuga.getId()).contentType(MediaType.APPLICATION_JSON)
				.content("{\"comentario\": \"Otra vez\"}").header("X-Usuario-Id", responsable.getId()))
				.andExpect(status().isConflict());

		// 400: cuerpo inválido
		mvc.perform(post("/api/ordenes").contentType(MediaType.APPLICATION_JSON).content("{}")
				.header("X-Usuario-Id", responsable.getId()))
				.andExpect(status().isBadRequest());

		assertThat(foto()).isEqualTo(antes);
	}

}
