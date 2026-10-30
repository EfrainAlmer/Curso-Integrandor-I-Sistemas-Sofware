package pe.edu.pitagoras.sga.model.evaluacion;

import java.util.Map;
import pe.edu.pitagoras.sga.model.academico.Alumno;

/**
 * Fila del consolidado bimestral por sección (RF19): un alumno, el calificativo de cada área
 * y el resumen de su asistencia en el periodo.
 *
 * @param alumno      alumno.
 * @param porCurso    cursoId → calificativo del área (puede faltar si no tiene notas).
 * @param asistencia  totales del periodo.
 */
public record FilaConsolidado(Alumno alumno, Map<Integer, Calificativo> porCurso, ResumenAsistencia asistencia) {
}
