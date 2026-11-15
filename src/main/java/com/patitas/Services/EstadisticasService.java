package com.patitas.Services;

import com.patitas.Models.Enums.EnumEstadoMascota;
import com.patitas.Models.Enums.EnumEstadoPostulacion;
import com.patitas.Models.PostulacionAdopcion;
import com.patitas.Models.Usuario;
import com.patitas.Repository.MascotaRepository;
import com.patitas.Repository.PostulacionRepository;
import com.patitas.dto.GraficoDatos;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Datos para los gráficos (Chart.js). Si el parámetro "refugio" es null devuelve las cifras de toda
 * la plataforma (administrador); si viene un refugio, solo las suyas.
 */
@Service
@RequiredArgsConstructor
public class EstadisticasService {

    private final MascotaRepository mascotas;
    private final PostulacionRepository postulaciones;

    public GraficoDatos mascotasPorEstado(Usuario refugio) {
        List<String> et = List.of("Disponibles", "En proceso", "Adoptadas");
        List<Long> val = new ArrayList<>();
        for (EnumEstadoMascota e : List.of(EnumEstadoMascota.DISPONIBLE, EnumEstadoMascota.EN_PROCESO, EnumEstadoMascota.ADOPTADO)) {
            val.add(refugio == null
                    ? mascotas.countByActivoTrueAndEstado(e)
                    : mascotas.countByRefugioAndActivoTrueAndEstado(refugio, e));
        }
        return new GraficoDatos(et, val);
    }

    public GraficoDatos postulacionesPorEstado(Usuario refugio) {
        List<String> et = List.of("Pendientes", "Aprobadas", "Rechazadas", "Completadas");
        List<Long> val = new ArrayList<>();
        for (EnumEstadoPostulacion e : List.of(EnumEstadoPostulacion.PENDIENTE, EnumEstadoPostulacion.APROBADA,
                EnumEstadoPostulacion.RECHAZADA, EnumEstadoPostulacion.COMPLETADA)) {
            val.add(refugio == null
                    ? postulaciones.countByEstado(e)
                    : postulaciones.countByMascotaRefugioAndEstado(refugio, e));
        }
        return new GraficoDatos(et, val);
    }

    /** Adopciones completadas en cada uno de los últimos 6 meses (incluye los meses con cero). */
    @Transactional(readOnly = true)
    public GraficoDatos adopcionesPorMes(Usuario refugio) {
        List<PostulacionAdopcion> completadas = refugio == null
                ? postulaciones.findByEstado(EnumEstadoPostulacion.COMPLETADA)
                : postulaciones.findByMascotaRefugioAndEstado(refugio, EnumEstadoPostulacion.COMPLETADA);

        YearMonth actual = YearMonth.from(LocalDate.now());
        Map<YearMonth, Long> porMes = new LinkedHashMap<>();
        for (int i = 5; i >= 0; i--) porMes.put(actual.minusMonths(i), 0L);
        for (PostulacionAdopcion p : completadas) {
            if (p.getFechaCompletada() == null) continue;
            YearMonth ym = YearMonth.from(p.getFechaCompletada());
            porMes.computeIfPresent(ym, (k, v) -> v + 1);
        }

        DateTimeFormatter f = DateTimeFormatter.ofPattern("MMM yyyy", Locale.forLanguageTag("es"));
        List<String> et = new ArrayList<>();
        List<Long> val = new ArrayList<>();
        porMes.forEach((ym, n) -> { et.add(ym.format(f)); val.add(n); });
        return new GraficoDatos(et, val);
    }
}
