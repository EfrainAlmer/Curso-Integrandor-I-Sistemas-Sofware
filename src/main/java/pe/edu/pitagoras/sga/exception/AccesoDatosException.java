package pe.edu.pitagoras.sga.exception;

/**
 * Error de acceso a la base de datos (servidor apagado, credenciales de SQL
 * incorrectas, procedimiento inexistente, etc.). Es no verificada para no
 * contaminar las firmas de las capas superiores; el controlador la captura
 * y muestra un mensaje claro en lugar de decir "contraseña incorrecta".
 */
public class AccesoDatosException extends RuntimeException {

    /**
     * @param mensaje descripción del problema para la bitácora.
     * @param causa   excepción original de JDBC.
     */
    public AccesoDatosException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}