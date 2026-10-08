package com.patitas.dto;

import com.patitas.Models.Enums.*;
import com.patitas.Models.*;
import jakarta.validation.constraints.*;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashSet;
import java.util.Set;

/**
 * Objeto que representa los datos del FORMULARIO de mascota (no es la entidad de la BD).
 * Sirve para validar lo que escribe el usuario antes de tocar la base de datos.
 *
 * Nota QA: los campos numéricos usan Integer/Double (con mayúscula) y NO int/double.
 * Un 'int' nunca puede ser null, así que si el usuario deja el campo vacío Spring
 * mostraba un error técnico en inglés. Con Integer + @NotNull el mensaje es claro.
 */
@Data // Lombok: genera getters, setters, equals, hashCode y toString.
public class MascotaForm {
    @NotBlank(message = "Ingresa el nombre")
    @Size(max = 60, message = "Máximo 60 caracteres")
    private String nombre;

    @NotBlank(message = "Ingresa la especie (perro, gato...)")
    @Size(max = 40, message = "Máximo 40 caracteres")
    private String especie;

    @NotNull(message = "Ingresa la edad")
    @Min(value = 0, message = "Mínimo 0")
    @Max(value = 40, message = "Máximo 40 años")
    private Integer edadAnios;

    @Size(max = 1000, message = "Máximo 1000 caracteres")
    private String descripcion;

    @NotNull(message = "Elige el nivel de energía")
    @Min(value = 1, message = "Mínimo 1")
    @Max(value = 5, message = "Máximo 5")
    private Integer nivelEnergia = 3;

    @NotNull(message = "Elige la sociabilidad")
    @Min(value = 1, message = "Mínimo 1")
    @Max(value = 5, message = "Máximo 5")
    private Integer sociabilidad = 3;

    @NotNull(message = "Ingresa el presupuesto mensual")
    @PositiveOrZero(message = "No puede ser negativo")
    private Double presupuestoMinimo;

    private Set<EnumEtiqueta> etiquetas = new HashSet<>();

    private boolean vacunado;
    private boolean esterilizado;

    @Size(max = 2000, message = "Máximo 2000 caracteres")
    private String fichaMedica;

    private EnumEstadoMascota estado = EnumEstadoMascota.DISPONIBLE;

    private MultipartFile imagen;

    /** Convierte una Mascota de la BD en un formulario prellenado (para la pantalla "Editar"). */
    public static MascotaForm desde(Mascota m) {
        MascotaForm f = new MascotaForm();
        f.nombre = m.getNombre();
        f.especie = m.getEspecie();
        f.edadAnios = m.getEdadAnios();
        f.descripcion = m.getDescripcion();
        f.nivelEnergia = m.getNivelEnergia();
        f.sociabilidad = m.getSociabilidad();
        f.presupuestoMinimo = m.getPresupuestoMinimo();
        f.etiquetas = new HashSet<>(m.getEtiquetas());
        f.vacunado = m.isVacunado();
        f.esterilizado = m.isEsterilizado();
        f.fichaMedica = m.getFichaMedica();
        f.estado = m.getEstado();
        return f;
    }
}
