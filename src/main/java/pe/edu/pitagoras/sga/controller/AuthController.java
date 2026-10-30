package pe.edu.pitagoras.sga.controller;

import com.google.common.base.Preconditions;
import java.util.Arrays;
import pe.edu.pitagoras.sga.model.auth.ResultadoLogin;
import pe.edu.pitagoras.sga.model.auth.Sesion;
import pe.edu.pitagoras.sga.service.AuthService;

/**
 * Único punto de contacto de las pantallas de acceso. La vista no conoce el service ni el DAO.
 * <p>
 * Contrato para la vista: los métodos lanzan, sin necesidad de try por tipo,
 * {@code AuthException} (credenciales), {@code CuentaBloqueadaException} (trae la hora
 * hasta la que debe esperar), {@code BusinessException} (clave nueva inválida) y
 * {@code AccesoDatosException} (la base de datos no responde). La vista muestra
 * {@code getMessage()} en un diálogo. Las contraseñas llegan como char[] y aquí se borran
 * de memoria al terminar.
 */
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = Preconditions.checkNotNull(authService, "authService");
    }

    /** @return la sesión y si debe cambiar su clave (primer ingreso). */
    public ResultadoLogin iniciarSesion(String username, char[] clave) {
        try {
            return authService.iniciarSesion(username, clave);
        } finally {
            borrar(clave);
        }
    }

    /** Cambia la clave de la sesión indicada (RF02). */
    public void cambiarPassword(Sesion sesion, char[] actual, char[] nueva) {
        try {
            authService.cambiarPassword(sesion, actual, nueva);
        } finally {
            borrar(actual);
            borrar(nueva);
        }
    }

    private static void borrar(char[] clave) {
        if (clave != null) {
            Arrays.fill(clave, '\0');
        }
    }
}