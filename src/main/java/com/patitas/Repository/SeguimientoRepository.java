package com.patitas.Repository;

import com.patitas.Models.Enums.EnumEstadoSeguimiento;
import com.patitas.Models.Seguimiento;
import com.patitas.Models.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface SeguimientoRepository extends JpaRepository<Seguimiento, Long> {
    List<Seguimiento> findByPostulacionAdoptanteOrderByFechaLimiteAsc(Usuario adoptante);
    List<Seguimiento> findByPostulacionMascotaRefugioOrderByFechaLimiteAsc(Usuario refugio);
    List<Seguimiento> findByEstadoAndFechaLimiteBefore(EnumEstadoSeguimiento estado, LocalDate fecha);
    long countByEstado(EnumEstadoSeguimiento estado);
}
