package com.patitas.Services;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
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

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Genera el contrato de adopción en PDF (OpenPDF) cuando la adopción está COMPLETADA.
 * Solo pueden descargarlo el adoptante y el refugio de esa postulación.
 */
@Service
@RequiredArgsConstructor
public class ContratoService {

    private static final DateTimeFormatter FECHA =
            DateTimeFormatter.ofPattern("d 'de' MMMM 'de' yyyy", Locale.forLanguageTag("es-EC"));

    private final PostulacionRepository postulaciones;

    @Transactional(readOnly = true)
    public byte[] generar(Usuario solicitante, Long postulacionId) {
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

        Font titulo = new Font(Font.HELVETICA, 17, Font.BOLD);
        Font sub = new Font(Font.HELVETICA, 11, Font.BOLD);
        Font normal = new Font(Font.HELVETICA, 10.5f, Font.NORMAL);
        Font chico = new Font(Font.HELVETICA, 9, Font.ITALIC);

        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            Document doc = new Document(PageSize.A4, 56, 56, 56, 56);
            PdfWriter.getInstance(doc, out);
            doc.open();

            Paragraph t = new Paragraph("CONTRATO DE ADOPCIÓN DE MASCOTA", titulo);
            t.setAlignment(Element.ALIGN_CENTER);
            doc.add(t);
            Paragraph n = new Paragraph("N.° " + String.format("%06d", p.getId()) + "  ·  " + fecha.format(FECHA), chico);
            n.setAlignment(Element.ALIGN_CENTER);
            n.setSpacingAfter(16);
            doc.add(n);

            doc.add(new Paragraph("Las partes", sub));
            doc.add(new Paragraph("EL REFUGIO: " + refugio.getNombre() + ", RUC " + nz(refugio.getDocumentoLegal())
                    + ", correo " + refugio.getEmail() + ".", normal));
            doc.add(new Paragraph("EL ADOPTANTE: " + adoptante.getNombre() + ", cédula " + nz(adoptante.getCedula())
                    + ", ciudad " + nz(adoptante.getCiudad()) + ", teléfono " + nz(adoptante.getTelefono()) + ".", normal));

            Paragraph h = new Paragraph("La mascota", sub);
            h.setSpacingBefore(10);
            doc.add(h);
            doc.add(new Paragraph("Nombre: " + m.getNombre() + "  ·  Especie: " + m.getEspecie()
                    + "  ·  Edad: " + (m.getEdadAnios() == null ? "no registrada" : m.getEdadAnios() + " año(s)")
                    + "  ·  Vacunada: " + (m.isVacunado() ? "sí" : "no")
                    + "  ·  Esterilizada: " + (m.isEsterilizado() ? "sí" : "no") + ".", normal));

            Paragraph c = new Paragraph("Cláusulas", sub);
            c.setSpacingBefore(10);
            doc.add(c);
            String[] clausulas = {
                "PRIMERA (Objeto). El refugio entrega en adopción a la mascota descrita y el adoptante la recibe como integrante de su hogar.",
                "SEGUNDA (Cuidado). El adoptante se compromete a darle alimento, agua, refugio, atención veterinaria y trato digno, y a no abandonarla, maltratarla ni cederla a terceros.",
                "TERCERA (Seguimiento). El adoptante entregará evidencia (foto y/o comentario) en la plataforma Patitas a los 30 días ("
                        + fecha.plusDays(30).format(FECHA) + "), 90 días (" + fecha.plusDays(90).format(FECHA)
                        + ") y 180 días (" + fecha.plusDays(180).format(FECHA) + ") de esta adopción.",
                "CUARTA (Devolución). Si el adoptante no puede conservar a la mascota, deberá devolverla al refugio y no entregarla a otra persona.",
                "QUINTA (Incumplimiento). El incumplimiento grave podrá ser reportado y el adoptante podrá ser incluido en la lista negra de la plataforma, que impide nuevas adopciones.",
                "SEXTA (Aceptación). Las partes aceptan este contrato y lo firman en dos ejemplares del mismo tenor."
            };
            for (String cl : clausulas) {
                Paragraph pc = new Paragraph(cl, normal);
                pc.setSpacingBefore(4);
                doc.add(pc);
            }

            // Firmas
            PdfPTable firmas = new PdfPTable(2);
            firmas.setWidthPercentage(100);
            firmas.setSpacingBefore(46);
            firmas.addCell(celda("______________________________\n" + refugio.getNombre() + "\nRUC " + nz(refugio.getDocumentoLegal()) + "\nEL REFUGIO", normal));
            firmas.addCell(celda("______________________________\n" + adoptante.getNombre() + "\nC.I. " + nz(adoptante.getCedula()) + "\nEL ADOPTANTE", normal));
            doc.add(firmas);

            Paragraph pie = new Paragraph("Documento generado automáticamente por Patitas el " + LocalDate.now().format(FECHA) + ".", chico);
            pie.setSpacingBefore(24);
            pie.setAlignment(Element.ALIGN_CENTER);
            doc.add(pie);

            doc.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new BusinessException("No se pudo generar el contrato.");
        }
    }

    private static PdfPCell celda(String texto, Font f) {
        PdfPCell cell = new PdfPCell(new Phrase(texto, f));
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        return cell;
    }

    private static String nz(String s) {
        return (s == null || s.isBlank()) ? "no registrado" : s;
    }
}
