package pe.edu.pitagoras.sga.exception;

/** Fallo de autenticación. El mensaje no revela si falló el usuario o la clave. */
public class AuthException extends BusinessException {
    public AuthException(String mensaje) {
        super(mensaje);
    }
}
