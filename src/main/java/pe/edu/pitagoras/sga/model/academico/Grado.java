package pe.edu.pitagoras.sga.model.academico;

/**
 * Grado del nivel secundario (1.° a 5.°).
 *
 * @param gradoId identificador.
 * @param nombre  texto mostrado, por ejemplo "1.°".
 */
public record Grado(int gradoId, String nombre) {
    @Override
    public String toString() {
        return nombre;
    }
}
