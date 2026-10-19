package com.mantix;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class EsquemaInicialTest {

	@Autowired
	private JdbcTemplate jdbc;

		@Test
	void flywayAplicaLasMigraciones() {
		String version = jdbc.queryForObject(
				"SELECT version FROM flyway_schema_history WHERE success = TRUE ORDER BY installed_rank DESC LIMIT 1",
				String.class);

		assertThat(version).isEqualTo("2");
	}


	@Test
	void creaLasSeisTablasDelModelo() {
		List<String> tablas = jdbc.queryForList(
				"SELECT table_name FROM information_schema.tables WHERE table_schema = DATABASE()",
				String.class);

		assertThat(tablas).contains(
				"usuario", "ubicacion", "equipo", "incidencia", "orden_trabajo", "historial");
	}

    	@Test
	void cargaUsuariosYUbicacionesIniciales() {
		Integer responsables = jdbc.queryForObject(
				"SELECT COUNT(*) FROM usuario WHERE perfil = 'RESPONSABLE'", Integer.class);
		Integer tecnicosActivos = jdbc.queryForObject(
				"SELECT COUNT(*) FROM usuario WHERE perfil = 'TECNICO' AND activo = TRUE", Integer.class);
		Integer tecnicosInactivos = jdbc.queryForObject(
				"SELECT COUNT(*) FROM usuario WHERE perfil = 'TECNICO' AND activo = FALSE", Integer.class);
		Integer ubicaciones = jdbc.queryForObject("SELECT COUNT(*) FROM ubicacion", Integer.class);

		assertThat(responsables).isEqualTo(1);
		assertThat(tecnicosActivos).isEqualTo(2);
		assertThat(tecnicosInactivos).isEqualTo(1);
		assertThat(ubicaciones).isEqualTo(3);
	}


}

