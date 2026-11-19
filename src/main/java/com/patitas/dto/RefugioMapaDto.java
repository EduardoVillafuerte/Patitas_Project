package com.patitas.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

/** Un refugio con su ubicación y las mascotas disponibles, para pintarlo en el mapa. */
@Getter
@AllArgsConstructor
public class RefugioMapaDto {
    private final Long id;
    private final String nombre;
    private final String direccion;
    private final double latitud;
    private final double longitud;
    private final List<MascotaMapaDto> mascotas;
}
