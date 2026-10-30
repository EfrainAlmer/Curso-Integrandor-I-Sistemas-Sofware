package pe.edu.pitagoras.sga.model.evaluacion;

/**
 * Estado que el docente quiere guardar para un alumno en una fecha.
 *
 * @param alumnoId    alumno.
 * @param estado      estado elegido.
 * @param observacion texto opcional (máximo 150 caracteres).
 */
public record MarcaAsistencia(int alumnoId, EstadoAsistencia estado, String observacion) {
}
