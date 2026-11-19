package com.patitas.Repository;

import com.patitas.Models.Mensaje;
import com.patitas.Models.PostulacionAdopcion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MensajeRepository extends JpaRepository<Mensaje, Long> {
    List<Mensaje> findByPostulacionAndIdGreaterThanOrderByIdAsc(PostulacionAdopcion postulacion, Long id);
}
