package com.mantix.web.dto;

import java.time.Instant;

import com.mantix.domain.AccionHistorial;
import com.mantix.domain.Historial;
import com.mantix.domain.TipoEntidad;

public record EntradaHistorialResponse(Long id, TipoEntidad tipoEntidad, Long entidadId, AccionHistorial accion,
		String valorAnterior, String valorNuevo, Long origenId, ResumenUsuario usuario, Instant fecha,
		String comentario) {

	public record ResumenUsuario(Long id, String nombre) {
	}

	public static EntradaHistorialResponse de(Historial entrada) {
		return new EntradaHistorialResponse(entrada.getId(), entrada.getTipoEntidad(), entrada.getEntidadId(),
				entrada.getAccion(), entrada.getValorAnterior(), entrada.getValorNuevo(),
				entrada.getOrigen() != null ? entrada.getOrigen().getId() : null,
				new ResumenUsuario(entrada.getUsuario().getId(), entrada.getUsuario().getNombre()),
				entrada.getFecha(), entrada.getComentario());
	}

}
