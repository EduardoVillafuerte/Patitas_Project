package com.patitas.dto;

import com.patitas.Models.Feria;
import com.patitas.Models.InscripcionFeria;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

/** Feria + sus inscritos, para el panel del refugio. */
@Getter
@AllArgsConstructor
public class FeriaRefugioItem {
    private final Feria feria;
    private final List<InscripcionFeria> inscritos;
    private final boolean pasada;
}
