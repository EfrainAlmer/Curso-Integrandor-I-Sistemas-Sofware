package pe.edu.pitagoras.sga.model.evaluacion;

/**
 * Calificativo que el docente quiere guardar.
 *
 * @param alumnoId          alumno.
 * @param codigoCompetencia '1' a '4'.
 * @param calificativo      nuevo valor.
 */
public record CambioNota(int alumnoId, String codigoCompetencia, Calificativo calificativo) {
}
