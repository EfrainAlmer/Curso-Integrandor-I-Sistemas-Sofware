package pe.edu.pitagoras.sga.model.evaluacion;

import java.util.Collection;

/**
 * Totales de asistencia (RF16).
 *
 * @param asistencias  días con A.
 * @param tardanzas    días con T.
 * @param faltas       faltas sin justificar.
 * @param justificadas faltas justificadas.
 */
public record ResumenAsistencia(int asistencias, int tardanzas, int faltas, int justificadas) {

    public static final ResumenAsistencia VACIO = new ResumenAsistencia(0, 0, 0, 0);

    /** @return los totales de una lista de marcas. */
    public static ResumenAsistencia de(Collection<AsistenciaRegistro> registros) {
        int a = 0;
        int t = 0;
        int f = 0;
        int j = 0;
        for (AsistenciaRegistro r : registros) {
            switch (r.estado()) {
                case ASISTIO -> a++;
                case TARDANZA -> t++;
                case FALTA -> f++;
                case FALTA_JUSTIFICADA -> j++;
            }
        }
        return new ResumenAsistencia(a, t, f, j);
    }

    public int total() {
        return asistencias + tardanzas + faltas + justificadas;
    }

    /** @return porcentaje de días en que el alumno estuvo presente (asistió o llegó tarde); 100 si no hay registros. */
    public double porcentajeAsistencia() {
        int total = total();
        return total == 0 ? 100.0 : (asistencias + tardanzas) * 100.0 / total;
    }
}
