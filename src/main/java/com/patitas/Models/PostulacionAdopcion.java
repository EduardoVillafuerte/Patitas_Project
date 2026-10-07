package com.patitas.Models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import com.patitas.Models.Enums.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "postulaciones_adopcion")
@Getter @Setter @NoArgsConstructor
public class PostulacionAdopcion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "adoptante_id")
    private Usuario adoptante;

    @ManyToOne(optional = false)
    @JoinColumn(name = "mascota_id")
    private Mascota mascota;

    private LocalDateTime fecha = LocalDateTime.now();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EnumEstadoPostulacion estado = EnumEstadoPostulacion.PENDIENTE;

    @Column(length = 1000)
    private String mensaje;

    private String motivoRechazo;

    /** Afinidad calculada al momento de postular (0-100). */
    private int scoreAlPostular;

    private LocalDate fechaCompletada;

    @OneToMany(mappedBy = "postulacion", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Seguimiento> seguimientos = new ArrayList<>();
}
