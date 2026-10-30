package pe.edu.pitagoras.sga.view.comun;

import java.awt.Component;
import java.awt.Cursor;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

/**
 * Ejecuta consultas a la base de datos fuera del hilo de la interfaz (SwingWorker) para que
 * la ventana no se congele, y vuelve al hilo de Swing para mostrar el resultado o el error.
 */
public final class Tarea {

    private Tarea() {
    }

    /**
     * @param padre     componente sobre el que se muestra el cursor de espera y los errores.
     * @param trabajo   lo que se ejecuta en segundo plano (llamadas al controlador).
     * @param alTerminar qué hacer con el resultado, ya en el hilo de Swing.
     */
    public static <T> void ejecutar(Component padre, Callable<T> trabajo, Consumer<T> alTerminar) {
        ejecutar(padre, trabajo, alTerminar, null);
    }

    /**
     * @param siempre se ejecuta al final haya error o no (por ejemplo, reactivar un botón).
     */
    public static <T> void ejecutar(Component padre, Callable<T> trabajo, Consumer<T> alTerminar, Runnable siempre) {
        Component raiz = padre == null ? null : SwingUtilities.getRoot(padre);
        if (raiz != null) {
            raiz.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        }
        new SwingWorker<T, Void>() {
            @Override
            protected T doInBackground() throws Exception {
                return trabajo.call();
            }

            @Override
            protected void done() {
                if (raiz != null) {
                    raiz.setCursor(Cursor.getDefaultCursor());
                }
                if (siempre != null) {
                    siempre.run();
                }
                try {
                    T resultado = get();
                    if (alTerminar != null) {
                        alTerminar.accept(resultado);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (Exception e) {
                    Mensajes.error(padre, e);
                }
            }
        }.execute();
    }

    /** Variante para acciones sin resultado. */
    public static void ejecutarAccion(Component padre, Runnable trabajo, Runnable alTerminar) {
        ejecutar(padre, () -> {
            trabajo.run();
            return Boolean.TRUE;
        }, r -> {
            if (alTerminar != null) {
                alTerminar.run();
            }
        });
    }
}
