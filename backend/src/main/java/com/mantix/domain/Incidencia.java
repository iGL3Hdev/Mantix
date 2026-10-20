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
@Table(name = "incidencia")
public class Incidencia {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "equipo_id", nullable = false)
	private Equipo equipo;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "reportada_por_id", nullable = false)
	private Usuario reportadaPor;

	@Column(nullable = false, length = 150)
	private String titulo;

	@Column(nullable = false, columnDefinition = "TEXT")
	private String descripcion;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private Prioridad prioridad;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private EstadoIncidencia estado;

	@Column(name = "fecha_apertura", nullable = false)
	private Instant fechaApertura;

	@Column(name = "fecha_cierre")
	private Instant fechaCierre;

	protected Incidencia() {
	}

	public Incidencia(Equipo equipo, Usuario reportadaPor, String titulo, String descripcion,
			Prioridad prioridad, Instant fechaApertura) {
		this.equipo = equipo;
		this.reportadaPor = reportadaPor;
		this.titulo = titulo;
		this.descripcion = descripcion;
		this.prioridad = prioridad;
		this.fechaApertura = fechaApertura;
		this.estado = EstadoIncidencia.ABIERTA;
	}

	public Long getId() {
		return id;
	}

	public Equipo getEquipo() {
		return equipo;
	}

	public Usuario getReportadaPor() {
		return reportadaPor;
	}

	public String getTitulo() {
		return titulo;
	}

	public String getDescripcion() {
		return descripcion;
	}

	public Prioridad getPrioridad() {
		return prioridad;
	}

	public EstadoIncidencia getEstado() {
		return estado;
	}

	public Instant getFechaApertura() {
		return fechaApertura;
	}

	public Instant getFechaCierre() {
		return fechaCierre;
	}

}

