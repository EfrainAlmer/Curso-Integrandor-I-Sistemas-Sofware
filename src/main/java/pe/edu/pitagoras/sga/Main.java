package pe.edu.pitagoras.sga;

import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.FlatLightLaf;
import com.formdev.flatlaf.fonts.roboto.FlatRobotoFont;
import java.awt.EventQueue;
import java.awt.Font;
import javax.swing.JOptionPane;
import javax.swing.UIManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pe.edu.pitagoras.sga.config.AppConfig;
import pe.edu.pitagoras.sga.config.Aplicacion;
import pe.edu.pitagoras.sga.config.ConexionBD;
import pe.edu.pitagoras.sga.view.auth.LoginFrame;

/**
 * Punto de entrada: configura el aspecto (FlatLaf), conecta con SQL Server, arma las capas
 * DAO → Service → Controller en {@link Aplicacion} y abre la ventana de inicio de sesión.
 */
public final class Main {

    private static final Logger LOG = LoggerFactory.getLogger(Main.class);

    private Main() {
    }

    public static void main(String[] args) {
        // Apache POI usa Log4j2; sin esta propiedad imprime un aviso inofensivo en la consola.
        System.setProperty("log4j2.statusLoggerLevel", "OFF");
        FlatRobotoFont.install();
        FlatLaf.registerCustomDefaultsSource("app.themes");
        FlatLightLaf.setup();
        UIManager.put("defaultFont", new Font(FlatRobotoFont.FAMILY, Font.PLAIN, 14));

        Aplicacion app;
        try {
            AppConfig config = AppConfig.cargar();
            ConexionBD.iniciar(config);
            app = new Aplicacion(config);
        } catch (RuntimeException e) {
            LOG.error("No se pudo iniciar la aplicación", e);
            JOptionPane.showMessageDialog(null, e.getMessage()
                    + "\n\nRevise que SQL Server esté encendido y el archivo config.properties.",
                    "No se pudo iniciar", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
            return;
        }
        Runtime.getRuntime().addShutdownHook(new Thread(ConexionBD::cerrar, "cierre-pool"));
        LOG.info("Aplicación iniciada");
        Aplicacion lista = app;
        EventQueue.invokeLater(() -> new LoginFrame(lista).setVisible(true));
    }
}
