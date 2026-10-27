package com.patitas.dto;

import com.patitas.Models.Enums.EnumRol;
import com.patitas.validation.CedulaEc;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegistroForm {

    @NotBlank(message = "Ingresa tu nombre")
    @Size(max = 80, message = "Máximo 80 caracteres")
    private String nombre;

    @NotBlank(message = "Ingresa tu correo")
    @Email(message = "Correo no válido")
    @Size(max = 120, message = "Máximo 120 caracteres")
    private String email;

    @NotBlank(message = "Ingresa una contraseña")
    @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).{8,72}$",
             message = "Mínimo 8 caracteres, con al menos una letra y un número")
    private String password;

    @NotNull(message = "Elige el tipo de cuenta")
    private EnumRol tipoCuenta = EnumRol.ADOPTANTE;

    /** Cédula del adoptante (dato sensible): se valida en el servidor. Obligatoria solo para adoptantes. */
    @CedulaEc
    private String cedula;

    /** RUC del refugio (dato sensible): 13 dígitos. Obligatorio solo para refugios. */
    @Pattern(regexp = "^$|^\\d{13}$", message = "El RUC debe tener 13 dígitos")
    private String documentoLegal;
}
