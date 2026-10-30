package pe.edu.pitagoras.sga.model.academico;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Bimestre del año escolar. Cuando está cerrado no se pueden editar notas ni asistencia (RF07).
 *
 * @param periodoId   identificador.
 * @param anioLectivo año escolar.
 * @param bimestre    1 a 4.
 * @param fechaInicio primer día del bimestre.
 * @param fechaFin    último día del bimestre.
 * @param cerrado     true si la edición está bloqueada.
 * @param fechaCierre cuándo se cerró (null si está abierto).
 * @param cerradoPor  usuario que lo cerró (null si está abierto).
 */
public record Periodo(int periodoId, int anioLectivo, int bimestre, LocalDate fechaInicio,
                      LocalDate fechaFin, boolean cerrado, LocalDateTime fechaCierre, String cerradoPor) {

    /** @return true si la fecha cae dentro del bimestre (ambos extremos incluidos). */
    public boolean contiene(LocalDate fecha) {
        return !fecha.isBefore(fechaInicio) && !fecha.isAfter(fechaFin);
    }

    /** @return "Bimestre 2 - 2026". */
    public String etiqueta() {
        return "Bimestre " + bimestre + " - " + anioLectivo;
    }

    @Override
    public String toString() {
        return etiqueta() + (cerrado ? " (cerrado)" : "");
    }
}
