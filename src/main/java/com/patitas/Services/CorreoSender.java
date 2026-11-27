package com.patitas.Services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * Envía el correo en un hilo aparte (@Async) para no hacer esperar al usuario.
 *
 * Usa la API web de Brevo por HTTPS. Se eligió esto y no SMTP porque Render (plan gratuito) bloquea
 * los puertos SMTP; HTTPS funciona en Render y en tu computador, sin dependencias nuevas.
 *
 * Si app.mail.enabled=false, o falta la clave o el remitente, NO se envía nada real: el correo se
 * muestra en el log como "CORREO SIMULADO". Un fallo del envío se registra pero nunca rompe el flujo.
 */
@Component
public class CorreoSender {

    private static final Logger log = LoggerFactory.getLogger(CorreoSender.class);

    private final boolean habilitado;
    private final String remitente;
    private final String nombreRemitente;
    private final String brevoKey;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8)).build();

    public CorreoSender(@Value("${app.mail.enabled:false}") boolean habilitado,
                        @Value("${app.mail.from:}") String remitente,
                        @Value("${app.mail.from-name:Patitas}") String nombreRemitente,
                        @Value("${app.mail.brevo-api-key:}") String brevoKey) {
        this.habilitado = habilitado;
        this.remitente = remitente;
        this.nombreRemitente = nombreRemitente;
        this.brevoKey = brevoKey;
    }

    @Async
    public void enviarHtml(String para, String nombre, String asunto, String html) {
        if (para == null || para.isBlank()) return;
        if (!habilitado || remitente.isBlank() || brevoKey.isBlank()) {
            simular(para, nombre, asunto, html);
            return;
        }
        try {
            enviarPorBrevo(para, nombre, asunto, html);
            log.info("Correo ENVIADO a {}: {}", para, asunto);
        } catch (Exception e) {
            log.error("No se pudo enviar el correo a {} ({}): {}", para, asunto, e.getMessage());
        }
    }

    private void enviarPorBrevo(String para, String nombre, String asunto, String html) throws Exception {
        String json = "{\"sender\":{\"name\":\"" + esc(nombreRemitente) + "\",\"email\":\"" + esc(remitente) + "\"},"
                + "\"to\":[{\"email\":\"" + esc(para) + "\",\"name\":\"" + esc(nombre == null ? para : nombre) + "\"}],"
                + "\"subject\":\"" + esc(asunto) + "\",\"htmlContent\":\"" + esc(html) + "\"}";
        HttpRequest req = HttpRequest.newBuilder(URI.create("https://api.brevo.com/v3/smtp/email"))
                .timeout(Duration.ofSeconds(15))
                .header("api-key", brevoKey)
                .header("Content-Type", "application/json")
                .header("accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                .build();
        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() >= 300) {
            throw new IllegalStateException("Brevo respondió " + resp.statusCode() + ": " + resp.body());
        }
    }

    private void simular(String para, String nombre, String asunto, String html) {
        String texto = html
                .replaceAll("(?s)<head>.*?</head>", " ")
                .replaceAll("(?s)<[^>]+>", " ")
                .replaceAll("\\s+", " ")
                .trim();
        log.info("""

                ========== CORREO SIMULADO (app.mail.enabled=false) ==========
                Para   : {} <{}>
                Asunto : {}
                Mensaje: {}
                ==============================================================""", nombre, para, asunto, texto);
    }

    /** Escapa un texto para colocarlo dentro de una cadena JSON. */
    private static String esc(String s) {
        StringBuilder sb = new StringBuilder();
        for (char c : s.toCharArray()) {
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> { if (c < 0x20) sb.append(String.format("\\u%04x", (int) c)); else sb.append(c); }
            }
        }
        return sb.toString();
    }
}
