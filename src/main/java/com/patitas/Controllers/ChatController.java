package com.patitas.Controllers;

import com.patitas.dto.MensajeDto;
import com.patitas.exception.BusinessException;
import com.patitas.Models.Enums.EnumRol;
import com.patitas.Models.PostulacionAdopcion;
import com.patitas.Models.Usuario;
import com.patitas.Services.ChatService;
import com.patitas.Services.UsuarioService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Map;

/**
 * Chat de una postulación. Lo usan los dos roles (adoptante y refugio), por eso vive en /chat/**
 * (SecurityConfig lo abre solo a ADOPTANTE y REFUGIO) y ChatService verifica que seas parte de ESA postulación.
 *
 * El navegador pide los mensajes nuevos cada pocos segundos (polling) con GET .../mensajes?desde=ULTIMO_ID.
 */
@Controller
@RequestMapping("/chat/postulaciones/{id}")
@RequiredArgsConstructor
public class ChatController {

    private final UsuarioService usuarios;
    private final ChatService chat;

    @GetMapping
    public String ver(@PathVariable Long id, Authentication auth, Model model, RedirectAttributes ra) {
        Usuario u = usuarios.actual(auth);
        boolean soyRefugio = u.getRol() == EnumRol.REFUGIO;
        try {
            PostulacionAdopcion p = chat.acceso(u, id);
            model.addAttribute("usuario", u);
            model.addAttribute("postulacion", p);
            model.addAttribute("cerrado", chat.estaCerrado(p));
            model.addAttribute("soyRefugio", soyRefugio);
            model.addAttribute("otro", soyRefugio ? p.getAdoptante().getNombre() : p.getMascota().getRefugio().getNombre());
            model.addAttribute("maxCaracteres", ChatService.MAX_CARACTERES);
            return "chat/chat";
        } catch (BusinessException e) {
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:" + (soyRefugio ? "/refugio/postulaciones" : "/adoptante/postulaciones");
        }
    }

    @GetMapping("/mensajes")
    @ResponseBody
    public ResponseEntity<List<MensajeDto>> listar(@PathVariable Long id,
                                                   @RequestParam(defaultValue = "0") long desde,
                                                   Authentication auth) {
        try {
            return ResponseEntity.ok(chat.listar(usuarios.actual(auth), id, desde));
        } catch (BusinessException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/mensajes")
    @ResponseBody
    public ResponseEntity<?> enviar(@PathVariable Long id,
                                    @RequestParam(required = false) String contenido,
                                    Authentication auth) {
        try {
            return ResponseEntity.ok(chat.enviar(usuarios.actual(auth), id, contenido));
        } catch (BusinessException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
