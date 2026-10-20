package com.mantix.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.mantix.domain.Usuario;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    
}
