package com.patitas.Controllers;

import com.patitas.dto.MascotaConScore;
import com.patitas.dto.PerfilForm;
import com.patitas.exception.BusinessException;
import com.patitas.Models.*;
import com.patitas.Models.Enums.*;
import com.patitas.Repository.MascotaRepository;
import com.patitas.Repository.PostulacionRepository;
import com.patitas.Repository.SeguimientoRepository;
import com.patitas.Services.AdopcionService;
import com.patitas.Services.CompatibilidadService;
import com.patitas.Services.UsuarioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Comparator;
import java.util.List;

@Controller
@RequestMapping("/adoptante")
@RequiredArgsConstructor
public class AdoptanteController {

    private final UsuarioService usuarios;
    private final MascotaRepository mascotas;
    private final PostulacionRepository postulaciones;
    private final SeguimientoRepository seguimientos;
    private final CompatibilidadService compatibilidad;
    private final AdopcionService adopciones;

    //Perfil de estilo de vida usuario
    @GetMapping("/perfil")
    public String perfil(Authentication auth, Model model) {
        Usuario u = usuarios.actual(auth);
        PerfilForm f = new PerfilForm();
        f.setHorasFueraCasa(u.getHorasFueraCasa());
        f.setTipoVivienda(u.getTipoVivienda());
        f.setPresupuestoMensual(u.getPresupuestoMensual());
        f.setTieneNinos(Boolean.TRUE.equals(u.getTieneNinos()));
        model.addAttribute("form", f);
        model.addAttribute("viviendas", EnumTipoVivienda.values());
        return "adoptante/perfil";
    }

    @PostMapping("/perfil")
    public String guardarPerfil(@Valid @ModelAttribute("form") PerfilForm form, BindingResult br,
                                Authentication auth, Model model, RedirectAttributes ra) {
        if (br.hasErrors()) {
            model.addAttribute("viviendas", EnumTipoVivienda.values());
            return "adoptante/perfil";
        }
        usuarios.guardarPerfil(usuarios.actual(auth), form);
        ra.addFlashAttribute("ok", "Perfil guardado. Ya puedes ver tu compatibilidad con cada mascota.");
        return "redirect:/adoptante/catalogo";
    }

    // Catálogo ordenado por compatibilidad
    @GetMapping("/catalogo")
    public String catalogo(Authentication auth, Model model) {
        Usuario u = usuarios.actual(auth);
        if (!u.isPerfilCompleto()) {
            return "redirect:/adoptante/perfil";
        }
        List<MascotaConScore> items = mascotas.findByActivoTrueAndEstado(EnumEstadoMascota.DISPONIBLE).stream()
                .map(m -> compatibilidad.evaluar(u, m))
                .sorted(Comparator.comparingInt(MascotaConScore::getScore).reversed())
                .toList();
        model.addAttribute("usuario", u);
        model.addAttribute("items", items);
        return "adoptante/catalogo";
    }

    @GetMapping("/mascotas/{id}")
    public String detalle(@PathVariable Long id, Authentication auth, Model model, RedirectAttributes ra) {
        Usuario u = usuarios.actual(auth);
        Mascota m = mascotas.findById(id).filter(Mascota::isActivo).orElse(null);
        if (m == null) {
            ra.addFlashAttribute("error", "La mascota no existe.");
            return "redirect:/adoptante/catalogo";
        }
        boolean yaPostulo = postulaciones.existsByAdoptanteAndMascotaAndEstadoIn(
                u, m, List.of(EnumEstadoPostulacion.PENDIENTE, EnumEstadoPostulacion.APROBADA));
        model.addAttribute("usuario", u);
        model.addAttribute("item", compatibilidad.evaluar(u, m));
        model.addAttribute("yaPostulo", yaPostulo);
        return "adoptante/detalle";
    }

    // ---------------- Postulaciones ----------------
    @PostMapping("/mascotas/{id}/postular")
    public String postular(@PathVariable Long id, @RequestParam(required = false) String mensaje,
                           Authentication auth, RedirectAttributes ra) {
        try {
            adopciones.postular(usuarios.actual(auth), id, mensaje);
            ra.addFlashAttribute("ok", "Postulación enviada. El refugio la revisará pronto.");
            return "redirect:/adoptante/postulaciones";
        } catch (BusinessException e) {
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/adoptante/mascotas/" + id;
        }
    }

    @GetMapping("/postulaciones")
    public String misPostulaciones(Authentication auth, Model model) {
        Usuario u = usuarios.actual(auth);
        model.addAttribute("usuario", u);
        model.addAttribute("postulaciones", postulaciones.findByAdoptanteOrderByFechaDesc(u));
        model.addAttribute("seguimientos", seguimientos.findByPostulacionAdoptanteOrderByFechaLimiteAsc(u));
        return "adoptante/postulaciones";
    }

    // ---------------- Evidencia de seguimiento ----------------
    @PostMapping("/seguimientos/{id}/evidencia")
    public String evidencia(@PathVariable Long id, @RequestParam(required = false) String comentario,
                            @RequestParam(required = false) MultipartFile foto,
                            Authentication auth, RedirectAttributes ra) {
        try {
            adopciones.entregarEvidencia(usuarios.actual(auth), id, comentario, foto);
            ra.addFlashAttribute("ok", "Evidencia enviada. ¡Gracias por mantener al día el seguimiento!");
        } catch (BusinessException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/adoptante/postulaciones";
    }
}
