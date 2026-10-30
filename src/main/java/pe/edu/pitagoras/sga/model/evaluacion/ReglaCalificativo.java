package pe.edu.pitagoras.sga.model.evaluacion;

import com.google.common.base.Preconditions;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;

/**
 * Reglas de calificación del MINEDU (basado en la clase de Angelo Paredes).
 * <ul>
 *   <li>Calificativo del área en un periodo: promedio redondeado de los puntos de sus
 *       competencias (AD=4, A=3, B=2, C=1). Si alguna competencia tiene C y la suma no
 *       pasa de 7, el área queda en C.</li>
 *   <li>RF14: el calificativo anual es el del último bimestre, sin promediar.</li>
 * </ul>
 */
public final class ReglaCalificativo {

    /** Bimestre cuyo calificativo se toma como anual (RF14). */
    public static final int ULTIMO_BIMESTRE = 4;
    private static final int SUMA_MAXIMA_PARA_C = 7;

    private ReglaCalificativo() {
    }

    /**
     * @param competencias calificativos de las competencias del área en un periodo.
     * @return el calificativo del área, o vacío si aún no hay ninguno registrado.
     */
    public static Optional<Calificativo> calificativoArea(Collection<Calificativo> competencias) {
        if (competencias == null || competencias.isEmpty()) {
            return Optional.empty();
        }
        int suma = 0;
        boolean tieneC = false;
        for (Calificativo c : competencias) {
            Preconditions.checkNotNull(c, "calificativo nulo");
            suma += c.getPuntos();
            tieneC |= c == Calificativo.C;
        }
        if (tieneC && suma <= SUMA_MAXIMA_PARA_C) {
            return Optional.of(Calificativo.C);
        }
        int redondeado = (int) Math.round((double) suma / competencias.size());
        return Optional.of(Calificativo.desdePuntos(redondeado));
    }

    /**
     * RF14: toma el calificativo del bimestre 4, sin promediar los anteriores.
     *
     * @param porBimestre calificativo del área en cada bimestre (clave 1 a 4).
     * @return el calificativo anual, o vacío si el bimestre 4 aún no tiene calificativo.
     */
    public static Optional<Calificativo> calificativoAnual(Map<Integer, Calificativo> porBimestre) {
        if (porBimestre == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(porBimestre.get(ULTIMO_BIMESTRE));
    }

    /**
     * Situación final del estudiante al cierre del año (regla simplificada del colegio, basada en las
     * áreas con calificativo anual C).
     *
     * @param anuales calificativo anual de cada área; vacío si el área aún no tiene bimestre 4.
     * @return texto de la situación final.
     */
    public static String situacionFinal(java.util.Collection<Optional<Calificativo>> anuales) {
        if (anuales.isEmpty() || anuales.stream().anyMatch(Optional::isEmpty)) {
            return "En proceso: el año escolar aún no concluye";
        }
        long enC = anuales.stream().filter(a -> a.get() == Calificativo.C).count();
        if (enC == 0) {
            return "Promovido de grado (PRO)";
        }
        return enC <= 3 ? "Requiere recuperación pedagógica en " + enC + " área(s) (RR)"
                : "Permanece en el grado (PER)";
    }

    /** @return texto para la columna de estado del consolidado: EN INICIO, EN PROCESO o LOGRADO. */
    public static String estadoAprobacion(Calificativo area) {
        return switch (area) {
            case C -> "EN INICIO";
            case B -> "EN PROCESO";
            case A, AD -> "LOGRADO";
        };
    }
}
