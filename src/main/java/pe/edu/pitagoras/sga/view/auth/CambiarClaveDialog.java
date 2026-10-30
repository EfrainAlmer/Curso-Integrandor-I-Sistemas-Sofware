package pe.edu.pitagoras.sga.view.auth;

import com.formdev.flatlaf.FlatClientProperties;
import java.awt.Window;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import net.miginfocom.swing.MigLayout;
import pe.edu.pitagoras.sga.controller.AuthController;
import pe.edu.pitagoras.sga.model.auth.Sesion;
import pe.edu.pitagoras.sga.view.comun.Mensajes;
import pe.edu.pitagoras.sga.view.comun.Tarea;
import pe.edu.pitagoras.sga.view.comun.Tema;

/** Cambio de contraseña (RF02): obligatorio en el primer ingreso y disponible desde el menú. */
public class CambiarClaveDialog extends JDialog {

    private final JPasswordField txtActual = campo("Contraseña actual o temporal");
    private final JPasswordField txtNueva = campo("Nueva contraseña");
    private final JPasswordField txtRepetir = campo("Repita la nueva contraseña");
    private boolean cambiada;

    /**
     * @param obligatorio true en el primer ingreso: el texto lo indica y no se puede omitir.
     */
    public CambiarClaveDialog(Window padre, AuthController controller, Sesion sesion, boolean obligatorio) {
        super(padre, "Cambiar contraseña", ModalityType.APPLICATION_MODAL);
        JPanel p = new JPanel(new MigLayout("insets 24, wrap, fillx", "[340!]"));
        p.setBackground(Tema.BLANCO);
        p.add(Tema.seccion(obligatorio ? "Cree su contraseña personal" : "Cambiar contraseña"));
        p.add(Tema.subtitulo("Usuario: " + sesion.getUsername()), "gapbottom 12");
        p.add(txtActual, "growx, h 40!");
        p.add(txtNueva, "growx, h 40!");
        p.add(txtRepetir, "growx, h 40!");
        JLabel reglas = Tema.subtitulo("<html>Mínimo 8 caracteres, con letras y números.<br>"
                + "Debe ser distinta de la actual.</html>");
        p.add(reglas, "gapbottom 12");
        JButton guardar = Tema.botonPrimario("Guardar contraseña");
        JButton cancelar = Tema.botonSecundario(obligatorio ? "Salir" : "Cancelar");
        p.add(guardar, "split 2, growx, h 40!");
        p.add(cancelar, "growx, h 40!");
        setContentPane(p);
        getRootPane().setDefaultButton(guardar);

        cancelar.addActionListener(e -> dispose());
        guardar.addActionListener(e -> {
            char[] nueva = txtNueva.getPassword();
            char[] repetir = txtRepetir.getPassword();
            if (!java.util.Arrays.equals(nueva, repetir)) {
                java.util.Arrays.fill(repetir, '\0');
                Mensajes.aviso(this, "Las contraseñas nuevas no coinciden.");
                return;
            }
            java.util.Arrays.fill(repetir, '\0');
            char[] actual = txtActual.getPassword();
            guardar.setEnabled(false);
            Tarea.ejecutar(this, () -> {
                controller.cambiarPassword(sesion, actual, nueva);
                return Boolean.TRUE;
            }, ok -> {
                cambiada = true;
                Mensajes.info(this, "Contraseña actualizada correctamente.");
                dispose();
            }, () -> guardar.setEnabled(true));
        });
        pack();
        setResizable(false);
        setLocationRelativeTo(padre);
    }

    public boolean isCambiada() {
        return cambiada;
    }

    private static JPasswordField campo(String placeholder) {
        JPasswordField f = new JPasswordField();
        f.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, placeholder);
        f.putClientProperty(FlatClientProperties.STYLE, "showRevealButton:true");
        return f;
    }
}
