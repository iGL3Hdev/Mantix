package com.mantix.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

import com.mantix.TestcontainersConfiguration;
import com.mantix.domain.Perfil;
import com.mantix.domain.Usuario;
import com.mantix.repository.UsuarioRepository;

@Import(TestcontainersConfiguration.class)
@SpringBootTest 
@Transactional 
public class AutorizacionServiceTest {
    
    @Autowired 
    private AutorizacionService autorizacion;

    @Autowired
    private UsuarioRepository usuarios;

    private Usuario usuario(Perfil perfil, boolean activo) {
        return usuarios.findAll().stream()
            .filter(u -> u.getPerfil() == perfil && u.isActivo() == activo) 
            .findFirst().orElseThrow();
    }

    @Test
    void sinUusarioDevuelveUsuarioNoIdentificado() {
        assertThatExceptionOfType(UsuarioNoIdentificadoException.class)
            .isThrownBy(() -> autorizacion.exigirActivo(null))
            .extracting(UsuarioNoIdentificadoException::getCodigo).isEqualTo("USUARIO_NO_IDENTIFICADO");
    } 

    @Test 
    void unUsuarioQueNoExisteDevuelveUsuarioNoIdentificado() {
        assertThatExceptionOfType(UsuarioNoIdentificadoException.class)
            .isThrownBy(() -> autorizacion.exigirActivo(999_999L));
    }

    @Test 
    void rn04UnUsuarioInactivoNoPuedeActuar() {
        Long inactivo = usuario(Perfil.TECNICO, false).getId();

        assertThatExceptionOfType(PermisoDenegadoException.class)
            .isThrownBy(() -> autorizacion.exigirActivo(inactivo))
            .extracting(PermisoDenegadoException::getCodigo).isEqualTo("RN-04");
    }

    @Test
    void rn05UnTecnicoNoPuedeHacerAccionesDelResponsable() {
        Long tecnico = usuario(Perfil.TECNICO, true).getId();

        assertThatExceptionOfType(PermisoDenegadoException.class)
            .isThrownBy(() -> autorizacion.exigirResponsable(tecnico))
            .extracting(PermisoDenegadoException::getCodigo).isEqualTo("RN-05");
    }

    @Test 
    void rn05ElResponsableActivoSiPuede() {
        Usuario responsable = usuario(Perfil.RESPONSABLE, true);

        assertThat(autorizacion.exigirResponsable(responsable.getId())).isEqualTo(responsable);
    }

 }
