package com.mantix.domain;

import java.time.Instant;

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
@Table(name = "orden_trabajo")
public class OrdenTrabajo {

    @Id 
    @GeneratedValue (strategy=GenerationType.IDENTITY)
    private Long id;

    @ManyToOne (fetch = FetchType.LAZY, optional = false)
    @JoinColumn (name = "incidencia_id", nullable = false)
    private Incidencia incidencia;

    @ManyToOne (fetch = FetchType.LAZY, optional = false)
    @JoinColumn (name = "creada_por_id", nullable = false)
    private Usuario creadaPor;

    @ManyToOne (fetch = FetchType.LAZY)
    @JoinColumn (name = "tecnico_id")
    private Usuario tecnico;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String descripcion;

    @Column (columnDefinition = "TEXT")
    private String resolucion;

    @Enumerated (EnumType.STRING)
    @Column (nullable = false, length = 20)
    private EstadoOrden estado;

    @Column (name = "fecha_creacion", nullable = false)
    private Instant fechaCreacion;

    @Column (name = "fecha_inicio")
    private Instant fechaInicio;

    @Column (name = "fecha_fin")
    private Instant fechaFin;
    
    protected OrdenTrabajo() {

    }

    public OrdenTrabajo(Incidencia incidencia, Usuario creadaPor, Usuario tecnico, String descripcion, Instant fechaCreacion) {
        
        this.incidencia = incidencia;
        this.creadaPor = creadaPor;
        this.tecnico = tecnico;
        this.descripcion = descripcion;
        this.fechaCreacion = fechaCreacion;
        this.estado = tecnico == null ? EstadoOrden.PENDIENTE : EstadoOrden.ASIGNADA;

        }

    public Long getId() {
		return id;
	}

	public Incidencia getIncidencia() {
		return incidencia;
	}

	public Usuario getCreadaPor() {
		return creadaPor;
	}

	public Usuario getTecnico() {
		return tecnico;
	}

	public String getDescripcion() {
		return descripcion;
	}

	public String getResolucion() {
		return resolucion;
	}

	public EstadoOrden getEstado() {
		return estado;
	}

	public Instant getFechaCreacion() {
		return fechaCreacion;
	}

	public Instant getFechaInicio() {
		return fechaInicio;
	}

	public Instant getFechaFin() {
		return fechaFin;
	}

}
