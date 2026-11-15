package com.patitas.dto;

import java.util.List;

/** Datos de un gráfico: una etiqueta y un valor por cada barra o porción. */
public class GraficoDatos {
    private final List<String> etiquetas;
    private final List<Long> valores;

    public GraficoDatos(List<String> etiquetas, List<Long> valores) {
        this.etiquetas = etiquetas;
        this.valores = valores;
    }

    public List<String> getEtiquetas() { return etiquetas; }
    public List<Long> getValores() { return valores; }

    /** Suma de valores: sirve para mostrar "sin datos" cuando todo está en cero. */
    public long getTotal() { return valores.stream().mapToLong(Long::longValue).sum(); }
}
