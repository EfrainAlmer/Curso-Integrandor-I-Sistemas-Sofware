package pe.edu.pitagoras.sga.model.evaluacion;

import java.util.List;
import java.util.Map;
import pe.edu.pitagoras.sga.model.academico.Alumno;
import pe.edu.pitagoras.sga.model.gestion.DatosColegio;

/**
 * Libreta de notas del año (RF17), con la estructura del Informe de progreso del aprendizaje del
 * MINEDU: competencias por bimestre, nivel final, conclusiones descriptivas, asistencia y situación final.
 *
 * @param colegio            datos de la institución.
 * @param alumno             alumno.
 * @param anio               año escolar.
 * @param lineas             una por competencia, agrupadas por área.
 * @param areas              calificativo de cada área por bimestre y anual.
 * @param conclusiones       conclusiones descriptivas registradas.
 * @param asistenciaBimestre asistencia de cada bimestre (clave 1 a 4).
 * @param asistencia         asistencia total del año.
 * @param situacionFinal     PRO, RR, PER o "en proceso".
 */
public record Libreta(DatosColegio colegio, Alumno alumno, int anio, List<LineaLibreta> lineas, List<ResumenArea> areas,
                      List<ConclusionLibreta> conclusiones, Map<Integer, ResumenAsistencia> asistenciaBimestre,
                      ResumenAsistencia asistencia, String situacionFinal) {
}
