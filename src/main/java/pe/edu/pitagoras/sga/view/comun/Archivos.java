package pe.edu.pitagoras.sga.view.comun;

import java.awt.Component;
import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.Callable;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;

/** Guardar reportes: elegir el archivo destino y ofrecer abrirlo al terminar. */
public final class Archivos {

    private Archivos() {
    }

    /**
     * Pide dónde guardar, genera el archivo en segundo plano y ofrece abrirlo.
     *
     * @param nombreSugerido nombre por defecto, con extensión (.pdf o .xlsx).
     * @param generar        recibe el archivo elegido y lo escribe.
     */
    public static void guardar(Component padre, String nombreSugerido, java.util.function.Function<File, File> generar) {
        String ext = nombreSugerido.substring(nombreSugerido.lastIndexOf('.') + 1);
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Guardar reporte");
        fc.setSelectedFile(new File(fc.getFileSystemView().getDefaultDirectory(), limpiar(nombreSugerido)));
        fc.setFileFilter(new FileNameExtensionFilter(ext.toUpperCase() + " (*." + ext + ")", ext));
        if (fc.showSaveDialog(padre) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        File elegido = fc.getSelectedFile();
        File destino = elegido.getName().toLowerCase().endsWith("." + ext) ? elegido
                : new File(elegido.getParentFile(), elegido.getName() + "." + ext);
        if (destino.exists() && !Mensajes.confirmar(padre, "El archivo ya existe. ¿Desea reemplazarlo?")) {
            return;
        }
        Callable<File> trabajo = () -> generar.apply(destino);
        Tarea.ejecutar(padre, trabajo, archivo -> {
            if (!Desktop.isDesktopSupported()) {
                Mensajes.info(padre, "Se guardó " + archivo.getAbsolutePath());
                return;
            }
            // RF18: vista previa (abrir) o impresión directa del reporte.
            Object[] opciones = archivo.getName().endsWith(".pdf")
                    ? new Object[]{"Abrir (vista previa)", "Imprimir", "Cerrar"} : new Object[]{"Abrir", "Cerrar"};
            int r = javax.swing.JOptionPane.showOptionDialog(padre, "Se guardó " + archivo.getName() + ".",
                    "Reporte generado", javax.swing.JOptionPane.DEFAULT_OPTION,
                    javax.swing.JOptionPane.INFORMATION_MESSAGE, null, opciones, opciones[0]);
            try {
                if (r == 0) {
                    Desktop.getDesktop().open(archivo);
                } else if (r == 1 && opciones.length == 3) {
                    Desktop.getDesktop().print(archivo);
                }
            } catch (IOException | UnsupportedOperationException e) {
                Mensajes.aviso(padre, "No se pudo abrir o imprimir automáticamente: " + archivo.getAbsolutePath());
            }
        });
    }

    /** Quita caracteres no válidos en nombres de archivo de Windows. */
    private static String limpiar(String nombre) {
        return nombre.replaceAll("[\\\\/:*?\"<>|°]", "").replace(' ', '_');
    }
}
