package com.patitas.Models.Enums;

public enum EnumExperiencia {
    NINGUNA("Primera vez con una mascota"),
    POCA("He tenido mascotas antes"),
    MUCHA("Mucha experiencia con mascotas");

    private final String label;
    EnumExperiencia(String label) { this.label = label; }
    public String getLabel() { return label; }
}
