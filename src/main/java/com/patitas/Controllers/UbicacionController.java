package com.patitas.Controllers;

import com.patitas.dto.UbicacionForm;
import com.patitas.Models.Usuario;
import com.patitas.Services.UsuarioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** El refugio marca en el mapa dónde queda (para que los adoptantes lo encuentren y puedan ir en persona). */
@Controller
@RequestMapping("/refugio/ubicacion")
@RequiredArgsConstructor
public class UbicacionController {

    private final UsuarioService usuarios;

    @GetMapping
    public String ver(Authentication auth, Model model) {
        Usuario u = usuarios.actual(auth);
        UbicacionForm f = new UbicacionForm();
        f.setDireccion(u.getDireccion());
        f.setLatitud(u.getLatitud());
        f.setLongitud(u.getLongitud());
        model.addAttribute("form", f);
        model.addAttribute("usuario", u);
        return "refugio/ubicacion";
    }

    @PostMapping
    public String guardar(@Valid @ModelAttribute("form") UbicacionForm form, BindingResult br,
                          Authentication auth, Model model, RedirectAttributes ra) {
        Usuario u = usuarios.actual(auth);
        if (br.hasErrors()) {
            model.addAttribute("usuario", u);
            return "refugio/ubicacion";
        }
        usuarios.guardarUbicacion(u, form);
        ra.addFlashAttribute("ok", "Ubicación guardada. Tu refugio ya aparece en el mapa.");
        return "redirect:/refugio/panel";
    }
}
