package com.patitas.dto;

import com.patitas.Models.Enums.EnumExperiencia;
import com.patitas.Models.Enums.EnumTipoVivienda;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDate;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class PerfilForm {
    @NotBlank(message = "Indica tu nombre")
    @Size(max = 80, message = "Máximo 80 caracteres")
    private String nombre;

    @NotNull(message = "Indica tu fecha de nacimiento")
    @Past(message = "La fecha debe ser pasada")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate fechaNacimiento;

    @NotBlank(message = "Indica tu ciudad")
    @Size(max = 60, message = "Máximo 60 caracteres")
    private String ciudad;

    @Size(max = 60, message = "Máximo 60 caracteres")
    private String ocupacion;

    @Pattern(regexp = "^$|^[0-9+()\\- ]{7,20}$", message = "Teléfono no válido")
    private String telefono;

    @Size(max = 200, message = "Máximo 200 caracteres")
    private String hobbies;

    @Size(max = 500, message = "Máximo 500 caracteres")
    private String sobreMi;

    @NotNull(message = "Indica tu experiencia")
    private EnumExperiencia experiencia;

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
