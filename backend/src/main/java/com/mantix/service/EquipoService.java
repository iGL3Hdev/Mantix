package com.mantix.service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;

import com.mantix.domain.AccionHistorial;
import com.mantix.domain.Equipo;
import com.mantix.domain.EstadoEquipo;
import com.mantix.domain.EstadoIncidencia;
import com.mantix.domain.TipoEntidad;
import com.mantix.domain.Ubicacion;
import com.mantix.domain.Usuario;
import com.mantix.repository.EquipoRepository;
import com.mantix.repository.IncidenciaRepository;
import com.mantix.repository.UbicacionRepository;
import com.mantix.domain.Historial;
import com.mantix.domain.Prioridad;

@Service
public class EquipoService {

	private static final Set<EstadoIncidencia> SIN_CERRAR = EnumSet.of(EstadoIncidencia.ABIERTA,
			EstadoIncidencia.EN_CURSO);

	private final EquipoRepository equipos;
	private final UbicacionRepository ubicaciones;
	private final IncidenciaRepository incidencias;
	private final AutorizacionService autorizacion;
	private final HistorialService historial;
	private final Clock reloj;

	public EquipoService(EquipoRepository equipos, UbicacionRepository ubicaciones,
			IncidenciaRepository incidencias, AutorizacionService autorizacion, HistorialService historial,
			Clock reloj) {
		this.equipos = equipos;
		this.ubicaciones = ubicaciones;
		this.incidencias = incidencias;
		this.autorizacion = autorizacion;
		this.historial = historial;
		this.reloj = reloj;
	}

	@Transactional
	public Equipo crear(Long usuarioId, DatosEquipo datos) {
		Usuario usuario = autorizacion.exigirResponsable(usuarioId);
		if (equipos.existsByCodigo(datos.codigo())) {
			throw new ReglaNegocioException("RN-09", "Ya existe un equipo con el código " + datos.codigo() + ".");
		}
		Ubicacion ubicacion = buscarUbicacion(datos.ubicacionId());
		LocalDate fechaAlta = datos.fechaAlta() != null ? datos.fechaAlta() : LocalDate.now(reloj);

		Equipo equipo = equipos.save(new Equipo(datos.codigo(), datos.nombre(), datos.fabricante(),
				datos.modelo(), datos.numeroSerie(), ubicacion, fechaAlta));
		historial.registrar(TipoEntidad.EQUIPO, equipo.getId(), AccionHistorial.ALTA, null,
				equipo.getEstado().name(), null, usuario, null);
		return equipo;
	}

	@Transactional
	public Equipo editar(Long usuarioId, Long equipoId, CambiosEquipo cambios) {
		autorizacion.exigirResponsable(usuarioId);
		Equipo equipo = buscar(equipoId);
		if (equipo.getEstado() == EstadoEquipo.BAJA) {
			throw new ReglaNegocioException("RN-14", "Un equipo de baja no se puede editar.");
		}
		Ubicacion ubicacion = buscarUbicacion(cambios.ubicacionId());

		equipo.actualizarDatos(cambios.nombre(), cambios.fabricante(), cambios.modelo(), cambios.numeroSerie(),
				ubicacion);
		return equipo;
	}

	@Transactional
	public Equipo darDeBaja(Long usuarioId, Long equipoId, String motivo) {
		Usuario usuario = autorizacion.exigirResponsable(usuarioId);
		Equipo equipo = buscar(equipoId);
		if (equipo.getEstado() == EstadoEquipo.BAJA) {
			throw new ReglaNegocioException("RN-01", "El equipo " + equipoId + " ya está de baja.");
		}
		if (incidencias.existsByEquipoIdAndEstadoIn(equipoId, SIN_CERRAR)) {
			throw new ReglaNegocioException("RN-12", "El equipo " + equipoId + " tiene incidencias sin cerrar.");
		}
		EstadoEquipo anterior = equipo.getEstado();

		equipo.darDeBaja();
		historial.registrar(TipoEntidad.EQUIPO, equipoId, AccionHistorial.CAMBIO_ESTADO, anterior.name(),
				equipo.getEstado().name(), null, usuario, motivo);
		return equipo;
	}

	@Transactional
	public Equipo reactivar(Long usuarioId, Long equipoId, String motivo) {
		Usuario usuario = autorizacion.exigirResponsable(usuarioId);
		Equipo equipo = buscar(equipoId);
		if (equipo.getEstado() != EstadoEquipo.BAJA) {
			throw new ReglaNegocioException("RN-01", "Solo se puede reactivar un equipo de baja.");
		}
		EstadoEquipo anterior = equipo.getEstado();

		equipo.reactivar();
		historial.registrar(TipoEntidad.EQUIPO, equipoId, AccionHistorial.CAMBIO_ESTADO, anterior.name(),
				equipo.getEstado().name(), null, usuario, motivo);
		return equipo;
	}

	@Transactional (propagation = Propagation.MANDATORY)
	public void reevaluarAveria(Equipo equipo, Usuario usuario, Historial origen) {
		if(equipo.getEstado() == EstadoEquipo.BAJA) {
			return;
		}
		boolean hayCritica = incidencias.existsByEquipoIdAndPrioridadAndEstadoIn(equipo.getId(),
						Prioridad.CRITICA,  SIN_CERRAR);
		EstadoEquipo anterior = equipo.getEstado();

		if(hayCritica && anterior == EstadoEquipo.OPERATIVO) {
			equipo.marcarAveriado();
		} else if (!hayCritica && anterior == EstadoEquipo.AVERIADO) {
			equipo.marcarAveriado();
		} else {
			return;
		}
		historial.registrar(TipoEntidad.EQUIPO, equipo.getId(), AccionHistorial.CAMBIO_ESTADO, anterior.name(), 
					equipo.getEstado().name(), origen, usuario, null);
	}

	private Equipo buscar(Long equipoId) {
		return equipos.findById(equipoId)
				.orElseThrow(() -> new RecursoNoEncontradoException("El equipo " + equipoId + " no existe."));
	}

	private Ubicacion buscarUbicacion(Long ubicacionId) {
		return ubicaciones.findById(ubicacionId)
				.orElseThrow(() -> new RecursoNoEncontradoException("La ubicación " + ubicacionId + " no existe."));
	}

}


