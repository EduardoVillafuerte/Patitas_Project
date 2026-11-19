package com.patitas.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UbicacionForm {

    @NotBlank(message = "Ingresa la dirección o una referencia")
    @Size(max = 200, message = "Máximo 200 caracteres")
    private String direccion;

    @NotNull(message = "Marca tu ubicación en el mapa")
    @DecimalMin(value = "-90", message = "Latitud no válida")
    @DecimalMax(value = "90", message = "Latitud no válida")
    private Double latitud;

    @NotNull(message = "Marca tu ubicación en el mapa")
    @DecimalMin(value = "-180", message = "Longitud no válida")
    @DecimalMax(value = "180", message = "Longitud no válida")
    private Double longitud;
}
