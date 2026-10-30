package pe.edu.pitagoras.sga.exception;

/** Regla de negocio incumplida. No verificada, para no ensuciar las firmas de las capas. */
public class BusinessException extends RuntimeException {
    public BusinessException(String mensaje) {
        super(mensaje);
    }
}