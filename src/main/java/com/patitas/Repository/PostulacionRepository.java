package com.patitas.Repository;

import com.patitas.Models.*;
import org.springframework.data.jpa.repository.JpaRepository;
import com.patitas.Models.Enums.*;

import java.util.Collection;
import java.util.List;

public interface PostulacionRepository extends JpaRepository<PostulacionAdopcion, Long> {
    List<PostulacionAdopcion> findByAdoptanteOrderByFechaDesc(Usuario adoptante);
    List<PostulacionAdopcion> findByMascotaRefugioOrderByFechaDesc(Usuario refugio);
    List<PostulacionAdopcion> findByMascotaAndEstado(Mascota mascota, EnumEstadoPostulacion estado);
    boolean existsByAdoptanteAndMascotaAndEstadoIn(Usuario adoptante, Mascota mascota, Collection<EnumEstadoPostulacion> estados);
    long countByEstado(EnumEstadoPostulacion estado);
    long countByMascotaRefugioAndEstado(Usuario refugio, EnumEstadoPostulacion estado);
}
