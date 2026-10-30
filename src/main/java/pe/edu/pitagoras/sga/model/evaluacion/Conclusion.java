package pe.edu.pitagoras.sga.model.evaluacion;

/**
 * Conclusión descriptiva del docente: logros, dificultades y recomendaciones de un alumno en una
 * competencia y periodo (sale en la libreta, formato del Informe de progreso del MINEDU).
 *
 * @param alumnoId          alumno.
 * @param cursoSeccionId    carga académica.
 * @param cursoId           área.
 * @param codigoCompetencia '1' a '4'.
 * @param periodoId         periodo.
 * @param bimestre          1 a 4.
 * @param texto             conclusión (máximo 500 caracteres).
 */
public record Conclusion(int alumnoId, int cursoSeccionId, int cursoId, String codigoCompetencia, int periodoId,
                         int bimestre, String texto) {
}
