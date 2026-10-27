package com.patitas.Controllers;

import com.patitas.dto.RegistroForm;
import com.patitas.Models.Enums.EnumRol;
import com.patitas.Services.UsuarioService;
import com.patitas.exception.BusinessException;
import com.patitas.validation.IdentificacionEc;

import org.springframework.dao.DataIntegrityViolationException;
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
        if (form.getTipoCuenta() == EnumRol.REFUGIO) {
            String ruc = form.getDocumentoLegal() == null ? "" : form.getDocumentoLegal().trim();
            if (ruc.isEmpty()) {
                br.rejectValue("documentoLegal", "requerido", "Los refugios deben indicar su RUC");
            } else if (!br.hasFieldErrors("documentoLegal")) {
                if (!IdentificacionEc.esRucValido(ruc)) {
                    br.rejectValue("documentoLegal", "invalido", "El RUC no es válido, revisa los 13 dígitos");
                } else if (usuarios.documentoExiste(ruc)) {
                    br.rejectValue("documentoLegal", "duplicado", "Ya existe un refugio registrado con ese RUC");
                }
            }
        } else {
            String cedula = form.getCedula() == null ? "" : form.getCedula().trim();
            if (cedula.isEmpty()) {
                br.rejectValue("cedula", "requerido", "Ingresa tu cédula");
            } else if (!br.hasFieldErrors("cedula") && usuarios.cedulaExiste(cedula)) {
                br.rejectValue("cedula", "duplicado", "Ya existe una cuenta registrada con esa cédula");
            }
        }
        // Si hubo errores, NO redirigimos: volvemos a mostrar el mismo formulario
        // (con los datos que escribió y los mensajes de error junto a cada campo).
        if (br.hasErrors()) {
            return "registro";
        }

        try {
            usuarios.registrar(form);
        } catch (BusinessException | DataIntegrityViolationException e) {
            // Última barrera: validación del servicio o restricción única de la BD (p. ej. dos registros simultáneos).
            String msg = (e instanceof BusinessException) ? e.getMessage()
                    : "Ya existe una cuenta con esos datos (correo, cédula o RUC)";
            br.reject("registro", msg);
            return "registro";
        }
        if (form.getTipoCuenta() == EnumRol.REFUGIO) {
            ra.addFlashAttribute("ok", "Cuenta creada. Un administrador revisará tu RUC/ONG antes de que puedas publicar mascotas.");
        } else {
            ra.addFlashAttribute("ok", "Cuenta creada. Inicia sesión para completar tu perfil.");
        }
        // Patrón POST -> Redirect -> GET: evita que al recargar (F5) se reenvíe el formulario.
        return "redirect:/login";
    }
}
