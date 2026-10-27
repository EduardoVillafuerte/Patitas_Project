package com.patitas.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/** Un valor vacío se considera válido aquí; si el campo es obligatorio se exige con @NotBlank o en el controlador. */
public class CedulaEcValidator implements ConstraintValidator<CedulaEc, String> {
    @Override
    public boolean isValid(String valor, ConstraintValidatorContext ctx) {
        if (valor == null || valor.isBlank()) return true;
        return IdentificacionEc.esCedulaValida(valor.trim());
    }
}
