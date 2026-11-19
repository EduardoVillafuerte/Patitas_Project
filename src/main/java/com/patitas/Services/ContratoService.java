package com.patitas.Services;

import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.ColumnText;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfGState;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfPageEventHelper;
import com.lowagie.text.pdf.PdfWriter;
import com.patitas.Models.Enums.EnumEstadoPostulacion;
import com.patitas.Models.Mascota;
import com.patitas.Models.PostulacionAdopcion;
import com.patitas.Models.Usuario;
import com.patitas.Repository.PostulacionRepository;
import com.patitas.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Genera el contrato de adopción en PDF (OpenPDF) cuando la adopción está COMPLETADA.
 * Solo pueden descargarlo el adoptante y el refugio de esa postulación.
 *
 * El diseño imita el de la aplicación: mismos colores (verde pino, verde azulado, amarillo),
 * la huella del logo de Patitas y los bloques de colores suaves de la sección "Cómo funciona".
 */
@Service
@RequiredArgsConstructor
public class ContratoService {

    private static final DateTimeFormatter FECHA =
            DateTimeFormatter.ofPattern("d 'de' MMMM 'de' yyyy", Locale.forLanguageTag("es-EC"));

    // ---- Paleta: son los mismos valores que las variables de static/css/app.css ----
    private static final Color PINE       = new Color(0x12332d); // --pine: barra superior y títulos
    private static final Color ACCENT     = new Color(0x0f766e); // --accent
    private static final Color ACCENT_DK  = new Color(0x0b5c56); // --accent-dark
    private static final Color SUN        = new Color(0xf2b134); // --sun: huella y detalles de marca
    private static final Color TONE       = new Color(0xe9efe9); // --tone: bloques secundarios
    private static final Color LINE       = new Color(0xdbe5dd); // --tone-2: líneas finas
    private static final Color INK        = new Color(0x14231e); // --ink: texto principal
    private static final Color MUTED      = new Color(0x4c5c55); // --muted: texto secundario
    private static final Color CLARO      = new Color(0xcfe3da); // texto claro sobre fondo verde pino
    private static final Color MINT       = new Color(0xd6efe0); // --mint
    private static final Color MINT_INK   = new Color(0x14532d);
    private static final Color BUTTER     = new Color(0xfcebb8); // --butter
    private static final Color BUTTER_INK = new Color(0x5f4200);
    private static final Color SKY        = new Color(0xd7e9f8); // --sky
    private static final Color SKY_INK    = new Color(0x164a6b);

    // ---- Tipografías: OpenPDF trae Helvetica incluida (no hay que cargar archivos de fuentes) ----
    private static final Font F_TITULO  = new Font(Font.HELVETICA, 24, Font.BOLD, PINE);
    private static final Font F_LEMA    = new Font(Font.HELVETICA, 11, Font.NORMAL, MUTED);
    private static final Font F_NUMERO  = new Font(Font.HELVETICA, 10, Font.BOLD, ACCENT_DK);
    private static final Font F_SECCION = new Font(Font.HELVETICA, 13, Font.BOLD, PINE);
    private static final Font F_ETIQ    = new Font(Font.HELVETICA, 8, Font.BOLD, ACCENT_DK);
    private static final Font F_NOMBRE  = new Font(Font.HELVETICA, 12, Font.BOLD, INK);
    private static final Font F_NORMAL  = new Font(Font.HELVETICA, 10, Font.NORMAL, INK);
    private static final Font F_CLAUSULA= new Font(Font.HELVETICA, 10, Font.BOLD, ACCENT_DK);
    private static final Font F_CHICO   = new Font(Font.HELVETICA, 8.5f, Font.ITALIC, MUTED);

    private final PostulacionRepository postulaciones;

    @Transactional(readOnly = true)
    public byte[] generar(Usuario solicitante, Long postulacionId) {
        // Solo el adoptante o el refugio de ESTA postulación pueden ver el contrato (seguridad por dueño).
        PostulacionAdopcion p = postulaciones.findById(postulacionId)
                .filter(x -> x.getAdoptante().getId().equals(solicitante.getId())
                          || x.getMascota().getRefugio().getId().equals(solicitante.getId()))
                .orElseThrow(() -> new BusinessException("Contrato no encontrado."));
        if (p.getEstado() != EnumEstadoPostulacion.COMPLETADA) {
            throw new BusinessException("El contrato se genera cuando la adopción está completada.");
        }

        Usuario adoptante = p.getAdoptante();
        Mascota m = p.getMascota();
        Usuario refugio = m.getRefugio();
        LocalDate fecha = p.getFechaCompletada() != null ? p.getFechaCompletada() : LocalDate.now();
        String numero = "N.° " + String.format("%06d", p.getId());

        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            // Márgenes: arriba 104 pt para dejar sitio a la banda verde del encabezado; abajo 64 pt para el pie.
            Document doc = new Document(PageSize.A4, 56, 56, 104, 64);
            PdfWriter writer = PdfWriter.getInstance(doc, out);
            // El "page event" dibuja el encabezado, el pie y la marca de agua en CADA página, sin repetir código.
            writer.setPageEvent(new MarcoPatitas(numero));
            doc.open();

            // ---------- Título ----------
            doc.add(new Paragraph("Contrato de adopción", F_TITULO));
            Paragraph lema = new Paragraph("Un compromiso de cuidado responsable para una mascota", F_LEMA);
            lema.setSpacingAfter(6);
            doc.add(lema);
            doc.add(new Paragraph(numero + "   ·   " + fecha.format(FECHA), F_NUMERO));

            // ---------- Las partes (dos bloques lado a lado) ----------
            doc.add(seccion("Las partes"));
            PdfPTable partes = new PdfPTable(new float[]{1f, 0.06f, 1f});
            partes.setWidthPercentage(100);
            partes.addCell(bloquePersona("EL REFUGIO", refugio.getNombre(),
                    "RUC " + nz(refugio.getDocumentoLegal()), refugio.getEmail()));
            partes.addCell(vacia());
            partes.addCell(bloquePersona("EL ADOPTANTE", adoptante.getNombre(),
                    "Cédula " + nz(adoptante.getCedula()) + "  ·  " + nz(adoptante.getCiudad()),
                    "Teléfono " + nz(adoptante.getTelefono())));
            doc.add(partes);

            // ---------- La mascota (fichas como en la pantalla de detalle) ----------
            doc.add(seccion("La mascota"));
            PdfPTable ficha = new PdfPTable(new float[]{1.3f, 1f, 1f, 1f, 1.2f});
            ficha.setWidthPercentage(100);
            ficha.addCell(fichaItem("Nombre", nz(m.getNombre())));
            ficha.addCell(fichaItem("Especie", nz(String.valueOf(m.getEspecie()))));
            ficha.addCell(fichaItem("Edad", m.getEdadAnios() == null ? "No registrada" : m.getEdadAnios() + " año(s)"));
            ficha.addCell(fichaItem("Vacunada", m.isVacunado() ? "Sí" : "No"));
            ficha.addCell(fichaItem("Esterilizada", m.isEsterilizado() ? "Sí" : "No"));
            doc.add(ficha);

            // ---------- Cláusulas ----------
            doc.add(seccion("Cláusulas"));
            String[][] clausulas = {
                {"PRIMERA · Objeto.", "El refugio entrega en adopción a la mascota descrita y el adoptante la recibe como integrante de su hogar."},
                {"SEGUNDA · Cuidado.", "El adoptante se compromete a darle alimento, agua, refugio, atención veterinaria y trato digno, y a no abandonarla, maltratarla ni cederla a terceros."},
                {"TERCERA · Seguimiento.", "El adoptante entregará evidencia (foto y/o comentario) en la plataforma Patitas a los 30, 90 y 180 días de esta adopción, en las fechas del calendario de seguimiento que figura más abajo."},
                {"CUARTA · Devolución.", "Si el adoptante no puede conservar a la mascota, deberá devolverla al refugio y no entregarla a otra persona."},
                {"QUINTA · Incumplimiento.", "El incumplimiento grave podrá ser reportado y el adoptante podrá ser incluido en la lista negra de la plataforma, que impide nuevas adopciones."},
                {"SEXTA · Aceptación.", "Las partes aceptan este contrato y lo firman en dos ejemplares del mismo tenor."}
            };
            for (String[] cl : clausulas) {
                Paragraph pc = new Paragraph();
                pc.setLeading(14);
                pc.setAlignment(Element.ALIGN_JUSTIFIED);
                pc.setSpacingBefore(6);
                pc.add(new Chunk(cl[0] + " ", F_CLAUSULA)); // título de la cláusula en color de marca
                pc.add(new Chunk(cl[1], F_NORMAL));
                doc.add(pc);
            }

            // ---------- Calendario de seguimiento (los tres colores de "Cómo funciona") ----------
            doc.add(seccion("Calendario de seguimiento"));
            PdfPTable cal = new PdfPTable(3);
            cal.setWidthPercentage(100);
            cal.addCell(hito("30 días", fecha.plusDays(30).format(FECHA), MINT, MINT_INK));
            cal.addCell(hito("90 días", fecha.plusDays(90).format(FECHA), BUTTER, BUTTER_INK));
            cal.addCell(hito("180 días", fecha.plusDays(180).format(FECHA), SKY, SKY_INK));
            doc.add(cal);

            // ---------- Firmas ----------
            PdfPTable firmas = new PdfPTable(new float[]{1f, 0.12f, 1f});
            firmas.setWidthPercentage(100);
            firmas.setSpacingBefore(48);
            firmas.setKeepTogether(true); // que las firmas nunca queden partidas entre dos páginas
            firmas.addCell(firma(refugio.getNombre(), "RUC " + nz(refugio.getDocumentoLegal()), "EL REFUGIO"));
            firmas.addCell(vacia());
            firmas.addCell(firma(adoptante.getNombre(), "C.I. " + nz(adoptante.getCedula()), "EL ADOPTANTE"));
            doc.add(firmas);

            Paragraph pie = new Paragraph("Documento generado automáticamente por Patitas el " + LocalDate.now().format(FECHA) + ".", F_CHICO);
            pie.setSpacingBefore(22);
            pie.setAlignment(Element.ALIGN_CENTER);
            doc.add(pie);

            doc.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new BusinessException("No se pudo generar el contrato.");
        }
    }

    // =====================================================================
    //  Encabezado, pie y marca de agua: se dibujan solos en cada página
    // =====================================================================
    private static class MarcoPatitas extends PdfPageEventHelper {
        private final String numero;

        MarcoPatitas(String numero) {
            this.numero = numero;
        }

        /** Al empezar la página: huella gigante y muy tenue DETRÁS del texto (getDirectContentUnder = capa de abajo). */
        @Override
        public void onStartPage(PdfWriter writer, Document document) {
            PdfContentByte fondo = writer.getDirectContentUnder();
            Rectangle pag = document.getPageSize();
            fondo.saveState();
            PdfGState transparencia = new PdfGState();
            transparencia.setFillOpacity(0.05f); // 5 %: se nota como textura, pero no estorba al leer ni al imprimir
            fondo.setGState(transparencia);
            dibujarHuella(fondo, pag.getWidth() / 2 - 160, pag.getHeight() / 2 + 150, 320, ACCENT);
            fondo.restoreState();
        }

        /** Al terminar la página: banda verde con el logo arriba y línea con número de página abajo. */
        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            PdfContentByte cb = writer.getDirectContent();
            Rectangle pag = document.getPageSize();
            float w = pag.getWidth();
            float h = pag.getHeight();
            float banda = 72;

            // Banda verde pino y filete amarillo (igual que la barra de navegación de la web)
            cb.saveState();
            cb.setColorFill(PINE);
            cb.rectangle(0, h - banda, w, banda);
            cb.fill();
            cb.setColorFill(SUN);
            cb.rectangle(0, h - banda - 4, w, 4);
            cb.fill();
            cb.restoreState();

            // Logo: huella amarilla + "Patitas" blanco + punto amarillo
            dibujarHuella(cb, 56, h - 21, 30, SUN);
            Phrase marca = new Phrase();
            marca.add(new Chunk("Patitas", new Font(Font.HELVETICA, 22, Font.BOLD, Color.WHITE)));
            marca.add(new Chunk(".", new Font(Font.HELVETICA, 22, Font.BOLD, SUN)));
            ColumnText.showTextAligned(cb, Element.ALIGN_LEFT, marca, 96, h - 43, 0);

            // Texto a la derecha de la banda
            ColumnText.showTextAligned(cb, Element.ALIGN_RIGHT,
                    new Phrase("Contrato de adopción", new Font(Font.HELVETICA, 10, Font.BOLD, Color.WHITE)), w - 56, h - 32, 0);
            ColumnText.showTextAligned(cb, Element.ALIGN_RIGHT,
                    new Phrase(numero, new Font(Font.HELVETICA, 9, Font.NORMAL, CLARO)), w - 56, h - 46, 0);

            // Pie: línea fina, lema a la izquierda y número de página a la derecha
            cb.saveState();
            cb.setColorStroke(LINE);
            cb.setLineWidth(0.8f);
            cb.moveTo(56, 46);
            cb.lineTo(w - 56, 46);
            cb.stroke();
            cb.restoreState();
            Font fPie = new Font(Font.HELVETICA, 8.5f, Font.NORMAL, MUTED);
            ColumnText.showTextAligned(cb, Element.ALIGN_LEFT, new Phrase("Patitas  ·  Cada patita merece un hogar", fPie), 56, 31, 0);
            ColumnText.showTextAligned(cb, Element.ALIGN_RIGHT, new Phrase("Página " + writer.getPageNumber(), fPie), w - 56, 31, 0);
        }
    }

    /**
     * Dibuja la huella del logo con vectores (no usa imágenes, así no hay archivos que copiar).
     * Son EXACTAMENTE las mismas formas del SVG de layout.html (lienzo de 24 x 24):
     * cuatro dedos (círculos) y una almohadilla (curvas).
     *
     * @param x         borde izquierdo
     * @param yArriba   borde superior (en PDF el eje Y crece hacia ARRIBA, por eso se resta)
     * @param tam       ancho/alto total en puntos
     */
    private static void dibujarHuella(PdfContentByte cb, float x, float yArriba, float tam, Color color) {
        float s = tam / 24f; // factor de escala del lienzo de 24 al tamaño pedido
        cb.saveState();
        cb.setColorFill(color);

        // Dedos: {centroX, centroY, radio}
        float[][] dedos = {{5.5f, 10f, 2.2f}, {9.5f, 5.5f, 2.2f}, {14.5f, 5.5f, 2.2f}, {18.5f, 10f, 2.2f}};
        for (float[] d : dedos) {
            cb.circle(x + d[0] * s, yArriba - d[1] * s, d[2] * s);
        }
        cb.fill();

        // Almohadilla: 6 curvas de Bézier {control1X, control1Y, control2X, control2Y, finalX, finalY}
        float[][] curvas = {
            {9f, 11f, 6f, 14.2f, 6f, 17f},
            {6f, 19f, 7.6f, 20f, 9.2f, 20f},
            {10.4f, 20f, 11.1f, 19.4f, 12f, 19.4f},
            {12.9f, 19.4f, 13.6f, 20f, 14.8f, 20f},
            {16.4f, 20f, 18f, 19f, 18f, 17f},
            {18f, 14.2f, 15f, 11f, 12f, 11f}
        };
        cb.moveTo(x + 12f * s, yArriba - 11f * s);
        for (float[] c : curvas) {
            cb.curveTo(x + c[0] * s, yArriba - c[1] * s, x + c[2] * s, yArriba - c[3] * s, x + c[4] * s, yArriba - c[5] * s);
        }
        cb.closePath();
        cb.fill();
        cb.restoreState();
    }

    // =====================================================================
    //  Piezas pequeñas reutilizables 
    // =====================================================================

   
    private static PdfPTable seccion(String texto) {
        PdfPTable t = new PdfPTable(1);
        t.setWidthPercentage(100);
        t.setSpacingBefore(18);
        t.setSpacingAfter(6);
        PdfPCell c = new PdfPCell(new Phrase(texto, F_SECCION));
        c.setBorder(Rectangle.LEFT);
        c.setBorderColorLeft(SUN);
        c.setBorderWidthLeft(3.5f);
        c.setPaddingLeft(9);
        c.setPaddingTop(2);
        c.setPaddingBottom(4);
        t.addCell(c);
        return t;
    }

   
    private static PdfPCell bloquePersona(String rol, String nombre, String linea1, String linea2) {
        PdfPCell c = new PdfPCell();
        c.setBorder(Rectangle.NO_BORDER);
        c.setBackgroundColor(TONE);
        c.setPadding(12);
        c.addElement(new Paragraph(rol, F_ETIQ));
        Paragraph n = new Paragraph(nz(nombre), F_NOMBRE);
        n.setSpacingBefore(3);
        c.addElement(n);
        Paragraph d1 = new Paragraph(linea1, F_NORMAL);
        d1.setSpacingBefore(4);
        c.addElement(d1);
        c.addElement(new Paragraph(nz(linea2), F_NORMAL));
        return c;
    }

   
    private static PdfPCell fichaItem(String etiqueta, String valor) {
        PdfPCell c = new PdfPCell();
        c.setBackgroundColor(TONE);
        c.setBorderColor(Color.WHITE); 
        c.setBorderWidth(3);
        c.setPadding(8);
        c.addElement(new Paragraph(etiqueta.toUpperCase(Locale.ROOT), new Font(Font.HELVETICA, 7.5f, Font.BOLD, MUTED)));
        Paragraph v = new Paragraph(valor, F_NOMBRE);
        v.setSpacingBefore(2);
        c.addElement(v);
        return c;
    }

   
    private static PdfPCell hito(String dias, String fecha, Color fondo, Color tinta) {
        PdfPCell c = new PdfPCell();
        c.setBackgroundColor(fondo);
        c.setBorderColor(Color.WHITE);
        c.setBorderWidth(3);
        c.setPadding(11);
        c.addElement(new Paragraph(dias, new Font(Font.HELVETICA, 15, Font.BOLD, tinta)));
        Paragraph f = new Paragraph(fecha, new Font(Font.HELVETICA, 9.5f, Font.NORMAL, tinta));
        f.setSpacingBefore(2);
        c.addElement(f);
        return c;
    }

    
    private static PdfPCell firma(String nombre, String documento, String rol) {
        PdfPCell c = new PdfPCell();
        c.setBorder(Rectangle.TOP);
        c.setBorderColorTop(INK);
        c.setBorderWidthTop(0.8f);
        c.setPaddingTop(7);
        Paragraph n = new Paragraph(nz(nombre), F_NOMBRE);
        n.setAlignment(Element.ALIGN_CENTER);
        c.addElement(n);
        Paragraph d = new Paragraph(documento, F_NORMAL);
        d.setAlignment(Element.ALIGN_CENTER);
        c.addElement(d);
        Paragraph r = new Paragraph(rol, F_ETIQ);
        r.setAlignment(Element.ALIGN_CENTER);
        r.setSpacingBefore(2);
        c.addElement(r);
        return c;
    }

 
    private static PdfPCell vacia() {
        PdfPCell c = new PdfPCell(new Phrase(""));
        c.setBorder(Rectangle.NO_BORDER);
        return c;
    }


    private static String nz(String s) {
        return (s == null || s.isBlank() || "null".equals(s)) ? "no registrado" : s;
    }
}
