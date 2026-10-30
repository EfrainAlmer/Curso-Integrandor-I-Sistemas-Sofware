package pe.edu.pitagoras.sga.model.evaluacion;

import pe.edu.pitagoras.sga.model.academico.Alumno;

/**
 * Totales de asistencia de un alumno (reporte de asistencia por sección y área).
 *
 * @param alumno  alumno.
 * @param resumen totales.
 */
public record ResumenAlumno(Alumno alumno, ResumenAsistencia resumen) {
}
