package org.pe.edu.cole.app;

import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.FlatLightLaf;
import com.formdev.flatlaf.fonts.roboto.FlatRobotoFont;
import org.pe.edu.cole.app.controller.LoginController;
import org.pe.edu.cole.app.view.LoginView;

import javax.swing.*;
import java.awt.*;

/**
 * Esta clase es el punto de entrada principal del sistema.
 * Su única responsabilidad es configurar el entorno gráfico (Look and Feel)
 * y arrancar la arquitectura MVC, delegando la lógica y el diseño a sus respectivas clases.
 */
public class Main {

    /**
     * Método de ejecución de la aplicación.
     * Instala las tipografías, registra el tema visual y lanza la vista
     * de inicio de sesión de manera segura a través del hilo de eventos de Swing.
     *
     * @param args los argumentos de la línea de comandos (no se utilizan en este proyecto).
     */
    public static void main(String[] args) {
        FlatRobotoFont.install();
        FlatLaf.registerCustomDefaultsSource("app.themes");
        FlatLightLaf.setup();
        UIManager.put("defaultFont", new Font(FlatRobotoFont.FAMILY, Font.PLAIN, 20));

        EventQueue.invokeLater(() -> {
            LoginView vista = new LoginView();
            new LoginController(vista); // conecta vista con lógica de autenticación
            vista.setVisible(true);
        });
    }//cierra método main
} //cierre de Main