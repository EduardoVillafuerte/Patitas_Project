package com.patitas.Repository;

import com.patitas.Models.Notificacion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificacionRepository extends JpaRepository<Notificacion, Long> {
    List<Notificacion> findTop50ByUsuarioIdOrderByFechaDesc(Long usuarioId);
    List<Notificacion> findByUsuarioIdAndLeidaFalse(Long usuarioId);
    long countByUsuarioIdAndLeidaFalse(Long usuarioId);
    long countByUsuarioEmailAndLeidaFalse(String email);
    /** Evita llenar la campanita con un aviso por cada mensaje del chat: uno sin leer por conversación basta. */
    boolean existsByUsuarioIdAndUrlAndLeidaFalse(Long usuarioId, String url);
}
