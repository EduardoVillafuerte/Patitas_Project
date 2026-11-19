package com.patitas.Controllers;

import com.patitas.Models.Enums.*;
import com.patitas.Models.*;
import com.patitas.Repository.MascotaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Comparator;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class PublicController {

    private final MascotaRepository mascotas;

    @GetMapping("/")
    public String raiz() {
        return "redirect:/public/inicio";
    }

    @GetMapping("/public/inicio")
    public String inicio(Model model) {
        // La portada muestra TODAS las mascotas activas (disponibles, en proceso y adoptadas), cada una con su etiqueta de estado.
        // OJO: el catálogo del adoptante (AdoptanteController.catalogo) NO se toca: ahí solo salen las DISPONIBLES.
        // Ordenamos para que las DISPONIBLES salgan primero. sorted() es estable: dentro de cada grupo se conserva
        // el orden de la consulta (las más recientes primero). false va antes que true, por eso "es distinta de DISPONIBLE".
        List<Mascota> todas = mascotas.findByActivoTrueOrderByIdDesc().stream()
                .sorted(Comparator.comparing((Mascota m) -> m.getEstado() != EnumEstadoMascota.DISPONIBLE))
                .toList();
        model.addAttribute("mascotas", todas);
        return "public/inicio";
    }

    @GetMapping("/public/mascotas/{id}/imagen")
    public ResponseEntity<byte[]> imagen(@PathVariable Long id) {
        return mascotas.findById(id)
                .filter(Mascota::isActivo)
                .filter(Mascota::tieneImagen)
                .map(m -> ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(m.getImagenTipo()))
                        .body(m.getImagen()))
                .orElse(ResponseEntity.notFound().build());
    }
}
