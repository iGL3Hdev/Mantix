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
	private final EquipoService equipoService;
	private final AutorizacionService autorizacion;
	private final HistorialService historial;
	private final Clock reloj;

	public OrdenTrabajoService(OrdenTrabajoRepository ordenes, IncidenciaRepository incidencias,
			UsuarioRepository usuarios, EquipoService equipoService, AutorizacionService autorizacion, HistorialService historial,
			Clock reloj) {
		this.ordenes = ordenes;
		this.incidencias = incidencias;
		this.usuarios = usuarios;
		this.equipoService = equipoService;
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

	@Transactional
	public OrdenTrabajo asignar(Long usuarioId, Long ordenId, Long tecnicoId) {
		Usuario responsable = autorizacion.exigirResponsable(usuarioId);
		OrdenTrabajo orden = buscar(ordenId);
		exigirNoFinal(orden);
		if (orden.getEstado() != EstadoOrden.PENDIENTE) {
			throw new ReglaNegocioException("RN-01", "Solo se puede asignar una orden pendiente.");
		}
		Usuario tecnico = exigirTecnicoActivo(tecnicoId);
		EstadoOrden anterior = orden.getEstado();

		orden.asignar(tecnico);
		Historial cambio = historial.registrar(TipoEntidad.ORDEN, ordenId, AccionHistorial.CAMBIO_ESTADO,
				anterior.name(), orden.getEstado().name(), null, responsable, null);
		historial.registrar(TipoEntidad.ORDEN, ordenId, AccionHistorial.CAMBIO_TECNICO, null,
				String.valueOf(tecnico.getId()), cambio, responsable, null);
		return orden;
	}

	@Transactional
	public OrdenTrabajo reasignar(Long usuarioId, Long ordenId, Long tecnicoId) {
		Usuario responsable = autorizacion.exigirResponsable(usuarioId);
		OrdenTrabajo orden = buscar(ordenId);
		exigirNoFinal(orden);
		if (orden.getEstado() != EstadoOrden.ASIGNADA) {
			throw new ReglaNegocioException("RN-24", "Solo se puede cambiar de técnico una orden asignada.");
		}
		Usuario nuevo = exigirTecnicoActivo(tecnicoId);
		Long anteriorId = orden.getTecnico().getId();
		if (nuevo.getId().equals(anteriorId)) {
			throw new ReglaNegocioException("RN-24", "El nuevo técnico tiene que ser distinto del actual");
		}

		orden.cambiarTecnico(nuevo);
		historial.registrar(TipoEntidad.ORDEN, ordenId, AccionHistorial.CAMBIO_TECNICO,
			String.valueOf(anteriorId), String.valueOf(nuevo.getId()), null, responsable, null);
		
		return orden;
	}

	private OrdenTrabajo buscar(Long ordenId) {
		return ordenes.findById(ordenId)
			.orElseThrow(() -> new RecursoNoEncontradoException("La orden " + ordenId + " no existe."));
	}

	private void exigirNoFinal(OrdenTrabajo orden) {
		if (orden.getEstado() == EstadoOrden.CERRADA || orden.getEstado() == EstadoOrden.CANCELADA) {
			throw new ReglaNegocioException("RN-23", "Una orden cerrada o cancelada no cambia más.");
		}
	}

	@Transactional
	public OrdenTrabajo empezar(Long usuarioId, Long ordenId) {
		Usuario usuario = autorizacion.exigirActivo(usuarioId);
		OrdenTrabajo orden = buscar(ordenId);
		exigirNoFinal(orden);
		exigirTecnicoAsignado(orden, usuario);
		if (orden.getEstado() != EstadoOrden.ASIGNADA) {
			throw new ReglaNegocioException("RN-01", "Solo se puede empezar una orden asignada");
		}
		EstadoOrden anterior = orden.getEstado();

		orden.empezar(Instant.now(reloj));
		historial.registrar(TipoEntidad.ORDEN, ordenId, AccionHistorial.CAMBIO_ESTADO, anterior.name(),
			orden.getEstado().name(), null, usuario, null);

		return orden;
	}

	@Transactional
	public OrdenTrabajo resolver(Long usuarioId, Long ordenId, String resolucion) {
		Usuario usuario = autorizacion.exigirActivo(usuarioId);
		OrdenTrabajo orden = buscar(ordenId);
		exigirNoFinal(orden);
		exigirTecnicoAsignado(orden, usuario);
		if (orden.getEstado() != EstadoOrden.EN_CURSO) {
			throw new ReglaNegocioException("RN-01", "Solo se puede resolver una orden en curso");
		}
		EstadoOrden anterior = orden.getEstado();

		orden.resolver(resolucion, Instant.now(reloj));
		historial.registrar(TipoEntidad.ORDEN, ordenId, AccionHistorial.CAMBIO_ESTADO, anterior.name(),
			orden.getEstado().name(), null, usuario, resolucion);
		
		return orden;
	}

	private void exigirTecnicoAsignado(OrdenTrabajo orden, Usuario usuario) {
		if(orden.getTecnico() == null || !orden.getTecnico().getId().equals(usuario.getId())) {
			throw new PermisoDenegadoException("RN-06", "Solo el técnico asignado puede hacer esta acción.");
		}
	}

	@Transactional
	public OrdenTrabajo validar(Long usuarioId, Long ordenId) {
		Usuario responsable = autorizacion.exigirResponsable(usuarioId);
		OrdenTrabajo orden = buscar(ordenId);
		exigirNoFinal(orden);
		if (orden.getEstado() != EstadoOrden.RESUELTA) {
			throw new ReglaNegocioException("RN-01", "Solo se puede validar una orden resuelta");
		}
		EstadoOrden anterior = orden.getEstado();

		orden.validar();
		Historial validacion = historial.registrar(TipoEntidad.ORDEN, ordenId, AccionHistorial.CAMBIO_ESTADO, 
			anterior.name(), orden.getEstado().name(), null, responsable, null);
		
		Incidencia incidencia = orden.getIncidencia();
		EstadoIncidencia estadoIncidencia = incidencia.getEstado();
		incidencia.cerrar(Instant.now(reloj));
		historial.registrar(TipoEntidad.INCIDENCIA, incidencia.getId(), AccionHistorial.CAMBIO_ESTADO, 
			estadoIncidencia.name(), incidencia.getEstado().name(), validacion, responsable, null);
		equipoService.reevaluarAveria(incidencia.getEquipo(), responsable, validacion);
		
		return orden;
	}

	@Transactional
	public OrdenTrabajo rechazar(Long usuarioId, Long ordenId, String motivo) {
		Usuario responsable = autorizacion.exigirResponsable(usuarioId);
		OrdenTrabajo orden = buscar(ordenId);
		exigirNoFinal(orden);
		if (orden.getEstado() != EstadoOrden.RESUELTA) {
			throw new ReglaNegocioException("RN-01", "Solo se puede rechazar una orden resuelta.");
		}
		EstadoOrden anterior = orden.getEstado();

		orden.rechazar();
		historial.registrar(TipoEntidad.ORDEN, ordenId, AccionHistorial.CAMBIO_ESTADO, anterior.name(),
			orden.getEstado().name(), null, responsable, motivo);
		
		return orden;
	}

	@Transactional
	public OrdenTrabajo cancelar(Long usuarioId, Long ordenId, String motivo) {
		Usuario responsable = autorizacion.exigirResponsable(usuarioId);
		OrdenTrabajo orden = buscar(ordenId);
		exigirNoFinal(orden);
		if (orden.getEstado() == EstadoOrden.RESUELTA) {
			throw new ReglaNegocioException("RN-01", "Una orden resuelta no se cancela: se valida o se rechaza");
		}
		EstadoOrden anterior = orden.getEstado();

		orden.cancelar();
		Historial cancelacion = historial.registrar(TipoEntidad.ORDEN, ordenId, AccionHistorial.CAMBIO_ESTADO, 
			anterior.name(), orden.getEstado().name(), null, responsable, motivo);
		
		Incidencia incidencia = orden.getIncidencia();
		EstadoIncidencia estadoIncidencia = incidencia.getEstado();
		incidencia.volverAAbierta();
		historial.registrar(TipoEntidad.INCIDENCIA, incidencia.getId(), AccionHistorial.CAMBIO_ESTADO,
			estadoIncidencia.name(), incidencia.getEstado().name(), cancelacion, responsable, null);

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
