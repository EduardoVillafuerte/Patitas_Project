package com.patitas.Controllers;

import com.patitas.Models.Usuario;
import com.patitas.Services.NotificacionService;
import com.patitas.Services.UsuarioService;
import com.patitas.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Bandeja de notificaciones (campanita). La ven los tres roles; cada quien solo las suyas. */
@Controller
@RequestMapping("/notificaciones")
@RequiredArgsConstructor
public class NotificacionesController {

    private final UsuarioService usuarios;
    private final NotificacionService notificaciones;

    @GetMapping
    public String lista(Authentication auth, Model model) {
        Usuario u = usuarios.actual(auth);
        model.addAttribute("lista", notificaciones.recientes(u));
        return "notificaciones";
    }

    /** Número de avisos sin leer: lo consulta cada 30 segundos el JavaScript de la campanita. */
    @GetMapping("/contador")
    @ResponseBody
    public long contador(Authentication auth) {
        return notificaciones.contar(usuarios.actual(auth));
    }

    /** Marca como leída y lleva a la pantalla relacionada. */
    @GetMapping("/{id}/abrir")
    public String abrir(@PathVariable Long id, Authentication auth, RedirectAttributes ra) {
        try {
            return "redirect:" + notificaciones.abrir(usuarios.actual(auth), id);
        } catch (BusinessException e) {
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/notificaciones";
        }
    }

    @PostMapping("/leer-todas")
    public String leerTodas(Authentication auth, RedirectAttributes ra) {
        notificaciones.marcarTodas(usuarios.actual(auth));
        ra.addFlashAttribute("ok", "Todas las notificaciones quedaron como leídas.");
        return "redirect:/notificaciones";
    }
}
