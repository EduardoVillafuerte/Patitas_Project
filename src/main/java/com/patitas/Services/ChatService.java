package com.patitas.Services;

import com.patitas.dto.MensajeDto;
import com.patitas.exception.BusinessException;
import com.patitas.Models.Enums.EnumEstadoPostulacion;
import com.patitas.Models.Mensaje;
import com.patitas.Models.PostulacionAdopcion;
import com.patitas.Models.Usuario;
import com.patitas.Repository.MensajeRepository;
import com.patitas.Repository.PostulacionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Chat de una postulación. Solo pueden entrar las dos partes: el adoptante que postuló
 * y el refugio dueño de la mascota. Si la postulación se rechaza, el chat queda en solo lectura.
 */
@Service
@RequiredArgsConstructor
public class ChatService {

    public static final int MAX_CARACTERES = 1000;
    private static final DateTimeFormatter FORMATO = DateTimeFormatter.ofPattern("dd/MM HH:mm");

    private final MensajeRepository mensajes;
    private final PostulacionRepository postulaciones;
    private final NotificacionService notificaciones;

    /** Devuelve la postulación si el usuario es una de las dos partes; si no, "no encontrada" (sin dar pistas). */
    public PostulacionAdopcion acceso(Usuario u, Long postulacionId) {
        PostulacionAdopcion p = postulaciones.findById(postulacionId)
                .orElseThrow(() -> new BusinessException("Postulación no encontrada."));
        boolean esAdoptante = p.getAdoptante().getId().equals(u.getId());
        boolean esRefugio = p.getMascota().getRefugio().getId().equals(u.getId());
        if (!esAdoptante && !esRefugio) {
            throw new BusinessException("Postulación no encontrada.");
        }
        return p;
    }

    public boolean estaCerrado(PostulacionAdopcion p) {
        return p.getEstado() == EnumEstadoPostulacion.RECHAZADA;
    }

    @Transactional(readOnly = true)
    public List<MensajeDto> listar(Usuario u, Long postulacionId, long desdeId) {
        PostulacionAdopcion p = acceso(u, postulacionId);
        return mensajes.findByPostulacionAndIdGreaterThanOrderByIdAsc(p, desdeId).stream()
                .map(m -> aDto(m, u))
                .toList();
    }

    @Transactional
    public MensajeDto enviar(Usuario u, Long postulacionId, String texto) {
        PostulacionAdopcion p = acceso(u, postulacionId);
        if (!u.isActivo()) {
            throw new BusinessException("Tu cuenta no puede enviar mensajes.");
        }
        if (estaCerrado(p)) {
            throw new BusinessException("Esta postulación fue rechazada: el chat quedó cerrado.");
        }
        String limpio = texto == null ? "" : texto.trim();
        if (limpio.isEmpty()) {
            throw new BusinessException("Escribe un mensaje.");
        }
        if (limpio.length() > MAX_CARACTERES) {
            throw new BusinessException("El mensaje es demasiado largo (máximo " + MAX_CARACTERES + " caracteres).");
        }
        Mensaje m = new Mensaje();
        m.setPostulacion(p);
        m.setAutor(u);
        m.setContenido(limpio);
        Mensaje guardado = mensajes.save(m);
        notificaciones.nuevoMensajeChat(p, u);
        return aDto(guardado, u);
    }

    private MensajeDto aDto(Mensaje m, Usuario yo) {
        return new MensajeDto(m.getId(), m.getAutor().getNombre(),
                m.getAutor().getId().equals(yo.getId()), m.getContenido(), m.getFecha().format(FORMATO));
    }
}
