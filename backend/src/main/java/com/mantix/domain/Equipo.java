package com.mantix.domain;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "equipo")
public class Equipo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 20, updatable = false)
    private String codigo;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(length = 100)
    private String fabricante;

    @Column(length = 100)
    private String modelo;

    @Column(name = "numero_serie", length = 100)
    private String numeroSerie;
    
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ubicacion_id", nullable = false)
    private Ubicacion ubicacion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoEquipo estado;

    @Column(name = "fecha_alta", nullable = false)
    private LocalDate fechaAlta;

    protected Equipo(){

    }

    public Equipo(String codigo, String nombre, Ubicacion ubicacion, LocalDate fechaAlta) {
        this(codigo, nombre, null, null, null, ubicacion, fechaAlta);
    }

    public Equipo(String codigo, String nombre, String fabricante, String modelo, String numeroSerie,
            Ubicacion ubicacion, LocalDate fechaAlta) {
        this.codigo = codigo;
        this.nombre = nombre;
        this.fabricante = fabricante;
        this.modelo = modelo;
        this.numeroSerie = numeroSerie;
        this.ubicacion = ubicacion;
        this.fechaAlta = fechaAlta;
        this.estado = EstadoEquipo.OPERATIVO;
    }

        public void actualizarDatos(String nombre, String fabricante, String modelo, String numeroSerie,
            Ubicacion ubicacion) {
        this.nombre = nombre;
        this.fabricante = fabricante;
        this.modelo = modelo;
        this.numeroSerie = numeroSerie;
        this.ubicacion = ubicacion;
    }

    public void darDeBaja() {
        this.estado = EstadoEquipo.BAJA;
    }

    public void reactivar() {
        this.estado = EstadoEquipo.OPERATIVO;
    }

    public Long getId() {
        return id;
    }

    public String getCodigo() {
        return codigo;
    }

    public String getNombre() {
        return nombre;
    }

    public String getFabricante() {
        return fabricante;
    }

    public String getModelo() {
        return modelo;
    }

    public String getNumeroSerie() {
        return numeroSerie;
    }

    public Ubicacion getUbicacion() {
        return ubicacion;
    }

    public EstadoEquipo getEstado() {
        return estado;
    }

    public LocalDate getFechaAlta() {
        return fechaAlta;
    }
}
