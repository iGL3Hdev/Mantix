package com.mantix.repository;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

import com.mantix.TestcontainersConfiguration;
import com.mantix.domain.Perfil;
import com.mantix.domain.Usuario;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@Transactional
class UsuarioRepositoryTest {

	@Autowired
	private UsuarioRepository usuarios;

	@Test
	void leeLosUsuariosIniciales() {
		assertThat(usuarios.findAll()).hasSize(4);
	}

	@Test
	void guardaUnUsuarioYLoRecupera() {
		Usuario guardado = usuarios.saveAndFlush(new Usuario("Eva Mora", "eva@mantix.local", Perfil.TECNICO));

		Usuario leido = usuarios.findById(guardado.getId()).orElseThrow();

		assertThat(leido.getNombre()).isEqualTo("Eva Mora");
		assertThat(leido.getPerfil()).isEqualTo(Perfil.TECNICO);
		assertThat(leido.isActivo()).isTrue();
	}

}
