package com.mantix.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.mantix.TestcontainersConfiguration;
import com.mantix.domain.AccionHistorial;
import com.mantix.domain.Historial;
import com.mantix.domain.TipoEntidad;
import com.mantix.domain.Usuario;
import com.mantix.repository.UsuarioRepository;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@Transactional
class HistorialServiceTest {

	@Autowired
	private HistorialService historialService;

	@Autowired
	private UsuarioRepository usuarios;

	@Test
	void registraLaFilaConLaFechaDelServidor() {
		Usuario usuario = usuarios.findAll().getFirst();
		Instant antes = Instant.now();

		Historial fila = historialService.registrar(TipoEntidad.EQUIPO, 7L, AccionHistorial.ALTA,
				null, "OPERATIVO", null, usuario, null);

		assertThat(fila.getId()).isNotNull();
		assertThat(fila.getFecha()).isBetween(antes, Instant.now());
		assertThat(fila.getUsuario().getId()).isEqualTo(usuario.getId());
	}

	@Test
	void guardaLaConsecuenciaApuntandoALaFilaPrincipal() {
		Usuario usuario = usuarios.findAll().getFirst();
		Historial principal = historialService.registrar(TipoEntidad.ORDEN, 12L,
				AccionHistorial.CAMBIO_ESTADO, "RESUELTA", "CERRADA", null, usuario, null);

		Historial consecuencia = historialService.registrar(TipoEntidad.INCIDENCIA, 5L,
				AccionHistorial.CAMBIO_ESTADO, "EN_CURSO", "CERRADA", principal, usuario, null);

		assertThat(consecuencia.getOrigen()).isSameAs(principal);
		assertThat(historialService.consultar(TipoEntidad.INCIDENCIA, 5L)).containsExactly(consecuencia);
	}

	@Test
	@Transactional(propagation = Propagation.NOT_SUPPORTED)
	void rechazaRegistrarSinUnaTransaccionAbierta() {
		Usuario usuario = usuarios.findAll().getFirst();

		assertThatThrownBy(() -> historialService.registrar(TipoEntidad.EQUIPO, 7L, AccionHistorial.ALTA,
				null, "OPERATIVO", null, usuario, null))
				.isInstanceOf(IllegalTransactionStateException.class);
	}

}
