package pe.edu.pitagoras.sga.model.academico;

/**
 * Estudiante inscrito y los datos de su apoderado (RF04).
 *
 * @param alumnoId          identificador (0 si aún no se guarda).
 * @param seccion           sección donde está inscrito.
 * @param dni               8 dígitos.
 * @param nombre            nombres.
 * @param apellido          apellidos.
 * @param nombreApoderado   nombre completo del apoderado.
 * @param telefonoApoderado teléfono de contacto (puede ser null).
 * @param estado            "ACTIVO" o "RETIRADO".
 * @param usuarioApoderado  username de la cuenta del apoderado (null si no tiene).
 */
public record Alumno(int alumnoId, Seccion seccion, String dni, String nombre, String apellido,
                     String nombreApoderado, String telefonoApoderado, String estado, String usuarioApoderado) {

    public static final String ACTIVO = "ACTIVO";
    public static final String RETIRADO = "RETIRADO";

    /** @return "Apellidos, Nombres", como en las listas oficiales. */
    public String nombreCompleto() {
        return apellido + ", " + nombre;
    }

    @Override
    public String toString() {
        return nombreCompleto();
    }

    public boolean isActivo() {
        return ACTIVO.equals(estado);
    }
}
