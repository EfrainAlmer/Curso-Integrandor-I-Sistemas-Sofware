package pe.edu.pitagoras.sga.model.gestion;

import java.util.List;

/**
 * Alumno con calificativo C en una o más áreas del periodo (RF08).
 *
 * @param alumnoId identificador.
 * @param alumno   "Apellidos, Nombres".
 * @param seccion  "3.° B".
 * @param areas    áreas en C.
 */
public record AlertaAlumno(int alumnoId, String alumno, String seccion, List<String> areas) {
}
