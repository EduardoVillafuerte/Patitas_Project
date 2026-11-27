package com.patitas.dto;

import com.patitas.Models.Feria;
import lombok.AllArgsConstructor;
import lombok.Getter;

/** Feria + datos calculados para la lista pública. */
@Getter
@AllArgsConstructor
public class FeriaItem {
    private final Feria feria;
    private final long inscritos;
    private final boolean inscrito;   // ¿la persona que mira ya está inscrita?
    private final boolean lleno;      // ¿se acabó el cupo?
}
