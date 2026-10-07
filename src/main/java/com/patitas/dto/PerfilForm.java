package com.patitas.dto;

import com.patitas.Models.Enums.EnumTipoVivienda;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class PerfilForm {
    @NotNull(message = "Indica las horas")
    @Min(value = 0, message = "Mínimo 0")
    @Max(value = 24, message = "Máximo 24")
    private Integer horasFueraCasa;

    @NotNull(message = "Elige un tipo de vivienda")
    private EnumTipoVivienda tipoVivienda;

    @NotNull(message = "Indica tu presupuesto")
    @PositiveOrZero(message = "No puede ser negativo")
    private Double presupuestoMensual;

    private boolean tieneNinos;
}
