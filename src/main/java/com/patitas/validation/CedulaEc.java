package com.patitas.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Anotación para validar una cédula ecuatoriana en un formulario (DTO). */
@Documented
@Constraint(validatedBy = CedulaEcValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface CedulaEc {
    String message() default "Cédula ecuatoriana no válida";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
