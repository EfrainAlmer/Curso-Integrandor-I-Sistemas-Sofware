package pe.edu.pitagoras.sga.exception;

import java.time.LocalDateTime;

/**
 * Cuenta bloqueada temporalmente por superar el máximo de intentos fallidos (RF02).
 * Es distinta de "credenciales incorrectas" para que el controlador muestre
 * hasta cuándo debe esperar el usuario.
 */
public class CuentaBloqueadaException extends AuthException {

    private final LocalDateTime bloqueadoHasta;

    /** @param bloqueadoHasta fecha y hora hasta la cual la cuenta sigue bloqueada. */
    public CuentaBloqueadaException(LocalDateTime bloqueadoHasta) {
        super("Cuenta bloqueada temporalmente por intentos fallidos.");
        this.bloqueadoHasta = bloqueadoHasta;
    }

    public LocalDateTime getBloqueadoHasta() {
        return bloqueadoHasta;
    }
}