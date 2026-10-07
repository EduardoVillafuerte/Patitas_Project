package com.patitas.Models.Enums;

public enum EnumTipoVivienda {
    DEPARTAMENTO("Departamento"),
    CASA_SIN_PATIO("Casa sin patio"),
    CASA_CON_PATIO("Casa con patio");

    private final String label;
    EnumTipoVivienda(String label) { this.label = label; }
    public String getLabel() { return label; }
}
