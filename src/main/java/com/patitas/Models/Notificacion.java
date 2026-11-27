package com.patitas.Models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/** Aviso dentro de la página (campanita). Cada cambio importante guarda uno para el usuario que corresponde. */
@Entity
@Table(name = "notificaciones")
@Getter @Setter @NoArgsConstructor
public class Notificacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    @Column(nullable = false, length = 150)
    private String titulo;

    @Column(length = 600)
    private String mensaje;

    /** Ruta interna a la que lleva el aviso, p. ej. /adoptante/postulaciones. */
    @Column(length = 200)
    private String url;

    private boolean leida;

    private LocalDateTime fecha = LocalDateTime.now();
}
