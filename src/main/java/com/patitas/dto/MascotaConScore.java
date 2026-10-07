package com.patitas.dto;

import com.patitas.Models.Mascota;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class MascotaConScore {
    private final Mascota mascota;
    private final int score;
    private final List<String> motivos;
}
