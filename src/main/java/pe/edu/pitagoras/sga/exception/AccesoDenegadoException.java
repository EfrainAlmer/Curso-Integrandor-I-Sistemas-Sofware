package pe.edu.pitagoras.sga.exception;

/** El rol o el alumno de la sesión no tiene permiso para la operación (RNF03). */
public class AccesoDenegadoException extends BusinessException {
    public AccesoDenegadoException(String mensaje) {
        super(mensaje);
    }
}