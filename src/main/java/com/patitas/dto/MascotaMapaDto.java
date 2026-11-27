package com.patitas.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class MascotaMapaDto {
    private final Long id;
    private final String nombre;
    private final String especie;
    private final String raza;
    private final Integer edadAnios;
}
