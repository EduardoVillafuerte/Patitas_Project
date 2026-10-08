package com.patitas.Models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import com.patitas.Models.Enums.*;

import java.time.LocalDate;
import java.time.Period;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

@Entity
@Table(name = "usuarios")
@Getter @Setter @NoArgsConstructor
public class Usuario implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nombre;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EnumRol rol;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EnumEstadoCuenta estado;

    /** RUC o registro de ONG (solo refugios). */
    private String documentoLegal;

    // Perfil de estilo de vida (solo adoptantes)
    private Integer horasFueraCasa;

    @Enumerated(EnumType.STRING)
    private EnumTipoVivienda tipoVivienda;

    private Double presupuestoMensual;
    private Boolean tieneNinos;
    private boolean perfilCompleto;

    // Datos personales (solo adoptantes)
    private LocalDate fechaNacimiento;
    private String ciudad;
    private String ocupacion;
    private String telefono;

    @Column(length = 200)
    private String hobbies;

    @Column(length = 500)
    private String sobreMi;

    @Enumerated(EnumType.STRING)
    private EnumExperiencia experiencia;

    public boolean isActivo() { return estado == EnumEstadoCuenta.ACTIVO; }
    public boolean isBaneado() { return estado == EnumEstadoCuenta.BANNED; }

    /** Primera letra del nombre en mayúscula, para el avatar. */
    public String getInicial() {
        return (nombre == null || nombre.isBlank()) ? "?" : nombre.trim().substring(0, 1).toUpperCase();
    }

    /** Edad calculada; null si no hay fecha de nacimiento. */
    public Integer getEdad() {
        return fechaNacimiento == null ? null : Period.between(fechaNacimiento, LocalDate.now()).getYears();
    }

    /** Hobbies separados por coma, como lista limpia. */
    public List<String> getHobbiesLista() {
        if (hobbies == null || hobbies.isBlank()) return List.of();
        return Arrays.stream(hobbies.split(",")).map(String::trim).filter(h -> !h.isEmpty()).toList();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + rol.name()));
    }

    @Override
    public String getUsername() { return email; }

    
}
