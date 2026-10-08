package com.patitas.Controllers;

import com.patitas.dto.MascotaForm;
import com.patitas.exception.BusinessException;
import com.patitas.Models.Enums.*;
import com.patitas.Models.*;
import com.patitas.Repository.MascotaRepository;
import com.patitas.Repository.PostulacionRepository;
import com.patitas.Repository.SeguimientoRepository;
import com.patitas.Services.AdopcionService;
import com.patitas.Services.MascotaService;
import com.patitas.Services.UsuarioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/refugio")
@RequiredArgsConstructor
public class RefugioController {

    private final UsuarioService usuarios;
    private final MascotaRepository mascotas;
    private final MascotaService mascotaService;
    private final AdopcionService adopciones;
    private final PostulacionRepository postulaciones;
    private final SeguimientoRepository seguimientos;

    // ---------------- Panel: inventario + alertas rojas ----------------
    @GetMapping("/panel")
    public String panel(Authentication auth, Model model) {
        Usuario u = usuarios.actual(auth);
        model.addAttribute("usuario", u);
        if (u.isActivo()) {
            adopciones.marcarVencidos(); // refresca las alertas al abrir el panel
            List<Seguimiento> segs = seguimientos.findByPostulacionMascotaRefugioOrderByFechaLimiteAsc(u);
            model.addAttribute("mascotas", mascotas.findByRefugioAndActivoTrueOrderByIdDesc(u));
            model.addAttribute("seguimientos", segs);
            model.addAttribute("alertas", segs.stream()
                    .filter(s -> s.getEstado() == EnumEstadoSeguimiento.VENCIDO).toList());
            model.addAttribute("pendientes",
                    postulaciones.countByMascotaRefugioAndEstado(u, EnumEstadoPostulacion.PENDIENTE));
        }
        return "refugio/panel";
    }

    // ---------------- CRUD de mascotas ----------------
    @GetMapping("/mascotas/nueva")
    public String nueva(Authentication auth, Model model) {
        if (!usuarios.actual(auth).isActivo()) return "redirect:/refugio/panel";
        model.addAttribute("form", new MascotaForm());
        model.addAttribute("mascotaId", null);
        catalogos(model);
        return "refugio/mascota-form";
    }

    @PostMapping("/mascotas")
    public String crear(@Valid @ModelAttribute("form") MascotaForm form, BindingResult br,
                        Authentication auth, Model model, RedirectAttributes ra) {
        Usuario u = usuarios.actual(auth);
        if (!u.isActivo()) return "redirect:/refugio/panel";
        if (br.hasErrors()) {
            model.addAttribute("mascotaId", null);
            catalogos(model);
            return "refugio/mascota-form";
        }
        try {
            mascotaService.crear(u, form);
            ra.addFlashAttribute("ok", "Mascota registrada.");
            return "redirect:/refugio/panel";
        } catch (BusinessException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("mascotaId", null);
            catalogos(model);
            return "refugio/mascota-form";
        }
    }

    @GetMapping("/mascotas/{id}/editar")
    public String editar(@PathVariable Long id, Authentication auth, Model model, RedirectAttributes ra) {
        Usuario u = usuarios.actual(auth);
        if (!u.isActivo()) return "redirect:/refugio/panel";
        try {
            Mascota m = mascotaService.propia(u, id);
            model.addAttribute("form", MascotaForm.desde(m));
            model.addAttribute("mascotaId", id);
            model.addAttribute("mascota", m);
            catalogos(model);
            return "refugio/mascota-form";
        } catch (BusinessException e) {
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/refugio/panel";
        }
    }

    @PostMapping("/mascotas/{id}")
    public String actualizar(@PathVariable Long id, @Valid @ModelAttribute("form") MascotaForm form,
                             BindingResult br, Authentication auth, Model model, RedirectAttributes ra) {
        Usuario u = usuarios.actual(auth);
        if (!u.isActivo()) return "redirect:/refugio/panel";
        try {
            if (br.hasErrors()) {
                model.addAttribute("mascotaId", id);
                model.addAttribute("mascota", mascotaService.propia(u, id));
                catalogos(model);
                return "refugio/mascota-form";
            }
            mascotaService.actualizar(u, id, form);
            ra.addFlashAttribute("ok", "Ficha actualizada.");
        } catch (BusinessException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/refugio/panel";
    }

    @PostMapping("/mascotas/{id}/eliminar")
    public String eliminar(@PathVariable Long id, Authentication auth, RedirectAttributes ra) {
        try {
            mascotaService.eliminar(usuarios.actual(auth), id);
            ra.addFlashAttribute("ok", "Mascota eliminada del catálogo (el historial se conserva).");
        } catch (BusinessException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/refugio/panel";
    }

    // ---------------- Postulaciones recibidas ----------------
    @GetMapping("/postulaciones")
    public String postulaciones(Authentication auth, Model model) {
        Usuario u = usuarios.actual(auth);
        if (!u.isActivo()) return "redirect:/refugio/panel";
        model.addAttribute("usuario", u);
        model.addAttribute("postulaciones", postulaciones.findByMascotaRefugioOrderByFechaDesc(u));
        return "refugio/postulaciones";
    }

    @PostMapping("/postulaciones/{id}/aprobar")
    public String aprobar(@PathVariable Long id, Authentication auth, RedirectAttributes ra) {
        return accion(ra, () -> adopciones.aprobar(usuarios.actual(auth), id), "Postulación aprobada.");
    }

    @PostMapping("/postulaciones/{id}/rechazar")
    public String rechazar(@PathVariable Long id, @RequestParam(required = false) String motivo,
                           Authentication auth, RedirectAttributes ra) {
        return accion(ra, () -> adopciones.rechazar(usuarios.actual(auth), id, motivo), "Postulación rechazada.");
    }

    @PostMapping("/postulaciones/{id}/completar")
    public String completar(@PathVariable Long id, Authentication auth, RedirectAttributes ra) {
        return accion(ra, () -> adopciones.completar(usuarios.actual(auth), id),
                "Adopción completada. Se generó el seguimiento a 30, 90 y 180 días.");
    }

    // ---------------- Evidencia de seguimiento (foto) ----------------
    @GetMapping("/seguimientos/{id}/evidencia")
    public ResponseEntity<byte[]> evidencia(@PathVariable Long id, Authentication auth) {
        Usuario u = usuarios.actual(auth);
        return seguimientos.findById(id)
                .filter(s -> s.getPostulacion().getMascota().getRefugio().getId().equals(u.getId()))
                .filter(Seguimiento::tieneEvidencia)
                .map(s -> ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(s.getEvidenciaTipo()))
                        .body(s.getEvidencia()))
                .orElse(ResponseEntity.notFound().build());
    }

    // ---------------- helpers ----------------
    private String accion(RedirectAttributes ra, Runnable r, String okMsg) {
        try {
            r.run();
            ra.addFlashAttribute("ok", okMsg);
        } catch (BusinessException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/refugio/postulaciones";
    }

    private void catalogos(Model model) {
        model.addAttribute("todasEtiquetas", EnumEtiqueta.values());
        model.addAttribute("estados", List.of(EnumEstadoMascota.DISPONIBLE, EnumEstadoMascota.EN_PROCESO));
    }
}
