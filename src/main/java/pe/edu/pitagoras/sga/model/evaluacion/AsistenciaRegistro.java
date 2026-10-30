package pe.edu.pitagoras.sga.model.evaluacion;

import java.time.LocalDate;

/**
 * Una marca de asistencia guardada.
 *
 * @param alumnoId       alumno.
 * @param cursoSeccionId carga académica.
 * @param curso          nombre del área.
 * @param fecha          día de clase.
 * @param estado         asistió, tardanza, falta o falta justificada.
 * @param observacion    nota opcional del docente.
 */
public record AsistenciaRegistro(int alumnoId, int cursoSeccionId, String curso, LocalDate fecha,
                                 EstadoAsistencia estado, String observacion) {
}
