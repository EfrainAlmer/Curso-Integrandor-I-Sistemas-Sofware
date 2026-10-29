package pe.edu.utp.pitagoras.main;

import pe.edu.utp.pitagoras.view.usuario.PnlVistaUsuario;

import javax.swing.*;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                e.printStackTrace();
            }

            JFrame frame = new JFrame("Sistema Académico Pitágoras - Vista Usuario [Omar Bavin]");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(900, 550);
            frame.setLocationRelativeTo(null);

            PnlVistaUsuario panel = new PnlVistaUsuario();
            frame.add(panel);

            frame.setVisible(true);
        });
    }
}
