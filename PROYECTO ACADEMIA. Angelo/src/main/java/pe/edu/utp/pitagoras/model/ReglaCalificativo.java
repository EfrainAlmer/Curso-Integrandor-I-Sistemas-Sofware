package pe.edu.utp.pitagoras.model;

import java.util.Set;

/**
 * Clase de análisis ReglaCalificativo (Tabla 6, pág. 38 y Tabla 9, pág. 41).
 * Implementa las reglas de negocio del MINEDU:
 * - RF13 / CU13: Validación en tiempo real de la escala literal permitida (AD, A, B, C).
 * - RF14 / CU14: Determinación del calificativo del área y calificativo anual tomando
 *   el calificativo registrado en el último bimestre (Bimestre 4), sin promediar los bimestres.
 */
public final class ReglaCalificativo {
    private static final Set<String> ESCALA_MINEDU = Set.of("AD", "A", "B", "C");

    private ReglaCalificativo() {
    }

    public static boolean esCalificativoValido(String calificativo) {
        return calificativo != null && ESCALA_MINEDU.contains(calificativo.trim().toUpperCase());
    }

    /**
     * Calcula el calificativo consolidado del área en un periodo a partir de sus competencias
     * empleando el nivel de logro predominante conforme a la escala literal MINEDU.
     */
    public static String calcularCalificativoArea(String[] competencias) {
        if (competencias == null || competencias.length == 0) {
            return "A";
        }
        int sumaPuntos = 0;
        boolean tieneC = false;
        for (String c : competencias) {
            int p = switch (c) {
                case "AD" -> 4;
                case "A"  -> 3;
                case "B"  -> 2;
                default   -> 1;
            };
            if ("C".equals(c)) {
                tieneC = true;
            }
            sumaPuntos += p;
        }
        if (tieneC && sumaPuntos <= 7) {
            return "C";
        }
        int redondeado = (int) Math.round((double) sumaPuntos / competencias.length);
        return switch (redondeado) {
            case 4 -> "AD";
            case 3 -> "A";
            case 2 -> "B";
            default -> "C";
        };
    }

    /**
     * RF14 / CU14 (pág. 26 y 31):
     * "El sistema deberá determinar el calificativo anual de cada área tomando el calificativo
     * registrado en el último bimestre, sin promediar los bimestres."
     */
    public static String determinarCalificativoAnual(String calificativoUltimoBimestre, boolean cuartoBimestreRegistrado) {
        if (!cuartoBimestreRegistrado || calificativoUltimoBimestre == null || calificativoUltimoBimestre.isBlank()) {
            return "Pendiente (Cierre Bim. 4)";
        }
        return calificativoUltimoBimestre;
    }

    public static String obtenerDescripcionEscala(String calificativo) {
        return switch (calificativo) {
            case "AD" -> "Logro destacado (AD)";
            case "A"  -> "Logro esperado (A)";
            case "B"  -> "En proceso (B)";
            case "C"  -> "En inicio / En riesgo (C)";
            default   -> "Pendiente";
        };
    }
}
