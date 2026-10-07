package com.patitas.Controllers;

import com.patitas.dto.RegistroForm;
import com.patitas.Models.Enums.EnumRol;
import com.patitas.Services.UsuarioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * CONTROLADOR de login y registro.
 *
 * Nota: el POST de "/login" NO está aquí. Lo procesa Spring Security solo
 * (configurado en SecurityConfig.formLogin). Aquí únicamente mostramos la pantalla.
 */
@Controller // Los métodos devuelven el NOMBRE de una vista (plantilla Thymeleaf) o una redirección.
@RequiredArgsConstructor
public class AuthController {

    private final UsuarioService usuarios;

    /**
     * Muestra el formulario de login (templates/login.html).
     * 'Authentication auth' vale null si NO hay sesión iniciada. Si ya hay sesión,
     * no tiene sentido mostrar el login otra vez: mandamos al inicio.
     */
    @GetMapping("/login")
    public String login(Authentication auth) {
        if (auth != null) {
            return "redirect:/";
        }
        return "login";
    }

    /** Muestra el formulario vacío. 'registroForm' es el objeto que Thymeleaf va a llenar. */
    @GetMapping("/registro")
    public String registro(Authentication auth, Model model) {
        if (auth != null) {
            return "redirect:/";
        }
        model.addAttribute("registroForm", new RegistroForm());
        return "registro";
    }

    /**
     * Recibe el formulario de registro.
     *  - @Valid            -> ejecuta las validaciones de RegistroForm (@NotBlank, @Email, @Size...).
     *  - BindingResult     -> guarda los errores; DEBE ir justo después del objeto validado.
     *  - RedirectAttributes-> permite enviar un mensaje (flash) que sobrevive a la redirección.
     */
    @PostMapping("/registro")
    public String registrar(@Valid @ModelAttribute("registroForm") RegistroForm form,
                            BindingResult br, RedirectAttributes ra) {
        // Validaciones de negocio que las anotaciones no pueden hacer:
        if (form.getTipoCuenta() == EnumRol.ADMIN) {
            br.rejectValue("tipoCuenta", "invalido", "Tipo de cuenta no permitido");
        }
        if (form.getEmail() != null && usuarios.emailExiste(form.getEmail())) {
            br.rejectValue("email", "duplicado", "Ya existe una cuenta con ese correo");
        }
        if (form.getTipoCuenta() == EnumRol.REFUGIO
                && (form.getDocumentoLegal() == null || form.getDocumentoLegal().isBlank())) {
            br.rejectValue("documentoLegal", "requerido", "Los refugios deben indicar su RUC o registro de ONG");
        }

        // Si hubo errores, NO redirigimos: volvemos a mostrar el mismo formulario
        // (con los datos que escribió y los mensajes de error junto a cada campo).
        if (br.hasErrors()) {
            return "registro";
        }

        usuarios.registrar(form);
        if (form.getTipoCuenta() == EnumRol.REFUGIO) {
            ra.addFlashAttribute("ok", "Cuenta creada. Un administrador revisará tu RUC/ONG antes de que puedas publicar mascotas.");
        } else {
            ra.addFlashAttribute("ok", "Cuenta creada. Inicia sesión para completar tu perfil.");
        }
        // Patrón POST -> Redirect -> GET: evita que al recargar (F5) se reenvíe el formulario.
        return "redirect:/login";
    }
}
