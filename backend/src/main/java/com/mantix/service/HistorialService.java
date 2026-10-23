package com.mantix.service;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.mantix.domain.AccionHistorial;
import com.mantix.domain.Historial;
import com.mantix.domain.TipoEntidad;
import com.mantix.domain.Usuario;
import com.mantix.repository.HistorialRepository;

@Service 
public class HistorialService {
    
    private final HistorialRepository historial;
    private final Clock reloj;

    public HistorialService(HistorialRepository historial, Clock reloj) {
        this.historial = historial;
        this.reloj = reloj;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public Historial registrar(TipoEntidad tipoEntidad, Long entidadId, AccionHistorial accion,
        String valorAnterior, String valorNuevo, Historial origen, Usuario usuario, String comentario) {
            Historial fila = new Historial(tipoEntidad, entidadId, accion, valorAnterior, valorNuevo, origen, usuario, Instant.now(reloj), comentario);

            return historial.save(fila);
    }

    @Transactional (readOnly = true)
    public List<Historial> consultar(TipoEntidad tipoEntidad, Long entidadId) {
        return historial.findByTipoEntidadAndEntidadIdOrderByFechaAscIdAsc(tipoEntidad, entidadId);
    }
    
}
