package com.patitas.validation;

/**
 * Validación de identificaciones ecuatorianas (cédula y RUC).
 * Se ejecuta SIEMPRE en el servidor, antes de guardar en la base de datos.
 * El HTML/JS del navegador solo ayuda al usuario; aquí está la validación real.
 */
public final class IdentificacionEc {

    private IdentificacionEc() {}

    /** Cédula: 10 dígitos, provincia válida, tercer dígito < 6 y dígito verificador (módulo 10). */
    public static boolean esCedulaValida(String cedula) {
        if (cedula == null || !cedula.matches("\\d{10}")) return false;
        if (!provinciaValida(cedula)) return false;
        if (cedula.charAt(2) - '0' > 5) return false;

        int suma = 0;
        for (int i = 0; i < 9; i++) {
            int d = cedula.charAt(i) - '0';
            int m = (i % 2 == 0) ? d * 2 : d;
            if (m > 9) m -= 9;
            suma += m;
        }
        int verificador = (10 - (suma % 10)) % 10;
        return verificador == cedula.charAt(9) - '0';
    }

    /**
     * RUC: 13 dígitos. Según el tercer dígito:
     *  - 0 a 5: persona natural (cédula válida + establecimiento).
     *  - 6: entidad pública (módulo 11).
     *  - 9: sociedad privada (módulo 11).
     */
    public static boolean esRucValido(String ruc) {
        if (ruc == null || !ruc.matches("\\d{13}")) return false;
        if (!provinciaValida(ruc)) return false;

        int tercero = ruc.charAt(2) - '0';
        if (tercero <= 5) {
            return esCedulaValida(ruc.substring(0, 10)) && !ruc.substring(10).equals("000");
        }
        if (tercero == 6) {
            return modulo11(ruc, new int[]{3, 2, 7, 6, 5, 4, 3, 2}, 8) && !ruc.substring(9).equals("0000");
        }
        if (tercero == 9) {
            return modulo11(ruc, new int[]{4, 3, 2, 7, 6, 5, 4, 3, 2}, 9) && !ruc.substring(10).equals("000");
        }
        return false;
    }

    private static boolean provinciaValida(String id) {
        int provincia = Integer.parseInt(id.substring(0, 2));
        return (provincia >= 1 && provincia <= 24) || provincia == 30;
    }

    private static boolean modulo11(String id, int[] coeficientes, int posVerificador) {
        int suma = 0;
        for (int i = 0; i < coeficientes.length; i++) {
            suma += (id.charAt(i) - '0') * coeficientes[i];
        }
        int residuo = suma % 11;
        int verificador = (residuo == 0) ? 0 : 11 - residuo;
        return verificador == id.charAt(posVerificador) - '0';
    }
}
