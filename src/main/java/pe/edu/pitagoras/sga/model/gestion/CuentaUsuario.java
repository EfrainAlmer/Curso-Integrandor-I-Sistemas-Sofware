package pe.edu.pitagoras.sga.model.gestion;

import java.time.LocalDateTime;
import pe.edu.pitagoras.sga.model.auth.Rol;

/**
 * Fila de la lista de usuarios del Director (RF03). Nunca incluye el hash.
 *
 * @param usuarioId           identificador.
 * @param username            nombre de usuario.
 * @param rol                 perfil.
 * @param nombre              nombre de la persona (docente o alumno del apoderado).
 * @param activo              false si fue dado de baja.
 * @param debeCambiarPassword true si aún usa una clave temporal.
 * @param bloqueadoHasta      fin del bloqueo por intentos (null si no está bloqueada).
 */
public record CuentaUsuario(int usuarioId, String username, Rol rol, String nombre, boolean activo,
                            boolean debeCambiarPassword, LocalDateTime bloqueadoHasta) {
}
