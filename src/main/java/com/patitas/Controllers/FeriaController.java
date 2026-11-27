package com.patitas.Controllers;

import com.patitas.dto.FeriaForm;
import com.patitas.exception.BusinessException;
import com.patitas.Models.Enums.EnumRol;
import com.patitas.Models.Usuario;
import com.patitas.Services.FeriaService;
import com.patitas.Services.UsuarioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Ferias de adopción.
 *   /public/ferias            -> cualquiera ve las próximas ferias (el adoptante además puede inscribirse)
 *   /adoptante/ferias/...     -> inscribirse / cancelar inscripción (SecurityConfig: solo ADOPTANTE)
 *   /refugio/ferias/...       -> publicar y cancelar ferias, ver inscritos (SecurityConfig: solo REFUGIO)
 */
@Controller
@RequiredArgsConstructor
public class FeriaController {

    private final FeriaService ferias;
    private final UsuarioService usuarios;

    // ---------------- Público ----------------
    @GetMapping("/public/ferias")
    public String lista(Authentication auth, Model model) {
        Usuario u = (auth != null) ? usuarios.actual(auth) : null;
        model.addAttribute("items", ferias.proximas(u));
        model.addAttribute("esAdoptante", u != null && u.getRol() == EnumRol.ADOPTANTE);
        return "ferias/lista";
    }

    // ---------------- Adoptante ----------------
    @PostMapping("/adoptante/ferias/{id}/inscribir")
    public String inscribir(@PathVariable Long id, Authentication auth, RedirectAttributes ra) {
        try {
            ferias.inscribir(usuarios.actual(auth), id);
            ra.addFlashAttribute("ok", "Listo, quedaste inscrito. El refugio verá tu nombre y correo.");
        } catch (BusinessException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/public/ferias";
    }

    @PostMapping("/adoptante/ferias/{id}/cancelar")
    public String cancelarInscripcion(@PathVariable Long id, Authentication auth, RedirectAttributes ra) {
        try {
            ferias.cancelarInscripcion(usuarios.actual(auth), id);
            ra.addFlashAttribute("ok", "Cancelaste tu inscripción.");
        } catch (BusinessException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/public/ferias";
    }

    // ---------------- Refugio ----------------
    @GetMapping("/refugio/ferias")
    public String panel(Authentication auth, Model model) {
        Usuario u = usuarios.actual(auth);
        cargar(model, u);
        model.addAttribute("form", new FeriaForm());
        return "ferias/refugio";
    }

    @PostMapping("/refugio/ferias")
    public String crear(@Valid @ModelAttribute("form") FeriaForm form, BindingResult br,
                        Authentication auth, Model model, RedirectAttributes ra) {
        Usuario u = usuarios.actual(auth);
        if (br.hasErrors()) {
            cargar(model, u);
            model.addAttribute("abrirForm", true); // deja el formulario desplegado para ver los errores
            return "ferias/refugio";
        }
        try {
            ferias.crear(u, form);
            ra.addFlashAttribute("ok", "Feria publicada.");
            return "redirect:/refugio/ferias";
        } catch (BusinessException e) {
            cargar(model, u);
            model.addAttribute("abrirForm", true);
            model.addAttribute("error", e.getMessage());
            return "ferias/refugio";
        }
    }

    @PostMapping("/refugio/ferias/{id}/cancelar")
    public String cancelar(@PathVariable Long id, Authentication auth, RedirectAttributes ra) {
        try {
            ferias.cancelar(usuarios.actual(auth), id);
            ra.addFlashAttribute("ok", "Feria cancelada.");
        } catch (BusinessException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/refugio/ferias";
    }

    private void cargar(Model model, Usuario u) {
        model.addAttribute("usuario", u);
        model.addAttribute("items", ferias.delRefugio(u));
    }
}
