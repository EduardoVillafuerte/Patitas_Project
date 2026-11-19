package com.patitas.Repository;

import com.patitas.Models.Feria;
import com.patitas.Models.Usuario;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface FeriaRepository extends JpaRepository<Feria, Long> {

    List<Feria> findByActivaTrueAndFechaGreaterThanEqualOrderByFechaAscHoraInicioAsc(LocalDate desde);

    List<Feria> findByRefugioOrderByFechaDesc(Usuario refugio);

    /** Bloquea la fila mientras dura la transacción: evita pasarse del cupo si dos personas se inscriben a la vez. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select f from Feria f where f.id = :id")
    Optional<Feria> buscarParaInscribir(@Param("id") Long id);
}
