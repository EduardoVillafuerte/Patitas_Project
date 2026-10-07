package com.patitas.Models;

import com.patitas.Models.Enums.*;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "mascotas")
@Getter @Setter @NoArgsConstructor
public class Mascota {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nombre;

    @Column(nullable = false)
    private String especie;

    private Integer edadAnios;

    @Column(length = 1000)
    private String descripcion;

    /** 1 (tranquilo) a 5 (muy activo) */
    private int nivelEnergia = 3;

    /** 1 (reservado) a 5 (muy sociable) */
    private int sociabilidad = 3;

    /** Presupuesto mensual mínimo recomendado (USD). */
    private double presupuestoMinimo;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "mascota_etiquetas", joinColumns = @JoinColumn(name = "mascota_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "etiqueta")
    private Set<EnumEtiqueta> etiquetas = new HashSet<>();

    // Ficha médica 
    private boolean vacunado;
    private boolean esterilizado;

    @Column(length = 2000)
    private String fichaMedica;

    // Imagen 
    @Lob
    private byte[] imagen;
    private String imagenTipo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EnumEstadoMascota estado = EnumEstadoMascota.DISPONIBLE;

    /** Borrado lógico: false = "eliminada" */
    private boolean activo = true;

    private LocalDate fechaRegistro = LocalDate.now();

    @ManyToOne(optional = false)
    @JoinColumn(name = "refugio_id")
    private Usuario refugio;

    public boolean tieneImagen() {
        return imagen != null && imagen.length > 0;
    }
}
