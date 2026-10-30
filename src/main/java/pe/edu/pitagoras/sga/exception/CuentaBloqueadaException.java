package pe.edu.pitagoras.sga.exception;

import java.sql.Timestamp;

/**
 * Se lanza cuando la cuenta está bloqueada temporalmente por superar el máximo
 * de intentos fallidos de inicio de sesión (RF02). Es distinta de "credenciales
 * incorrectas" para que el controlador muestre un mensaje más útil.
 */
public class CuentaBloqueadaException extends Exception {

    private final Timestamp bloqueadoHasta;

    /**
     * @param bloqueadoHasta fecha y hora hasta la cual la cuenta sigue bloqueada.
     */
    public CuentaBloqueadaException(Timestamp bloqueadoHasta) {
        super("Cuenta bloqueada temporalmente por intentos fallidos.");
        this.bloqueadoHasta = bloqueadoHasta;
    }

    /**
     * @return fecha y hora hasta la cual no se permite un nuevo intento.
     */
    public Timestamp getBloqueadoHasta() {
        return bloqueadoHasta;
    }
}
