package com.patitas.Services;

import com.patitas.dto.MascotaConScore;
import com.patitas.Models.Enums.*;
import com.patitas.Models.*;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Motor de afinidad: parte de 100 puntos y descuenta cada vez que el perfil
 * del adoptante choca con una etiqueta o requisito de la mascota.
 */
@Service
public class CompatibilidadService {

    public MascotaConScore evaluar(Usuario a, Mascota m) {
        int score = 100;
        List<String> motivos = new ArrayList<>();
        Set<EnumEtiqueta> et = m.getEtiquetas();

        int horas = a.getHorasFueraCasa() == null ? 0 : a.getHorasFueraCasa();
        double presupuesto = a.getPresupuestoMensual() == null ? 0 : a.getPresupuestoMensual();
        boolean ninos = Boolean.TRUE.equals(a.getTieneNinos());

        if (et.contains(EnumEtiqueta.NO_APTO_NINOS) && ninos) {
            score -= 40;
            motivos.add("No es apto para niños y en tu hogar hay niños");
        }

        if (et.contains(EnumEtiqueta.REQUIERE_PATIO) && a.getTipoVivienda() != EnumTipoVivienda.CASA_CON_PATIO) {
            score -= 30;
            motivos.add("Necesita patio y tu vivienda no tiene");
        }

        if (et.contains(EnumEtiqueta.ANSIEDAD_SEPARACION)) {
            if (horas > 6) {
                score -= 35;
                motivos.add("Sufre ansiedad por separación y pasarías más de 6 horas fuera");
            } else if (horas > 4) {
                score -= 15;
                motivos.add("Sufre ansiedad por separación y pasarías varias horas fuera");
            }
        } else if (horas > 10) {
            score -= 10;
            motivos.add("Pasaría muchas horas solo");
        }

        if (presupuesto < m.getPresupuestoMinimo()) {
            double deficit = (m.getPresupuestoMinimo() - presupuesto) / m.getPresupuestoMinimo();
            score -= (int) Math.round(10 + 20 * Math.min(1.0, deficit));
            motivos.add("Su cuidado cuesta más que tu presupuesto mensual");
        }

        if (et.contains(EnumEtiqueta.COSTO_ALTO) && presupuesto < 150) {
            score -= 10;
            motivos.add("Requiere cuidados costosos");
        }

        if (m.getNivelEnergia() >= 4 && a.getTipoVivienda() == EnumTipoVivienda.DEPARTAMENTO) {
            score -= 10;
            motivos.add("Tiene mucha energía y vivirías en un departamento");
        }

        if (motivos.isEmpty()) {
            motivos.add("Tu perfil encaja muy bien con esta mascota");
        }
        return new MascotaConScore(m, Math.max(0, score), motivos);
    }
}
