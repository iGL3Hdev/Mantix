package com.mantix.domain;

import java.time.Instant;

import org.hibernate.annotations.Immutable;

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
@Immutable 
@Table(name = "historial")
public class Historial {

    @Id 
    @GeneratedValue (strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column (name = "tipo_entidad", nullable = false, length = 20)
    private TipoEntidad tipoEntidad;

    @Column (name = "entidad_id", nullable = false)
    private Long entidadId;

    @Enumerated (EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AccionHistorial accion;

    @Column (name = "valor_anterior", length = 50)
    private String valorAnterior;

    @Column (name = "valor_nuevo", nullable = false, length = 50)
    private String valorNuevo;

    @ManyToOne (fetch = FetchType.LAZY)
    @JoinColumn (name = "origen_id")
    private Historial origen;

    @ManyToOne (fetch = FetchType.LAZY, optional = false)
    @JoinColumn (name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column (nullable = false)
    private Instant fecha;

    @Column (columnDefinition = "TEXT")
    private String comentario;

    protected Historial() {

    }

    public Historial(TipoEntidad tipoEntidad, Long entidadId, AccionHistorial accion, String valorAnterior, String valorNuevo, Historial origen, Usuario usuario, Instant fecha, String comentario) {
 
            this.tipoEntidad = tipoEntidad;
            this.entidadId = entidadId;
            this.accion = accion;
            this.valorAnterior = valorAnterior;
            this.valorNuevo = valorNuevo;
            this.origen = origen;
            this.usuario = usuario;
            this.fecha = fecha;
            this.comentario = comentario;
    }

    public Long getId() {
		return id;
	}

	public TipoEntidad getTipoEntidad() {
		return tipoEntidad;
	}

	public Long getEntidadId() {
		return entidadId;
	}

	public AccionHistorial getAccion() {
		return accion;
	}

	public String getValorAnterior() {
		return valorAnterior;
	}

	public String getValorNuevo() {
		return valorNuevo;
	}

	public Historial getOrigen() {
		return origen;
	}

	public Usuario getUsuario() {
		return usuario;
	}

	public Instant getFecha() {
		return fecha;
	}

	public String getComentario() {
		return comentario;
	}

}
