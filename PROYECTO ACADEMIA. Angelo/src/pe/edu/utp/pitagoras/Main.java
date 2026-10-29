package pe.edu.utp.pitagoras;

import pe.edu.utp.pitagoras.controller.AuthController;
import pe.edu.utp.pitagoras.view.FrmLogin;

import javax.swing.*;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            AuthController authController = new AuthController();
            FrmLogin login = new FrmLogin(authController);
            login.setVisible(true);
        });
    }
}
