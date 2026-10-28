package com.mantix.service;

import java.time.Clock;
import java.time.Instant;
import java.util.EnumSet;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mantix.domain.AccionHistorial;
import com.mantix.domain.EstadoIncidencia;
import com.mantix.domain.EstadoOrden;
import com.mantix.domain.Historial;
import com.mantix.domain.Incidencia;
import com.mantix.domain.OrdenTrabajo;
import com.mantix.domain.Perfil;
import com.mantix.domain.TipoEntidad;
import com.mantix.domain.Usuario;
import com.mantix.repository.IncidenciaRepository;
import com.mantix.repository.OrdenTrabajoRepository;
import com.mantix.repository.UsuarioRepository;

@Service
public class OrdenTrabajoService {

	private static final Set<EstadoOrden> SIN_CERRAR = EnumSet.of(EstadoOrden.PENDIENTE, EstadoOrden.ASIGNADA,
			EstadoOrden.EN_CURSO, EstadoOrden.RESUELTA);

	private final OrdenTrabajoRepository ordenes;
	private final IncidenciaRepository incidencias;
	private final UsuarioRepository usuarios;
	private final AutorizacionService autorizacion;
	private final HistorialService historial;
	private final Clock reloj;

	public OrdenTrabajoService(OrdenTrabajoRepository ordenes, IncidenciaRepository incidencias,
			UsuarioRepository usuarios, AutorizacionService autorizacion, HistorialService historial,
			Clock reloj) {
		this.ordenes = ordenes;
		this.incidencias = incidencias;
		this.usuarios = usuarios;
		this.autorizacion = autorizacion;
		this.historial = historial;
		this.reloj = reloj;
	}

	@Transactional
	public OrdenTrabajo crear(Long usuarioId, DatosOrden datos) {
		Usuario responsable = autorizacion.exigirResponsable(usuarioId);
		Incidencia incidencia = incidencias.findById(datos.incidenciaId()).orElseThrow(
				() -> new RecursoNoEncontradoException("La incidencia " + datos.incidenciaId() + " no existe."));
		if (incidencia.getEstado() != EstadoIncidencia.ABIERTA) {
			throw new ReglaNegocioException("RN-19", "Solo se puede crear una orden para una incidencia abierta.");
		}
		if (ordenes.existsByIncidenciaIdAndEstadoIn(incidencia.getId(), SIN_CERRAR)) {
			throw new ReglaNegocioException("RN-20", "La incidencia " + incidencia.getId()
					+ " ya tiene una orden sin cerrar.");
		}
		Usuario tecnico = datos.tecnicoId() != null ? exigirTecnicoActivo(datos.tecnicoId()) : null;

		OrdenTrabajo orden = ordenes.save(new OrdenTrabajo(incidencia, responsable, tecnico,
				datos.descripcion(), Instant.now(reloj)));
		Historial alta = historial.registrar(TipoEntidad.ORDEN, orden.getId(), AccionHistorial.ALTA, null,
				orden.getEstado().name(), null, responsable, null);
		if (tecnico != null) {
			historial.registrar(TipoEntidad.ORDEN, orden.getId(), AccionHistorial.CAMBIO_TECNICO, null,
					String.valueOf(tecnico.getId()), alta, responsable, null);
		}
		incidencia.ponerEnCurso();
		historial.registrar(TipoEntidad.INCIDENCIA, incidencia.getId(), AccionHistorial.CAMBIO_ESTADO,
				EstadoIncidencia.ABIERTA.name(), incidencia.getEstado().name(), alta, responsable, null);
		return orden;
	}

	private Usuario exigirTecnicoActivo(Long tecnicoId) {
		Usuario tecnico = usuarios.findById(tecnicoId)
				.orElseThrow(() -> new RecursoNoEncontradoException("El usuario " + tecnicoId + " no existe."));
		if (tecnico.getPerfil() != Perfil.TECNICO) {
			throw new PermisoDenegadoException("RN-07", "Una orden solo se puede asignar a un técnico.");
		}
		if (!tecnico.isActivo()) {
			throw new PermisoDenegadoException("RN-04", "El usuario " + tecnicoId + " no está activo");
		}
		return tecnico;
	}

}
