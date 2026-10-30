package pe.edu.pitagoras.sga.model.academico;

/**
 * Docente y su cuenta de acceso.
 *
 * @param docenteId identificador.
 * @param usuarioId cuenta vinculada.
 * @param dni       8 dígitos.
 * @param nombre    nombres.
 * @param apellido  apellidos.
 * @param email     correo (puede ser null).
 */
public record Docente(int docenteId, int usuarioId, String dni, String nombre, String apellido, String email) {

    /** @return "Apellidos, Nombres". */
    public String nombreCompleto() {
        return apellido + ", " + nombre;
    }

    @Override
    public String toString() {
        return nombreCompleto();
    }
}
