package com.patitas.Services;

import com.patitas.Models.Feria;
import com.patitas.Models.Notificacion;
import com.patitas.Models.PostulacionAdopcion;
import com.patitas.Models.Seguimiento;
import com.patitas.Models.Usuario;
import com.patitas.Repository.NotificacionRepository;
import com.patitas.exception.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Cada cambio importante avisa al usuario por DOS vías:
 *  1) la campanita dentro de la página (tabla notificaciones), y
 *  2) un correo con la plantilla templates/email/notificacion.html (real o simulado, ver CorreoSender).
 * Los avisos de bajo valor (mensajes del chat, inscripciones a ferias) van solo a la campanita.
 * El correo sale DESPUÉS del commit: si la operación se deshace, no sale ningún correo.
 */
@Service
public class NotificacionService {

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final TemplateEngine plantillas;
    private final CorreoSender sender;
    private final NotificacionRepository bandeja;
    private final String urlBase;

    public NotificacionService(TemplateEngine plantillas, CorreoSender sender, NotificacionRepository bandeja,
                               @Value("${app.url:http://localhost:8080}") String urlBase) {
        this.plantillas = plantillas;
        this.sender = sender;
        this.bandeja = bandeja;
        this.urlBase = urlBase.endsWith("/") ? urlBase.substring(0, urlBase.length() - 1) : urlBase;
    }

    // ================= Flujo de adopción (campanita + correo) =================

    public void refugioNuevaPostulacion(PostulacionAdopcion p) {
        enviar(p.getMascota().getRefugio(), "Nueva postulación para " + p.getMascota().getNombre(),
                "Tienes una nueva postulación",
                p.getAdoptante().getNombre() + " quiere adoptar a " + p.getMascota().getNombre()
                        + " (compatibilidad " + p.getScoreAlPostular() + "%). Revísala y decide.",
                "/refugio/postulaciones", "Ver postulaciones");
    }

    public void adoptantePostulacionAprobada(PostulacionAdopcion p) {
        enviar(p.getAdoptante(), "Tu postulación para " + p.getMascota().getNombre() + " fue aprobada",
                "¡Buenas noticias!",
                "El refugio " + p.getMascota().getRefugio().getNombre() + " aprobó tu postulación para adoptar a "
                        + p.getMascota().getNombre() + ". Pronto se pondrán en contacto contigo para la entrega.",
                "/adoptante/postulaciones", "Ver mis postulaciones");
    }

    public void adoptantePostulacionRechazada(PostulacionAdopcion p) {
        String motivo = (p.getMotivoRechazo() == null || p.getMotivoRechazo().isBlank()) ? "" : " Motivo: " + p.getMotivoRechazo();
        enviar(p.getAdoptante(), "Actualización de tu postulación para " + p.getMascota().getNombre(),
                "Tu postulación no continuó",
                "Esta vez la postulación para " + p.getMascota().getNombre() + " no continuó." + motivo
                        + " Hay más mascotas esperando un hogar.",
                "/adoptante/catalogo", "Ver mascotas");
    }

    public void adopcionCompletada(PostulacionAdopcion p) {
        enviar(p.getAdoptante(), "¡Adopción de " + p.getMascota().getNombre() + " completada!",
                "¡Felicidades por tu nueva familia!",
                "La adopción de " + p.getMascota().getNombre() + " quedó registrada. Ya puedes descargar el contrato en PDF "
                        + "y recuerda subir la evidencia de seguimiento a los 30, 90 y 180 días.",
                "/adoptante/postulaciones", "Ver contrato y seguimiento");
    }

    // ================= Seguimiento =================

    public void evidenciaEntregada(Seguimiento s) {
        String mascota = s.getPostulacion().getMascota().getNombre();
        enviar(s.getPostulacion().getMascota().getRefugio(), "Seguimiento recibido: " + mascota,
                "Llegó una evidencia de seguimiento",
                s.getPostulacion().getAdoptante().getNombre() + " entregó el seguimiento del día "
                        + s.getDiasPostAdopcion() + " de " + mascota + ".",
                "/refugio/postulaciones", "Ver evidencia");
    }

    public void seguimientoVencido(Seguimiento s) {
        String mascota = s.getPostulacion().getMascota().getNombre();
        enviar(s.getPostulacion().getAdoptante(), "Seguimiento vencido: día " + s.getDiasPostAdopcion() + " de " + mascota,
                "Falta tu evidencia de seguimiento",
                "Pasó la fecha límite del seguimiento del día " + s.getDiasPostAdopcion() + " de " + mascota
                        + ". Sube una foto o un comentario cuanto antes.",
                "/adoptante/postulaciones", "Subir evidencia");
        enviar(s.getPostulacion().getMascota().getRefugio(), "Seguimiento vencido: " + mascota,
                "Un seguimiento quedó vencido",
                s.getPostulacion().getAdoptante().getNombre() + " no entregó a tiempo el seguimiento del día "
                        + s.getDiasPostAdopcion() + " de " + mascota + ".",
                "/refugio/postulaciones", "Ver postulaciones");
    }

    // ================= Cuentas (las decide el administrador) =================

    public void refugioAprobado(Usuario refugio) {
        enviar(refugio, "Tu refugio fue aprobado", "¡Tu refugio ya está activo!",
                "Un administrador aprobó tu cuenta. Ya puedes publicar mascotas, crear ferias y marcar tu ubicación en el mapa.",
                "/refugio/panel", "Ir a mi panel");
    }

    public void cuentaBaneada(Usuario u) {
        enviar(u, "Tu cuenta fue suspendida", "Tu cuenta fue suspendida",
                "Un administrador suspendió tu cuenta y no podrás realizar nuevas acciones. Si crees que es un error, contáctanos.",
                "/", "Ir a Patitas");
    }

    public void cuentaReactivada(Usuario u) {
        enviar(u, "Tu cuenta fue reactivada", "Tu cuenta volvió a estar activa",
                "Un administrador reactivó tu cuenta. Ya puedes usar Patitas con normalidad.",
                "/", "Ir a Patitas");
    }

    // ================= Ferias (solo campanita, salvo la cancelación) =================

    public void nuevaInscripcionFeria(Feria f, Usuario adoptante) {
        soloEnApp(f.getRefugio(), "Nueva inscripción a «" + f.getTitulo() + "»",
                adoptante.getNombre() + " se inscribió a tu feria.", "/refugio/ferias", false);
    }

    public void feriaCancelada(Feria f, Usuario inscrito) {
        enviar(inscrito, "Feria cancelada: " + f.getTitulo(), "Se canceló una feria",
                "El refugio " + f.getRefugio().getNombre() + " canceló la feria «" + f.getTitulo() + "» del "
                        + f.getFecha().format(FECHA) + ". Tu inscripción ya no es necesaria.",
                "/public/ferias", "Ver otras ferias");
    }

    // ================= Chat (solo campanita, un aviso sin leer por conversación) =================

    public void nuevoMensajeChat(PostulacionAdopcion p, Usuario autor) {
        Usuario destino = autor.getId().equals(p.getAdoptante().getId()) ? p.getMascota().getRefugio() : p.getAdoptante();
        soloEnApp(destino, "Nuevo mensaje de " + autor.getNombre(),
                "Tienes mensajes nuevos sobre " + p.getMascota().getNombre() + ".",
                "/chat/postulaciones/" + p.getId(), true);
    }

    // ================= Campanita: lectura y estado =================

    /** Cantidad de avisos sin leer de quien tiene la sesión abierta (se usa en la barra de navegación). */
    @Transactional(readOnly = true)
    public long noLeidas() {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        if (a == null || !a.isAuthenticated() || a instanceof AnonymousAuthenticationToken) return 0;
        return bandeja.countByUsuarioEmailAndLeidaFalse(a.getName());
    }

    @Transactional(readOnly = true)
    public long contar(Usuario u) {
        return bandeja.countByUsuarioIdAndLeidaFalse(u.getId());
    }

    @Transactional(readOnly = true)
    public List<Notificacion> recientes(Usuario u) {
        return bandeja.findTop50ByUsuarioIdOrderByFechaDesc(u.getId());
    }

    /** Marca como leída (solo si es del usuario) y devuelve la ruta a la que lleva el aviso. */
    @Transactional
    public String abrir(Usuario u, Long id) {
        Notificacion n = bandeja.findById(id)
                .filter(x -> x.getUsuario().getId().equals(u.getId()))
                .orElseThrow(() -> new BusinessException("Notificación no encontrada."));
        n.setLeida(true);
        return (n.getUrl() == null || n.getUrl().isBlank()) ? "/notificaciones" : n.getUrl();
    }

    @Transactional
    public void marcarTodas(Usuario u) {
        bandeja.findByUsuarioIdAndLeidaFalse(u.getId()).forEach(n -> n.setLeida(true));
    }

    // ================= Armado y envío =================

    /** Campanita + correo. */
    private void enviar(Usuario destino, String asunto, String titulo, String mensaje, String ruta, String textoBoton) {
        guardarAviso(destino, asunto, mensaje, ruta);

        // Todo se calcula aquí, en el hilo de la petición, antes de que la entidad deje de estar disponible.
        final String para = destino.getEmail();
        final String nombre = destino.getNombre();
        Context ctx = new Context();
        ctx.setVariable("nombre", nombre);
        ctx.setVariable("titulo", titulo);
        ctx.setVariable("mensaje", mensaje);
        ctx.setVariable("url", urlBase + ruta);
        ctx.setVariable("textoBoton", textoBoton);
        final String html = plantillas.process("email/notificacion", ctx);

        Runnable envio = () -> sender.enviarHtml(para, nombre, asunto, html);
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override public void afterCommit() { envio.run(); }
            });
        } else {
            envio.run();
        }
    }

    /** Solo campanita. Con sinDuplicar=true no crea otro aviso si ya hay uno sin leer en la misma ruta. */
    private void soloEnApp(Usuario destino, String titulo, String mensaje, String ruta, boolean sinDuplicar) {
        if (sinDuplicar && bandeja.existsByUsuarioIdAndUrlAndLeidaFalse(destino.getId(), ruta)) return;
        guardarAviso(destino, titulo, mensaje, ruta);
    }

    private void guardarAviso(Usuario destino, String titulo, String mensaje, String ruta) {
        Notificacion n = new Notificacion();
        n.setUsuario(destino);
        n.setTitulo(recortar(titulo, 150));
        n.setMensaje(recortar(mensaje, 600));
        n.setUrl(ruta);
        bandeja.save(n);
    }

    private static String recortar(String s, int max) {
        if (s == null) return null;
        return s.length() <= max ? s : s.substring(0, max - 1) + "…";
    }
}
