package com.patitas.Models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/** Feria de adopción publicada por un refugio. */
@Entity
@Table(name = "ferias")
@Getter @Setter @NoArgsConstructor
public class Feria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "refugio_id")
    private Usuario refugio;

    @Column(nullable = false, length = 100)
    private String titulo;

    @Column(length = 1000)
    private String descripcion;

    @Column(nullable = false)
    private LocalDate fecha;

    @Column(nullable = false)
    private LocalTime horaInicio;

    private LocalTime horaFin;

    @Column(nullable = false, length = 200)
    private String lugar;

    /** Máximo de inscritos. null = sin límite. */
    private Integer cupo;

    /** false = cancelada por el refugio. */
    private boolean activa = true;

    private LocalDateTime creadaEn = LocalDateTime.now();
}
