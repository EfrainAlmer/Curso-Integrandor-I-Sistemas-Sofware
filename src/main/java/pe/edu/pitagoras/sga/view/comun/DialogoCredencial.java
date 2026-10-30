package pe.edu.pitagoras.sga.view.comun;

import java.awt.Component;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import javax.swing.JOptionPane;
import javax.swing.JTextArea;
import pe.edu.pitagoras.sga.model.gestion.Credencial;

/** Muestra una sola vez el usuario y la clave temporal para entregarlos en persona. */
public final class DialogoCredencial {

    private DialogoCredencial() {
    }

    public static void mostrar(Component padre, String titulo, Credencial c) {
        JTextArea texto = new JTextArea("Usuario:  " + c.username() + "\nClave temporal:  " + c.claveTemporal()
                + "\n\nEntréguela en persona. Se pedirá cambiarla en el primer ingreso.\n"
                + "Esta clave no se vuelve a mostrar: en la base solo queda su hash BCrypt.");
        texto.setEditable(false);
        texto.setOpaque(false);
        texto.setFont(texto.getFont().deriveFont(14f));
        Object[] opciones = {"Copiar y cerrar", "Cerrar"};
        int r = JOptionPane.showOptionDialog(padre, texto, titulo, JOptionPane.DEFAULT_OPTION,
                JOptionPane.INFORMATION_MESSAGE, null, opciones, opciones[0]);
        if (r == 0) {
            Toolkit.getDefaultToolkit().getSystemClipboard()
                    .setContents(new StringSelection(c.username() + " / " + c.claveTemporal()), null);
        }
    }
}
