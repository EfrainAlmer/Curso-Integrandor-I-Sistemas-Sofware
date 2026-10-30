package pe.edu.pitagoras.sga.model.academico;

/**
 * Área curricular (Matemática, Comunicación, ...).
 *
 * @param cursoId identificador.
 * @param nombre  nombre del área.
 */
public record Curso(int cursoId, String nombre) {
    @Override
    public String toString() {
        return nombre;
    }
}
