package com.patitas.Repository;

import com.patitas.Models.Feria;
import com.patitas.Models.InscripcionFeria;
import com.patitas.Models.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InscripcionFeriaRepository extends JpaRepository<InscripcionFeria, Long> {
    boolean existsByFeriaAndAdoptante(Feria feria, Usuario adoptante);
    Optional<InscripcionFeria> findByFeriaAndAdoptante(Feria feria, Usuario adoptante);
    long countByFeria(Feria feria);
    List<InscripcionFeria> findByFeriaOrderByFechaAsc(Feria feria);
}
