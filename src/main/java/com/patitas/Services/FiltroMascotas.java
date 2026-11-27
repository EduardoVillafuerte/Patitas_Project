package com.patitas.Services;

import com.patitas.Models.Mascota;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/** Filtros del catálogo por especie y raza (se hacen en memoria: el catálogo ya se carga completo para calcular la compatibilidad). */
public final class FiltroMascotas {

    private FiltroMascotas() {}

    /** Especies distintas (sin repetir "perro" y "Perro"), ordenadas, para el desplegable. */
    public static List<String> especiesDe(List<Mascota> lista) {
        Map<String, String> unicas = new TreeMap<>();
        for (Mascota m : lista) {
            String e = m.getEspecie();
            if (e == null || e.isBlank()) continue;
            e = e.trim();
            unicas.putIfAbsent(e.toLowerCase(Locale.ROOT),
                    e.substring(0, 1).toUpperCase(Locale.ROOT) + e.substring(1).toLowerCase(Locale.ROOT));
        }
        return new ArrayList<>(unicas.values());
    }

    /** Especie: coincidencia exacta (sin mayúsculas). Raza: contiene el texto escrito. */
    public static List<Mascota> filtrar(List<Mascota> lista, String especie, String raza) {
        String esp = norm(especie);
        String raz = norm(raza);
        return lista.stream()
                .filter(m -> esp.isEmpty() || esp.equals(norm(m.getEspecie())))
                .filter(m -> raz.isEmpty() || norm(m.getRaza()).contains(raz))
                .toList();
    }

    private static String norm(String s) {
        return s == null ? "" : s.trim().toLowerCase(Locale.ROOT);
    }
}
