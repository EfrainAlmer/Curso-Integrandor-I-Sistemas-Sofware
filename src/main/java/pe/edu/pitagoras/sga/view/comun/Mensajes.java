package pe.edu.pitagoras.sga.view.comun;

import java.awt.Component;
import javax.swing.JOptionPane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pe.edu.pitagoras.sga.exception.AccesoDatosException;
import pe.edu.pitagoras.sga.exception.AccesoDenegadoException;
import pe.edu.pitagoras.sga.exception.BusinessException;
import pe.edu.pitagoras.sga.exception.CuentaBloqueadaException;

/**
 * Diálogos de la aplicación. Traduce las excepciones a mensajes claros: las reglas de negocio
 * muestran su texto; los errores técnicos se registran en el log y el usuario ve un aviso
 * general (nunca la traza ni datos sensibles).
 */
public final class Mensajes {

    private static final Logger LOG = LoggerFactory.getLogger(Mensajes.class);

    private Mensajes() {
    }

    public static void info(Component padre, String texto) {
        JOptionPane.showMessageDialog(padre, texto, "Información", JOptionPane.INFORMATION_MESSAGE);
    }

    public static void aviso(Component padre, String texto) {
        JOptionPane.showMessageDialog(padre, texto, "Atención", JOptionPane.WARNING_MESSAGE);
    }

    public static boolean confirmar(Component padre, String texto) {
        return JOptionPane.showConfirmDialog(padre, texto, "Confirmar", JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION;
    }

    /** Muestra el error adecuado según su tipo. */
    public static void error(Component padre, Throwable e) {
        Throwable causa = e;
        while ((causa instanceof java.util.concurrent.ExecutionException
                || causa instanceof java.lang.reflect.InvocationTargetException) && causa.getCause() != null) {
            causa = causa.getCause();
        }
        if (causa instanceof CuentaBloqueadaException b) {
            JOptionPane.showMessageDialog(padre, b.getMessage() + "\nPodrá intentarlo nuevamente después de las "
                    + b.getBloqueadoHasta().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm")) + ".",
                    "Cuenta bloqueada", JOptionPane.WARNING_MESSAGE);
        } else if (causa instanceof AccesoDenegadoException) {
            LOG.warn("Acceso denegado: {}", causa.getMessage());
            JOptionPane.showMessageDialog(padre, causa.getMessage(), "Acceso denegado", JOptionPane.WARNING_MESSAGE);
        } else if (causa instanceof BusinessException) {
            JOptionPane.showMessageDialog(padre, causa.getMessage(), "Revise los datos", JOptionPane.WARNING_MESSAGE);
        } else if (causa instanceof AccesoDatosException) {
            LOG.error("Error de base de datos", causa);
            JOptionPane.showMessageDialog(padre, causa.getMessage()
                    + "\n\nSi el problema continúa, verifique la conexión con el servidor.",
                    "Base de datos", JOptionPane.ERROR_MESSAGE);
        } else {
            LOG.error("Error inesperado", causa);
            JOptionPane.showMessageDialog(padre, "Ocurrió un error inesperado. El detalle quedó en logs/sga.log.",
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
