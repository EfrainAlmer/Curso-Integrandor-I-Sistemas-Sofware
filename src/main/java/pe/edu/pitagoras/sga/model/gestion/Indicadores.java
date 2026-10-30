package pe.edu.pitagoras.sga.model.gestion;

import java.util.List;
import java.util.Map;
import pe.edu.pitagoras.sga.model.academico.Periodo;
import pe.edu.pitagoras.sga.model.evaluacion.Calificativo;

/**
 * Datos del panel de la Dirección (RF08).
 *
 * @param periodo               periodo consultado.
 * @param totalAlumnos          alumnos activos.
 * @param totalSecciones        secciones con alumnos.
 * @param porcentajeAsistencia  asistencia del periodo (0 a 100).
 * @param distribucionPorGrado  grado → cantidad de áreas en AD, A, B y C.
 * @param distribucionPorSeccion sección ("1.° A") → cantidad de áreas en AD, A, B y C.
 * @param distribucionPorArea   área curricular → cantidad de alumnos en AD, A, B y C.
 * @param avanceDocentes        avance del registro de notas por docente.
 * @param alertas               alumnos con C en alguna área.
 */
public record Indicadores(Periodo periodo, int totalAlumnos, int totalSecciones, double porcentajeAsistencia,
                          Map<String, Map<Calificativo, Integer>> distribucionPorGrado,
                          Map<String, Map<Calificativo, Integer>> distribucionPorSeccion,
                          Map<String, Map<Calificativo, Integer>> distribucionPorArea,
                          List<AvanceDocente> avanceDocentes, List<AlertaAlumno> alertas) {

    /** @return porcentaje global del registro de notas. */
    public double avanceGlobal() {
        int reg = 0;
        int esp = 0;
        for (AvanceDocente a : avanceDocentes) {
            reg += a.registradas();
            esp += a.esperados();
        }
        return esp == 0 ? 100.0 : reg * 100.0 / esp;
    }
}
