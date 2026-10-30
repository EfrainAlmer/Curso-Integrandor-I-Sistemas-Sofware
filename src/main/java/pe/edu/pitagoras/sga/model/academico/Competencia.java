package pe.edu.pitagoras.sga.model.academico;

/**
 * Competencia del Currículo Nacional que se califica dentro de un área.
 *
 * @param cursoId área a la que pertenece.
 * @param codigo  '1' a '4'.
 * @param nombre  descripción de la competencia.
 */
public record Competencia(int cursoId, String codigo, String nombre) {
}
