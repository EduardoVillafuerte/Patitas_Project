package com.patitas.Models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/** Un mensaje del chat de una postulación (entre el adoptante y el refugio). */
@Entity
@Table(name = "mensajes_chat", indexes = @Index(name = "idx_mensaje_postulacion", columnList = "postulacion_id"))
@Getter @Setter @NoArgsConstructor
public class Mensaje {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "postulacion_id")
    private PostulacionAdopcion postulacion;

    @ManyToOne(optional = false)
    @JoinColumn(name = "autor_id")
    private Usuario autor;

    @Column(nullable = false, length = 1000)
    private String contenido;

    private LocalDateTime fecha = LocalDateTime.now();
}
