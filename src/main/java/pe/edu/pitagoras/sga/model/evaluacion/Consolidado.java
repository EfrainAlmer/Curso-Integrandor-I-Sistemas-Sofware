package pe.edu.pitagoras.sga.model.evaluacion;

import java.util.List;
import pe.edu.pitagoras.sga.model.academico.Curso;
import pe.edu.pitagoras.sga.model.academico.Periodo;
import pe.edu.pitagoras.sga.model.academico.Seccion;

/**
 * Consolidado bimestral de una sección (RF19).
 *
 * @param seccion sección.
 * @param periodo bimestre.
 * @param cursos  áreas, en el orden de las columnas.
 * @param filas   una por alumno.
 */
public record Consolidado(Seccion seccion, Periodo periodo, List<Curso> cursos, List<FilaConsolidado> filas) {
}
