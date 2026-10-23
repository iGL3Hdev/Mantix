package com.mantix.repository;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

import com.mantix.TestcontainersConfiguration;
import com.mantix.domain.AccionHistorial;
import com.mantix.domain.Historial;
import com.mantix.domain.TipoEntidad;
import com.mantix.domain.Usuario;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@Transactional
class HistorialRepositoryTest {

	@Autowired
	private HistorialRepository historial;

	@Autowired
	private UsuarioRepository usuarios;

	@Test
	void guardaUnaConsecuenciaQueApuntaASuFilaPrincipal() {
		Usuario usuario = usuarios.findAll().getFirst();
		Historial principal = historial.saveAndFlush(new Historial(TipoEntidad.ORDEN, 12L,
				AccionHistorial.CAMBIO_ESTADO, "RESUELTA", "CERRADA", null, usuario,
				Instant.parse("2026-10-22T10:00:00Z"), null));
		Historial consecuencia = historial.saveAndFlush(new Historial(TipoEntidad.INCIDENCIA, 5L,
				AccionHistorial.CAMBIO_ESTADO, "EN_CURSO", "CERRADA", principal, usuario,
				Instant.parse("2026-10-22T10:00:00Z"), null));

		Historial leida = historial.findById(consecuencia.getId()).orElseThrow();

		assertThat(leida.getOrigen().getId()).isEqualTo(principal.getId());
		assertThat(principal.getOrigen()).isNull();
	}

	@Test
	void devuelveElHistorialDeUnaEntidadEnOrdenCronologico() {
		Usuario usuario = usuarios.findAll().getFirst();
		historial.saveAndFlush(new Historial(TipoEntidad.ORDEN, 12L, AccionHistorial.CAMBIO_ESTADO,
				"ASIGNADA", "EN_CURSO", null, usuario, Instant.parse("2026-10-22T11:00:00Z"), null));
		historial.saveAndFlush(new Historial(TipoEntidad.ORDEN, 12L, AccionHistorial.ALTA,
				null, "PENDIENTE", null, usuario, Instant.parse("2026-10-22T09:00:00Z"), null));
		historial.saveAndFlush(new Historial(TipoEntidad.INCIDENCIA, 12L, AccionHistorial.ALTA,
				null, "ABIERTA", null, usuario, Instant.parse("2026-10-22T08:00:00Z"), null));

		List<Historial> filas = historial.findByTipoEntidadAndEntidadIdOrderByFechaAscIdAsc(TipoEntidad.ORDEN, 12L);

		assertThat(filas).extracting(Historial::getValorNuevo).containsExactly("PENDIENTE", "EN_CURSO");
	}

}
