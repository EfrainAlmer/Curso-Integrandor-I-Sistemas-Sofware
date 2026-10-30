package pe.edu.pitagoras.sga.model.evaluacion;

/**
 * Conclusión descriptiva tal como se imprime en la libreta.
 *
 * @param area        área curricular.
 * @param competencia competencia.
 * @param bimestre    1 a 4.
 * @param nivel       calificativo del periodo (puede ser null).
 * @param texto       logros, dificultades y recomendaciones.
 */
public record ConclusionLibreta(String area, String competencia, int bimestre, Calificativo nivel, String texto) {
}
