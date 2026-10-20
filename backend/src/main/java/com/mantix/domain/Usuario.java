package com.mantix.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity 
@Table (name = "usuario")
public class Usuario {
    
    @Id 
    @GeneratedValue (strategy = GenerationType.IDENTITY)
    private Long id;

    @Column (nullable = false, length = 100)
    private String nombre;
    
    @Column (nullable = false, length = 150)
    private String email;

    @Enumerated (EnumType.STRING)
    @Column (nullable = false, length = 20)
    private Perfil perfil;

    @Column (nullable = false)
    private boolean activo;

    protected Usuario(){

    }

    public Usuario(String nombre, String email, Perfil perfil) {
        this.nombre = nombre;
        this.email = email;
        this.perfil = perfil;
        this.activo = true;
    }

    public Long getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public String getEmail() {
        return email;
    
    }

    public Perfil getPerfil() {
        return perfil;
    }

    public boolean isActivo() {
        return activo;
    }

}
