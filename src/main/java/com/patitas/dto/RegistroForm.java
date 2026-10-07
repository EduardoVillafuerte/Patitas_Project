package com.patitas.dto;

import com.patitas.Models.Enums.EnumRol;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegistroForm {
    @NotBlank(message = "Ingresa tu nombre")
    private String nombre;

    @NotBlank(message = "Ingresa tu correo")
    @Email(message = "Correo no válido")
    private String email;

    @NotBlank(message = "Ingresa una contraseña")
    @Size(min = 6, message = "Mínimo 6 caracteres")
    private String password;

    @NotNull(message = "Elige el tipo de cuenta")
    private EnumRol tipoCuenta = EnumRol.ADOPTANTE;

    private String documentoLegal;
}
