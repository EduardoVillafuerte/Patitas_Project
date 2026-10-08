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
        model.addAttribute("mascotas", mascotas.findByActivoTrueAndEstado(EnumEstadoMascota.DISPONIBLE));
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
