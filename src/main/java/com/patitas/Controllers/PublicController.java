package com.patitas.Controllers;

import com.patitas.dto.MascotaMapaDto;
import com.patitas.dto.RefugioMapaDto;
import com.patitas.Models.Enums.*;
import com.patitas.Models.*;
import com.patitas.Repository.MascotaRepository;
import com.patitas.Repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
public class PublicController {

    private final MascotaRepository mascotas;
    private final UsuarioRepository usuarios;

    @GetMapping("/")
    public String raiz() {
        return "redirect:/public/inicio";
    }

    @GetMapping("/public/inicio")
    public String inicio(Model model) {
        model.addAttribute("mascotas", mascotas.findByActivoTrueAndEstado(EnumEstadoMascota.DISPONIBLE));
        return "public/inicio";
    }

    // ---------------- Mapa de refugios ----------------
    @GetMapping("/public/mapa")
    public String mapa() {
        return "public/mapa";
    }

    /** JSON que consume el mapa: refugios activos con ubicación y sus mascotas disponibles. */
    @GetMapping("/public/mapa/datos")
    @ResponseBody
    public List<RefugioMapaDto> datosMapa() {
        // Una sola consulta de mascotas, agrupadas por refugio (evita una consulta por refugio).
        Map<Long, List<MascotaMapaDto>> porRefugio = mascotas.findByActivoTrueAndEstado(EnumEstadoMascota.DISPONIBLE).stream()
                .collect(Collectors.groupingBy(m -> m.getRefugio().getId(),
                        Collectors.mapping(m -> new MascotaMapaDto(m.getId(), m.getNombre(), m.getEspecie(),
                                m.getRaza(), m.getEdadAnios()), Collectors.toList())));

        return usuarios.findByRolAndEstado(EnumRol.REFUGIO, EnumEstadoCuenta.ACTIVO).stream()
                .filter(Usuario::tieneUbicacion)
                .map(r -> new RefugioMapaDto(r.getId(), r.getNombre(), r.getDireccion(),
                        r.getLatitud(), r.getLongitud(), porRefugio.getOrDefault(r.getId(), List.of())))
                .toList();
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
