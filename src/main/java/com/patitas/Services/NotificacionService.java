package com.patitas.Services;

import com.patitas.Models.PostulacionAdopcion;
import com.patitas.Models.Seguimiento;
import com.patitas.Models.Usuario;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

/**
 * Arma los correos del sistema con una plantilla HTML de Thymeleaf (templates/email/notificacion.html).
 * El envío ocurre DESPUÉS del commit de la transacción: si algo falla y se deshace, no sale ningún correo.
 */
@Service
public class NotificacionService {

    private final TemplateEngine plantillas;
    private final CorreoSender sender;
    private final String urlBase;

    public NotificacionService(TemplateEngine plantillas, CorreoSender sender,
                               @Value("${app.url:http://localhost:8080}") String urlBase) {
        this.plantillas = plantillas;
        this.sender = sender;
        this.urlBase = urlBase.endsWith("/") ? urlBase.substring(0, urlBase.length() - 1) : urlBase;
    }

    // ---- Eventos del flujo de adopción ----

    public void refugioNuevaPostulacion(PostulacionAdopcion p) {
        Usuario refugio = p.getMascota().getRefugio();
        enviar(refugio, "Nueva postulación para " + p.getMascota().getNombre(),
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
                "/refugio/panel", "Ver mi panel");
    }

    // ---- Armado y envío ----

    private void enviar(Usuario destino, String asunto, String titulo, String mensaje, String ruta, String textoBoton) {
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
}
