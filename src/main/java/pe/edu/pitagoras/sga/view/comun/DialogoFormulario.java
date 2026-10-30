package pe.edu.pitagoras.sga.view.comun;

import com.formdev.flatlaf.FlatClientProperties;
import java.awt.Component;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import net.miginfocom.swing.MigLayout;

/**
 * Formulario modal reutilizable: etiqueta y campo por fila, y botones Guardar/Cancelar. Si el
 * guardado falla (validación o BD) el diálogo sigue abierto con los datos, para corregirlos.
 */
public class DialogoFormulario extends JDialog {

    private final JPanel campos = new JPanel(new MigLayout("insets 0, wrap 2, fillx", "[right][300!]", ""));
    private final JButton btnGuardar = Tema.botonPrimario("Guardar");

    public DialogoFormulario(Component padre, String titulo, String descripcion) {
        super(SwingUtilities.getWindowAncestor(padre), titulo, ModalityType.APPLICATION_MODAL);
        JPanel p = new JPanel(new MigLayout("insets 24, wrap, fillx", "[grow]"));
        p.setBackground(Tema.BLANCO);
        campos.setOpaque(false);
        p.add(Tema.seccion(titulo));
        if (descripcion != null) {
            p.add(Tema.subtitulo(descripcion), "gapbottom 10");
        }
        p.add(campos, "growx");
        JButton cancelar = Tema.botonSecundario("Cancelar");
        cancelar.addActionListener(e -> dispose());
        p.add(cancelar, "split 2, right, gaptop 16, w 120!");
        p.add(btnGuardar, "w 140!");
        setContentPane(p);
        getRootPane().setDefaultButton(btnGuardar);
    }

    /** Agrega una fila con etiqueta y campo. */
    public DialogoFormulario campo(String etiqueta, JComponent componente) {
        campos.add(Tema.etiqueta(etiqueta));
        if (componente instanceof JTextField t) {
            t.putClientProperty(FlatClientProperties.STYLE, "arc:6");
        }
        campos.add(componente, "growx, h 36!");
        return this;
    }

    /** Agrega una fila con un componente de alto libre (casillas, listas). */
    public DialogoFormulario campoLibre(String etiqueta, JComponent componente) {
        campos.add(Tema.etiqueta(etiqueta), "top, gaptop 4");
        campos.add(componente, "growx");
        return this;
    }

    /**
     * Muestra el diálogo. Al pulsar Guardar ejecuta la acción en segundo plano; si termina bien
     * cierra el diálogo y llama a alTerminar.
     */
    public <T> void mostrar(Callable<T> guardar, Consumer<T> alTerminar) {
        btnGuardar.addActionListener(e -> {
            btnGuardar.setEnabled(false);
            Tarea.ejecutar(this, guardar, r -> {
                dispose();
                alTerminar.accept(r);
            }, () -> btnGuardar.setEnabled(true));
        });
        pack();
        setResizable(false);
        setLocationRelativeTo(getOwner());
        setVisible(true);
    }
}
