package pe.edu.pitagoras.sga.view.comun;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JPanel;

/**
 * Base de todas las pantallas del menú: cabecera con título, subtítulo y acciones a la derecha,
 * y un área de contenido. {@link #alMostrar()} se llama cada vez que el usuario abre el módulo,
 * así los datos siempre están al día.
 */
public abstract class PanelModulo extends JPanel {

    private final JPanel acciones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
    protected final JPanel contenido = new JPanel(new BorderLayout(0, 16));
    private final javax.swing.JLabel lblTitulo;
    private final javax.swing.JLabel lblSubtitulo;
    private boolean cargado;

    protected PanelModulo(String titulo, String subtitulo) {
        super(new BorderLayout(0, 18));
        setBackground(Tema.FONDO);
        setBorder(BorderFactory.createEmptyBorder(24, 28, 24, 28));

        JPanel textos = new JPanel();
        textos.setOpaque(false);
        textos.setLayout(new BoxLayout(textos, BoxLayout.Y_AXIS));
        lblTitulo = Tema.titulo(titulo);
        lblSubtitulo = Tema.subtitulo(subtitulo);
        textos.add(lblTitulo);
        textos.add(Box.createVerticalStrut(4));
        textos.add(lblSubtitulo);

        acciones.setOpaque(false);
        JPanel cabecera = new JPanel(new BorderLayout());
        cabecera.setOpaque(false);
        cabecera.add(textos, BorderLayout.WEST);
        cabecera.add(acciones, BorderLayout.EAST);

        contenido.setOpaque(false);
        add(cabecera, BorderLayout.NORTH);
        add(contenido, BorderLayout.CENTER);
    }

    /** Cambia el título y el subtítulo (por ejemplo, para mostrar el nombre del alumno). */
    protected void setCabecera(String titulo, String subtitulo) {
        lblTitulo.setText(titulo);
        lblSubtitulo.setText(subtitulo);
    }

    /** Agrega un botón o control a la derecha de la cabecera. */
    protected void accion(JComponent componente) {
        acciones.add(componente);
    }

    /** Llamado por la ventana principal al mostrar el módulo. */
    public final void mostrar() {
        alMostrar(!cargado);
        cargado = true;
    }

    /**
     * @param primeraVez true la primera vez que se abre (cargar combos); luego solo refrescar.
     */
    protected abstract void alMostrar(boolean primeraVez);

    /** @return true si el módulo tiene cambios sin guardar (la ventana pregunta antes de salir). */
    public boolean tieneCambiosSinGuardar() {
        return false;
    }
}
