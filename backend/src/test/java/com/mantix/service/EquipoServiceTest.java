package com.mantix.service;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

import com.mantix.TestcontainersConfiguration;
import com.mantix.domain.AccionHistorial;
import com.mantix.domain.Equipo;
import com.mantix.domain.EstadoEquipo;
import com.mantix.domain.Historial;
import com.mantix.domain.Perfil;
import com.mantix.domain.TipoEntidad;
import com.mantix.domain.Usuario;
import com.mantix.repository.EquipoRepository;
import com.mantix.repository.UbicacionRepository;
import com.mantix.repository.UsuarioRepository;

@Import(TestcontainersConfiguration.class)
@SpringBootTest 
@Transactional 
public class EquipoServiceTest {
    
    @Autowired 
    private EquipoService equipoService;

    @Autowired
    private HistorialService historialService;

    @Autowired 
    private EquipoRepository equipos;

    @Autowired 
    private UbicacionRepository ubicaciones;

    @Autowired 
    private UsuarioRepository usuarios;

    private Usuario responsable;
    private Usuario tecnico;
    private Long ubicacionId;

    @BeforeEach 
    void prepararDatos() {
        responsable = usuarios.findAll().stream()
            .filter(u -> u.getPerfil() == Perfil.RESPONSABLE).findFirst().orElseThrow();
        tecnico = usuarios.findAll().stream()
            .filter(u -> u.getPerfil() == Perfil.TECNICO && u.isActivo()).findFirst().orElseThrow();
        ubicacionId = ubicaciones.findAll().getFirst().getId();
    }

    private DatosEquipo datos(String codigo) {
        return new DatosEquipo(codigo, "Bomba principal", "Grundfos", "CR 10", "A1234", ubicacionId, null);

    }

    @Test
    void elResponsableDaDeAltaUnEquipoOperativoConLaFechaDeHoy() {
        LocalDate antes = LocalDate.now(ZoneOffset.UTC);

        Equipo equipo = equipoService.crear(responsable.getId(), datos("BOM-001"));

        assertThat(equipo.getId()).isNotNull();
        assertThat(equipo.getEstado()).isEqualTo(EstadoEquipo.OPERATIVO);
        assertThat(equipo.getFabricante()).isEqualTo("Grundfos");
        assertThat(equipo.getFechaAlta()).isBetween(antes, LocalDate.now(ZoneOffset.UTC));
    }

    @Test 
    void elAltaDejaUnaFilaAltaEnElHistorial() {
        Equipo equipo = equipoService.crear(responsable.getId(), datos("BOM-001"));

        List<Historial> filas = historialService.consultar(TipoEntidad.EQUIPO, equipo.getId());

        assertThat(filas).hasSize(1);
        assertThat(filas.getFirst().getAccion()).isEqualTo(AccionHistorial.ALTA);
        assertThat(filas.getFirst().getValorAnterior()).isNull();
        assertThat(filas.getFirst().getValorNuevo()).isEqualTo("OPERATIVO");
        assertThat(filas.getFirst().getUsuario().getId()).isEqualTo(responsable.getId());  
    }

    @Test
        void rn05UnTecnicoNoPuedeDarDeAltaUnEquipo() {
            long antes = equipos.count();

            assertThatExceptionOfType(PermisoDenegadoException.class)
                    .isThrownBy(() -> equipoService.crear(tecnico.getId(), datos("BOM-001")))
                    .extracting(PermisoDenegadoException::getCodigo).isEqualTo("RN-05");

            assertThat(equipos.count()).isEqualTo(antes);
        }

        @Test
        void rn09NoSePuedenRepetirLosCodigosDeEquipo() {
            equipoService.crear(responsable.getId(), datos("BOM-001"));

            assertThatExceptionOfType(ReglaNegocioException.class)
                    .isThrownBy(() -> equipoService.crear(responsable.getId(), datos("BOM-001")))
                    .extracting(ReglaNegocioException::getCodigo).isEqualTo("RN-09");
        }

        @Test
        void unaUbicacionInexistenteDevuelveNoEncontrado() {
            DatosEquipo sinUbicacion = new DatosEquipo("BOM-001", "Bomba", null, null, null, 999_999L, null);

            assertThatExceptionOfType(RecursoNoEncontradoException.class)
                    .isThrownBy(() -> equipoService.crear(responsable.getId(), sinUbicacion))
                    .extracting(RecursoNoEncontradoException::getCodigo).isEqualTo("NO_ENCONTRADO");
        }

}
