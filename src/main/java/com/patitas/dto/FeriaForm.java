package com.patitas.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class FeriaForm {

    @NotBlank(message = "Ingresa el título de la feria")
    @Size(max = 100, message = "Máximo 100 caracteres")
    private String titulo;

    @Size(max = 1000, message = "Máximo 1000 caracteres")
    private String descripcion;

    @NotNull(message = "Indica la fecha")
    @FutureOrPresent(message = "La fecha no puede ser pasada")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate fecha;

    @NotNull(message = "Indica la hora de inicio")
    @DateTimeFormat(iso = DateTimeFormat.ISO.TIME)
    private LocalTime horaInicio;

    @DateTimeFormat(iso = DateTimeFormat.ISO.TIME)
    private LocalTime horaFin;

    @NotBlank(message = "Indica dónde será la feria")
    @Size(max = 200, message = "Máximo 200 caracteres")
    private String lugar;

    @Min(value = 1, message = "El cupo mínimo es 1")
    @Max(value = 10000, message = "Cupo demasiado alto")
    private Integer cupo;
}
