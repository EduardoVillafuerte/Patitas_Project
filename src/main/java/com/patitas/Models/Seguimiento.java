package com.patitas.Models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import com.patitas.Models.Enums.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "seguimientos")
@Getter @Setter @NoArgsConstructor
public class Seguimiento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "postulacion_id")
    private PostulacionAdopcion postulacion;

    /** 30, 90 o 180 */
    private int diasPostAdopcion;

    private LocalDate fechaLimite;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EnumEstadoSeguimiento estado = EnumEstadoSeguimiento.PENDIENTE;

    @Column(length = 1000)
    private String comentario;

    @Lob
    private byte[] evidencia;
    private String evidenciaTipo;

    private LocalDateTime fechaEntrega;

    public boolean tieneEvidencia() {
        return evidencia != null && evidencia.length > 0;
    }
}
