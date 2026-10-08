package com.patitas.Controllers;

import com.patitas.exception.BusinessException;
import com.patitas.Models.Enums.*;
import com.patitas.Repository.PostulacionRepository;
import com.patitas.Repository.SeguimientoRepository;
import com.patitas.Repository.UsuarioRepository;
import com.patitas.Repository.MascotaRepository;
import com.patitas.Services.MascotaService;
import com.patitas.Services.UsuarioService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final UsuarioService usuarioService;
    private final UsuarioRepository usuarios;
    private final MascotaRepository mascotas;
    private final MascotaService mascotaService;
    private final PostulacionRepository postulaciones;
    private final SeguimientoRepository seguimientos;

    @GetMapping("/panel")
    public String panel(Model model) {
        // Estadísticas globales
        model.addAttribute("totalMascotas", mascotas.countByActivoTrue());
        model.addAttribute("disponibles", mascotas.countByEstado(EnumEstadoMascota.DISPONIBLE));
        model.addAttribute("adoptadas", mascotas.countByEstado(EnumEstadoMascota.ADOPTADO));
        model.addAttribute("adoptantes", usuarios.countByRol(EnumRol.ADOPTANTE));
        model.addAttribute("refugiosActivos", usuarios.countByRolAndEstado(EnumRol.REFUGIO, EnumEstadoCuenta.ACTIVO));
        model.addAttribute("postulacionesPendientes", postulaciones.countByEstado(EnumEstadoPostulacion.PENDIENTE));
        model.addAttribute("seguimientosVencidos", seguimientos.countByEstado(EnumEstadoSeguimiento.VENCIDO));

        // Listas de gestión
        model.addAttribute("refugiosPendientes",
                usuarios.findByRolAndEstado(EnumRol.REFUGIO, EnumEstadoCuenta.PENDIENTE_APROBACION));
        model.addAttribute("usuarios", usuarios.findByRolNotOrderByIdDesc(EnumRol.ADMIN));
        model.addAttribute("mascotas", mascotas.findByActivoTrueOrderByIdDesc());
        return "admin/panel";
    }

    @PostMapping("/refugios/{id}/aprobar")
    public String aprobarRefugio(@PathVariable Long id, RedirectAttributes ra) {
        return cambiar(id, EnumEstadoCuenta.ACTIVO, "Refugio aprobado.", ra);
    }

    @PostMapping("/usuarios/{id}/ban")
    public String ban(@PathVariable Long id, RedirectAttributes ra) {
        return cambiar(id, EnumEstadoCuenta.BANNED, "Usuario agregado a la lista negra.", ra);
    }

    @PostMapping("/usuarios/{id}/desbanear")
    public String desbanear(@PathVariable Long id, RedirectAttributes ra) {
        return cambiar(id, EnumEstadoCuenta.ACTIVO, "Usuario reactivado.", ra);
    }

    @PostMapping("/mascotas/{id}/eliminar")
    public String eliminarMascota(@PathVariable Long id, Authentication auth, RedirectAttributes ra) {
        try {
            mascotaService.eliminar(usuarioService.actual(auth), id);
            ra.addFlashAttribute("ok", "Mascota eliminada del catálogo (el historial se conserva).");
        } catch (BusinessException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/panel";
    }

    private String cambiar(Long id, EnumEstadoCuenta estado, String okMsg, RedirectAttributes ra) {
        try {
            usuarioService.cambiarEstado(id, estado);
            ra.addFlashAttribute("ok", okMsg);
        } catch (BusinessException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/panel";
    }
}
