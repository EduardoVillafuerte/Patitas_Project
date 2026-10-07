package com.patitas.Models.Enums;

public enum EnumEtiqueta {
    REQUIERE_PATIO("Requiere patio"),
    ANSIEDAD_SEPARACION("Ansiedad por separación"),
    NO_APTO_NINOS("No apto para niños"),
    COSTO_ALTO("Cuidados costosos");

    private final String label;
    EnumEtiqueta(String label) { this.label = label; }
    public String getLabel() { return label; }
}
