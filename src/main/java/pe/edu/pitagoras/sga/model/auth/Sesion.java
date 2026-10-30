package pe.edu.pitagoras.sga.model.auth;

import com.google.common.base.Preconditions;

/**
 * Quién está usando el sistema. Cada service la recibe para validar permisos
 * (RNF03); ocultar un botón en la vista no basta.
 */
public final class Sesion {

    private final int usuarioId;
    private final String username;
    private final Rol rol;
    private final Integer alumnoId; // solo para el apoderado

    /**
     * @param alumnoId id del alumno vinculado; obligatorio si el rol es APODERADO, null en los demás.
     */
    public Sesion(int usuarioId, String username, Rol rol, Integer alumnoId) {
        Preconditions.checkNotNull(username, "username");
        Preconditions.checkNotNull(rol, "rol");
        Preconditions.checkArgument(rol != Rol.APODERADO || alumnoId != null,
                "Un apoderado debe tener alumnoId");
        this.usuarioId = usuarioId;
        this.username = username;
        this.rol = rol;
        this.alumnoId = alumnoId;
    }

    public boolean tieneRol(Rol esperado) { return rol == esperado; }

    public int getUsuarioId() { return usuarioId; }
    public String getUsername() { return username; }
    public Rol getRol() { return rol; }
    /** @return el alumno del apoderado, o null si no es apoderado. */
    public Integer getAlumnoId() { return alumnoId; }
}