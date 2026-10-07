package com.patitas.Repository;

import com.patitas.Models.Enums.EnumEstadoMascota;
import com.patitas.Models.Mascota;
import com.patitas.Models.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MascotaRepository extends JpaRepository<Mascota, Long> {
    List<Mascota> findByActivoTrueAndEstado(EnumEstadoMascota estado);
    List<Mascota> findByRefugioAndActivoTrueOrderByIdDesc(Usuario refugio);
    List<Mascota> findByActivoTrueOrderByIdDesc();
    long countByActivoTrue();
    long countByEstado(EnumEstadoMascota estado);
}
