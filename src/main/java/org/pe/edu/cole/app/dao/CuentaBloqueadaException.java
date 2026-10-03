package org.pe.edu.cole.app.dao;

import java.sql.Timestamp;

/**
 * Se lanza cuando una cuenta está bloqueada temporalmente por haber superado
 * el número máximo de intentos fallidos de inicio de sesión (RF02).
 * Es una excepción aparte de "credenciales incorrectas" para que el
 * controlador pueda mostrar un mensaje distinto y más útil al usuario.
 */
public class CuentaBloqueadaException extends Exception {

    private final Timestamp bloqueadoHasta;

    /**
     * Este constructor crea la excepción indicando hasta cuándo dura el bloqueo.
     * @param bloqueadoHasta la fecha y hora hasta la cual la cuenta sigue bloqueada.
     */
    public CuentaBloqueadaException(Timestamp bloqueadoHasta) {
        super("Cuenta bloqueada temporalmente por intentos fallidos.");
        this.bloqueadoHasta = bloqueadoHasta;
    }

    /**
     * Este método obtiene hasta cuándo dura el bloqueo.
     * @return la fecha y hora hasta la cual no se debe permitir un nuevo intento.
     */
    public Timestamp getBloqueadoHasta() {
        return bloqueadoHasta;
    }
}//cierra class CuentaBloqueadaException
