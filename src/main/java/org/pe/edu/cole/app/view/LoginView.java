package org.pe.edu.cole.app.view;

import com.formdev.flatlaf.FlatClientProperties;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.awt.*;
import java.net.URL;

/**
 * Vista de login en dos paneles (50/50): formulario a la izquierda,
 * imagen/degradado a la derecha. Sin selector de rol: el rol se determina
 * automáticamente en la base de datos (sp_AutenticarUsuario) según las
 * credenciales, no lo elige la persona.
 */
public class LoginView extends JFrame {

    private static final Color ACCENT = new Color(0xF8513E);
    private static final Color ACCENT_LIGHT = new Color(0xFF7A45);
    private static final Color TEXTO_OSCURO = new Color(0x22, 0x27, 0x32);

    /** Imagen opcional del panel derecho: src/main/resources/ejemplo/img/colegio.jpg */
    private static final String RUTA_IMAGEN = "/app/img/alumno.jpg";

    private final JTextField txtUsuario;
    private final JPasswordField txtPass;
    private final JButton btnLogin;

    /**
     * Este constructor construye la ventana de inicio de sesión.
     * Configura dimensiones, diseño responsivo (MigLayout) y todos los componentes visuales.
     */
    public LoginView() {
        setTitle("Gestión Académica");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(new Dimension(1440, 960));
        setLocationRelativeTo(null);
        setLayout(new MigLayout("insets 0, fill"));
        getContentPane().setBackground(Color.WHITE);

        // ---------- Panel izquierdo: formulario ----------
        JPanel izquierdo = new JPanel(new MigLayout("insets 60, al center center, wrap", "[360:460:460]"));
        izquierdo.setBackground(Color.WHITE);

        JLabel lblSistema = new JLabel("Sistema");
        lblSistema.setForeground(TEXTO_OSCURO);
        lblSistema.setFont(lblSistema.getFont().deriveFont(Font.BOLD, 26f));

        JLabel lblColegio = new JLabel("Colegio");
        lblColegio.setForeground(ACCENT);
        lblColegio.setFont(lblColegio.getFont().deriveFont(Font.BOLD, 45f));

        JLabel lblBienvenido = new JLabel("¡Hola, bienvenido!");
        lblBienvenido.setForeground(TEXTO_OSCURO);
        lblBienvenido.setFont(lblBienvenido.getFont().deriveFont(Font.BOLD, 30f));

        JLabel lblSubtitulo = new JLabel(
                "<html>Ingresa tus credenciales para acceder<br>al sistema de gestión académica.</html>");
        lblSubtitulo.setForeground(TEXTO_OSCURO);
        lblSubtitulo.setFont(lblSubtitulo.getFont().deriveFont(16f));

        txtUsuario = new JTextField();
        txtUsuario.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Usuario");

        txtPass = new JPasswordField();
        txtPass.putClientProperty(FlatClientProperties.STYLE, "showRevealButton:true");
        txtPass.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Contraseña");

        JLabel lblNota = new JLabel("Las credenciales se validan según tu usuario");
        lblNota.setForeground(Color.GRAY);
        lblNota.setFont(lblNota.getFont().deriveFont(14f));

        btnLogin = new JButton("Iniciar Sesión");

        JLabel lblPie = new JLabel("© 2026 Sistema Colegio. Todos los derechos reservados.");
        lblPie.setForeground(Color.GRAY);
        lblPie.setFont(lblPie.getFont().deriveFont(12f));

        izquierdo.add(lblSistema);
        izquierdo.add(lblColegio, "gapbottom 8");
        izquierdo.add(lblBienvenido);
        izquierdo.add(lblSubtitulo, "gapbottom 20");
        izquierdo.add(txtUsuario, "w 230!, sg campo");
        izquierdo.add(txtPass, "w 230!, sg campo, gapbottom 5");
        izquierdo.add(lblNota, "gapbottom 5");
        izquierdo.add(btnLogin, "w 230!, gapbottom 90");
        izquierdo.add(lblPie);

        // ---------- Panel derecho ----------
        add(izquierdo, "width 50%, grow");
        add(new PanelDerecho(), "width 50%, grow");

        getRootPane().setDefaultButton(btnLogin);
    }//cierra constructor LoginView

    /**
     * Este método obtiene el nombre de usuario ingresado en el formulario.
     * @return el texto del campo de usuario, sin espacios en los extremos.
     */
    public String getUsuario() {
        return txtUsuario.getText().trim();
    }

    /**
     * Este método Obtiene la contraseña ingresada en el formulario.
     * @return la contraseña en formato de cadena de texto.
     */
    public String getPassword() {
        return new String(txtPass.getPassword());
    }

    /**
     * Este método obtiene el botón principal de inicio de sesión.
     * @return el componente JButton para asignar eventos desde el controlador.
     */
    public JButton getBtnLogin() {
        return btnLogin;
    }

    /**
     * Clase interna: Panel derecho, si existe la imagen en resources la dibuja a pantalla completa;
     * si no, usa un degradado naranja como respaldo.
     */
    private static class PanelDerecho extends JPanel {

        private final Image imagen;

        /**
         * Este constructor construye el panel derecho e intenta cargar la imagen desde los recursos del proyecto.
         */
        PanelDerecho() {
            setLayout(new MigLayout("insets 40, al center center, wrap", "[grow]"));

            URL url = LoginView.class.getResource(RUTA_IMAGEN);
            imagen = (url != null) ? new ImageIcon(url).getImage() : null;

            if (imagen == null) {
                JLabel l1 = new JLabel("Sistema de Gestión");
                l1.setForeground(Color.WHITE);
                l1.setFont(l1.getFont().deriveFont(Font.PLAIN, 32f));

                JLabel l2 = new JLabel("Académica");
                l2.setForeground(Color.WHITE);
                l2.setFont(l2.getFont().deriveFont(Font.BOLD, 56f));

                add(l1, "al center");
                add(l2, "al center");
            }
        }//cierra constructor panel derecho

        /**
         * Este método central de Java Swing, renderiza el componente aplicando la imagen de fondo o el degradado.
         * @param g el contexto gráfico proporcionado por Swing.
         */
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (imagen == null) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setPaint(new GradientPaint(0, 0, ACCENT_LIGHT, getWidth(), getHeight(), ACCENT));
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.dispose();
                return;
            }

            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

            int w = getWidth();
            int h = getHeight();
            int iw = imagen.getWidth(null);
            int ih = imagen.getHeight(null);
            double escala = Math.max((double) w / iw, (double) h / ih);

            int dw = (int) (iw * escala);
            int dh = (int) (ih * escala);
            g2.drawImage(imagen, (w - dw) / 2, (h - dh) / 2, dw, dh, null);
            g2.dispose();
        }//cierre de método central de swing
    }//cierra clase interna - panel derecho
}//cierra class LoginView