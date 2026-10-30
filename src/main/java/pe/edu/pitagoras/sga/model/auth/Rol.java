package pe.edu.pitagoras.sga.model.auth;

import java.util.Locale;

/** Perfiles del sistema; coinciden con la tabla Rol de la base de datos. */
public enum Rol {
    DIRECTOR, DOCENTE, APODERADO;

    /**
     * @param nombre texto guardado en la BD, por ejemplo "DOCENTE".
     * @return el rol correspondiente.
     * @throws IllegalArgumentException si el nombre no existe.
     */
    public static Rol desdeNombre(String nombre) {
        return Rol.valueOf(nombre.trim().toUpperCase(Locale.ROOT));
    }
}