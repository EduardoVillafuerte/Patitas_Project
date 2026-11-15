package com.patitas.Services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * SIMULACIÓN de envío de correo.
 * No se conecta a ningún servidor de correo: muestra en la consola (log) el correo tal como
 * se enviaría, con destinatario, asunto y mensaje. Así el flujo de notificaciones queda
 * demostrable sin contraseñas ni servicios externos.
 * Para hacerlo real más adelante solo hay que cambiar el cuerpo de enviarHtml(...).
 */
@Component
public class CorreoSender {

    private static final Logger log = LoggerFactory.getLogger(CorreoSender.class);

    public void enviarHtml(String para, String nombre, String asunto, String html) {
        String texto = html
                .replaceAll("(?s)<head>.*?</head>", " ")
                .replaceAll("(?s)<[^>]+>", " ")
                .replaceAll("\\s+", " ")
                .trim();
        log.info("""

                ========== CORREO SIMULADO ==========
                Para   : {} <{}>
                Asunto : {}
                Mensaje: {}
                =====================================""", nombre, para, asunto, texto);
    }
}
