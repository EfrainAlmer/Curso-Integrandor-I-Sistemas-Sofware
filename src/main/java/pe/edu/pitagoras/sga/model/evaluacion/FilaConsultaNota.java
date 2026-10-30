package pe.edu.pitagoras.sga.model.evaluacion;

/**
 * Fila de la consulta de notas del apoderado (RF15).
 *
 * @param area         nombre del área.
 * @param competencia  nombre de la competencia ("Calificativo del área" en la fila de resumen).
 * @param calificativo calificativo, o null si aún no se registra.
 * @param esResumen    true en la fila que muestra el calificativo del área.
 * @param conclusion   conclusión descriptiva del docente (puede ser null).
 */
public record FilaConsultaNota(String area, String competencia, Calificativo calificativo, boolean esResumen,
                               String conclusion) {
}
