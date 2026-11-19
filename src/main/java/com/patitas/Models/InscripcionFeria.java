package com.patitas.Models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/** Inscripción de un adoptante a una feria. Una persona solo se puede inscribir una vez por feria. */
@Entity
@Table(name = "inscripciones_feria",
        uniqueConstraints = @UniqueConstraint(name = "uk_inscripcion_feria_adoptante", columnNames = {"feria_id", "adoptante_id"}))
@Getter @Setter @NoArgsConstructor
public class InscripcionFeria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "feria_id")
    private Feria feria;

    @ManyToOne(optional = false)
    @JoinColumn(name = "adoptante_id")
    private Usuario adoptante;

    private LocalDateTime fecha = LocalDateTime.now();
}
