package com.patitas.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

/** Mensaje del chat tal como lo recibe el navegador (JSON). */
@Getter
@AllArgsConstructor
public class MensajeDto {
    private final long id;
    private final String autor;
    private final boolean mio;
    private final String contenido;
    private final String fecha;
}
