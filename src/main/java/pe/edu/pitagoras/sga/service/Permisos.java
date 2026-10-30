package pe.edu.pitagoras.sga.service;

import java.util.Arrays;
import pe.edu.pitagoras.sga.exception.AccesoDenegadoException;
import pe.edu.pitagoras.sga.model.auth.Rol;
import pe.edu.pitagoras.sga.model.auth.Sesion;

/**
 * Control de acceso por perfil (RNF03). Cada service lo llama al inicio de sus métodos:
 * ocultar un botón en la vista no basta.
 */
public final class Permisos {

    private Permisos() {
    }

    /**
     * @throws AccesoDenegadoException si no hay sesión o su rol no está entre los permitidos.
     */
    public static void exigir(Sesion sesion, Rol... permitidos) {
        if (sesion == null || Arrays.stream(permitidos).noneMatch(sesion::tieneRol)) {
            throw new AccesoDenegadoException("No tiene permiso para realizar esta operación.");
        }
    }
}
