package com.patitas.Services;

import com.patitas.exception.BusinessException;
import com.patitas.Models.*;
import com.patitas.Models.Enums.*;
import com.patitas.Repository.MascotaRepository;
import com.patitas.Repository.PostulacionRepository;
import com.patitas.Repository.SeguimientoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdopcionService {

    public static final int[] CRONOGRAMA_DIAS = {30, 90, 180};

    private final PostulacionRepository postulaciones;
    private final MascotaRepository mascotas;
    private final SeguimientoRepository seguimientos;
    private final CompatibilidadService compatibilidad;

    // bloqueo por lista negra
    @Transactional
    public PostulacionAdopcion postular(Usuario adoptante, Long mascotaId, String mensaje) {
        if (adoptante.getEstado() == EnumEstadoCuenta.BANNED) {
            throw new BusinessException("Tu cuenta está en la lista negra: no puedes postular a adopciones.");
        }
        if (adoptante.getEstado() != EnumEstadoCuenta.ACTIVO) {
            throw new BusinessException("Tu cuenta aún no está activa.");
        }
        if (!adoptante.isPerfilCompleto()) {
            throw new BusinessException("Completa tu perfil de estilo de vida antes de postular.");
        }
        Mascota m = mascotas.findById(mascotaId)
                .filter(Mascota::isActivo)
                .orElseThrow(() -> new BusinessException("La mascota no existe."));
        if (m.getEstado() != EnumEstadoMascota.DISPONIBLE) {
            throw new BusinessException("Esta mascota ya no está disponible.");
        }
        boolean yaPostulo = postulaciones.existsByAdoptanteAndMascotaAndEstadoIn(
                adoptante, m, List.of(EnumEstadoPostulacion.PENDIENTE, EnumEstadoPostulacion.APROBADA));
        if (yaPostulo) {
            throw new BusinessException("Ya tienes una postulación en curso para esta mascota.");
        }

        PostulacionAdopcion p = new PostulacionAdopcion();
        p.setAdoptante(adoptante);
        p.setMascota(m);
        p.setMensaje(mensaje);
        p.setScoreAlPostular(compatibilidad.evaluar(adoptante, m).getScore());
        return postulaciones.save(p);
    }

    // REFUGIO: gestionar postulaciones
    @Transactional
    public void aprobar(Usuario refugio, Long postulacionId) {
        PostulacionAdopcion p = propia(refugio, postulacionId);
        if (p.getEstado() != EnumEstadoPostulacion.PENDIENTE) {
            throw new BusinessException("Solo se pueden aprobar postulaciones pendientes.");
        }
        if (p.getAdoptante().isBaneado()) {
            throw new BusinessException("El adoptante está en la lista negra.");
        }
        Mascota m = p.getMascota();
        if (m.getEstado() != EnumEstadoMascota.DISPONIBLE) {
            throw new BusinessException("La mascota ya tiene un proceso en curso.");
        }
        p.setEstado(EnumEstadoPostulacion.APROBADA);
        m.setEstado(EnumEstadoMascota.EN_PROCESO);

        // El resto de postulaciones pendientes para la misma mascota se rechazan solas.
        for (PostulacionAdopcion otra : postulaciones.findByMascotaAndEstado(m, EnumEstadoPostulacion.PENDIENTE)) {
            if (!otra.getId().equals(p.getId())) {
                otra.setEstado(EnumEstadoPostulacion.RECHAZADA);
                otra.setMotivoRechazo("Se aprobó otra postulación para esta mascota.");
            }
        }
    }

    @Transactional
    public void rechazar(Usuario refugio, Long postulacionId, String motivo) {
        PostulacionAdopcion p = propia(refugio, postulacionId);
        if (p.getEstado() != EnumEstadoPostulacion.PENDIENTE && p.getEstado() != EnumEstadoPostulacion.APROBADA) {
            throw new BusinessException("Esta postulación ya no se puede rechazar.");
        }
        if (p.getEstado() == EnumEstadoPostulacion.APROBADA) {
            p.getMascota().setEstado(EnumEstadoMascota.DISPONIBLE);
        }
        p.setEstado(EnumEstadoPostulacion.RECHAZADA);
        p.setMotivoRechazo(motivo == null || motivo.isBlank() ? "Sin motivo indicado" : motivo.trim());
    }

    /*Cierra la adopción y genera automáticamente el cronograma de seguimiento 30, 90 y 180 días).*/
    @Transactional
    public void completar(Usuario refugio, Long postulacionId) {
        PostulacionAdopcion p = propia(refugio, postulacionId);
        if (p.getEstado() != EnumEstadoPostulacion.APROBADA) {
            throw new BusinessException("Primero hay que aprobar la postulación.");
        }
        LocalDate hoy = LocalDate.now();
        p.setEstado(EnumEstadoPostulacion.COMPLETADA);
        p.setFechaCompletada(hoy);
        p.getMascota().setEstado(EnumEstadoMascota.ADOPTADO);

        for (int dias : CRONOGRAMA_DIAS) {
            Seguimiento s = new Seguimiento();
            s.setPostulacion(p);
            s.setDiasPostAdopcion(dias);
            s.setFechaLimite(hoy.plusDays(dias));
            p.getSeguimientos().add(s);
        }
    }

    // ADOPTANTE: subir evidencia de seguimiento
    @Transactional
    public void entregarEvidencia(Usuario adoptante, Long seguimientoId, String comentario, MultipartFile foto) {
        Seguimiento s = seguimientos.findById(seguimientoId)
                .filter(x -> x.getPostulacion().getAdoptante().getId().equals(adoptante.getId()))
                .orElseThrow(() -> new BusinessException("Seguimiento no encontrado."));
        if (s.getEstado() == EnumEstadoSeguimiento.ENTREGADO) {
            throw new BusinessException("Este seguimiento ya fue entregado.");
        }
        boolean hayTexto = comentario != null && !comentario.isBlank();
        boolean hayFoto = foto != null && !foto.isEmpty();
        if (!hayTexto && !hayFoto) {
            throw new BusinessException("Agrega un comentario o una foto como evidencia.");
        }
        if (hayFoto) {
            String tipo = foto.getContentType();
            if (tipo == null || !tipo.startsWith("image/")) {
                throw new BusinessException("La evidencia debe ser una imagen.");
            }
            try {
                s.setEvidencia(foto.getBytes());
                s.setEvidenciaTipo(tipo);
            } catch (IOException e) {
                throw new BusinessException("No se pudo leer la imagen.");
            }
        }
        s.setComentario(comentario);
        s.setFechaEntrega(LocalDateTime.now());
        s.setEstado(EnumEstadoSeguimiento.ENTREGADO);
    }

    // ALERTAS: pendientes con fecha límite cumplida pasan a VENCIDO
    @Transactional
    public int marcarVencidos() {
        List<Seguimiento> vencidos = seguimientos
                .findByEstadoAndFechaLimiteBefore(EnumEstadoSeguimiento.PENDIENTE, LocalDate.now());
        vencidos.forEach(s -> s.setEstado(EnumEstadoSeguimiento.VENCIDO));
        return vencidos.size();
    }

    private PostulacionAdopcion propia(Usuario refugio, Long id) {
        return postulaciones.findById(id)
                .filter(p -> p.getMascota().getRefugio().getId().equals(refugio.getId()))
                .orElseThrow(() -> new BusinessException("Postulación no encontrada."));
    }
}
