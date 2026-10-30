package pe.edu.pitagoras.sga.view.auth;

import com.formdev.flatlaf.FlatClientProperties;
import java.awt.Dimension;
import java.awt.Font;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import net.miginfocom.swing.MigLayout;
import pe.edu.pitagoras.sga.config.Aplicacion;
import pe.edu.pitagoras.sga.model.auth.ResultadoLogin;
import pe.edu.pitagoras.sga.view.comun.Mensajes;
import pe.edu.pitagoras.sga.view.comun.Tarea;
import pe.edu.pitagoras.sga.view.comun.Tarjeta;
import pe.edu.pitagoras.sga.view.comun.Tema;
import pe.edu.pitagoras.sga.view.principal.FrmPrincipal;

/**
 * Inicio de sesión (RF01). No hay selector de rol: el perfil sale de la base de datos según la
 * cuenta. Tras tres intentos fallidos la cuenta se bloquea 15 minutos (RF02).
 */
public class LoginFrame extends JFrame {


    private final transient Aplicacion app;
    private final JTextField txtUsuario = new JTextField();
    private final JPasswordField txtClave = new JPasswordField();
    private final JButton btnIngresar = Tema.botonPrimario("Ingresar");
    private final JLabel lblEstado = new JLabel(" ");

    public LoginFrame(Aplicacion app) {
        this.app = app;
        setTitle("Sistema de Gestión Académica - Colegio Particular Pitágoras");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(new Dimension(1000, 680));
        setMinimumSize(new Dimension(720, 560));
        setLocationRelativeTo(null);
        JPanel fondo = new JPanel(new java.awt.BorderLayout());
        fondo.setBackground(Tema.FONDO);
        setContentPane(fondo);
        JPanel centro = new JPanel(new java.awt.GridBagLayout());
        centro.setOpaque(false);
        fondo.add(centro, java.awt.BorderLayout.CENTER);

        Tarjeta tarjeta = new Tarjeta() {
            @Override
            protected void paintComponent(java.awt.Graphics g) {
                super.paintComponent(g);
                g.setColor(Tema.DORADO);
                g.fillRect(8, 0, getWidth() - 16, 4); // franja amarilla superior
            }
        };
        tarjeta.setLayout(new MigLayout("insets 30 34 30 34, wrap, fillx", "[320!]"));
        JLabel titulo = new JLabel("<html><center>Sistema de Gestión<br>Académica</center></html>", JLabel.CENTER);
        titulo.setFont(titulo.getFont().deriveFont(Font.BOLD, 24f));
        titulo.setForeground(Tema.TEXTO);
        JLabel colegio = Tema.subtitulo("Colegio Particular Pitágoras");
        colegio.setHorizontalAlignment(JLabel.CENTER);

        txtUsuario.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Ingrese su usuario");
        txtClave.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Ingrese su contraseña");
        txtClave.putClientProperty(FlatClientProperties.STYLE, "showRevealButton:true");

        JLabel nota = new JLabel("<html>Tras tres intentos fallidos la cuenta se bloquea temporalmente. "
                + "En el primer ingreso se solicita cambiar la contraseña.</html>");
        nota.setForeground(Tema.TEXTO_SUAVE);
        nota.setFont(nota.getFont().deriveFont(12f));
        lblEstado.setForeground(Tema.PELIGRO);

        javax.swing.ImageIcon escudo = Tema.escudo(92);
        if (escudo != null) {
            tarjeta.add(new JLabel(escudo), "center, gapbottom 6");
        }
        tarjeta.add(titulo, "growx, center");
        tarjeta.add(colegio, "growx, gapbottom 18");
        tarjeta.add(Tema.etiqueta("Usuario"));
        tarjeta.add(txtUsuario, "growx, h 38!, gapbottom 8");
        tarjeta.add(Tema.etiqueta("Contraseña"));
        tarjeta.add(txtClave, "growx, h 38!, gapbottom 12");
        tarjeta.add(btnIngresar, "growx, h 40!");
        tarjeta.add(lblEstado, "growx");
        tarjeta.add(nota, "growx, w 320!");
        centro.add(tarjeta);

        JLabel pie = Tema.subtitulo("© 2026 Colegio Particular Pitágoras - Cerro de Pasco");
        pie.setHorizontalAlignment(JLabel.CENTER);
        pie.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 16, 0));
        fondo.add(pie, java.awt.BorderLayout.SOUTH);

        getRootPane().setDefaultButton(btnIngresar);
        btnIngresar.addActionListener(e -> ingresar());
    }

    private void ingresar() {
        String usuario = txtUsuario.getText();
        char[] clave = txtClave.getPassword(); // el controlador la borra de memoria al terminar
        txtClave.setText("");
        lblEstado.setText(" ");
        btnIngresar.setEnabled(false);
        // Si hay error (clave incorrecta, cuenta bloqueada, BD apagada), Tarea muestra el mensaje.
        Tarea.ejecutar(this, () -> app.authController().iniciarSesion(usuario, clave), this::alIngresar,
                () -> btnIngresar.setEnabled(true));
    }

    private void alIngresar(ResultadoLogin resultado) {
        if (resultado.debeCambiarPassword()) {
            Mensajes.info(this, "Es su primer ingreso o su clave fue restablecida.\nDebe crear una contraseña nueva.");
            CambiarClaveDialog dialogo = new CambiarClaveDialog(this, app.authController(), resultado.sesion(), true);
            dialogo.setVisible(true);
            if (!dialogo.isCambiada()) {
                lblEstado.setText("Debe cambiar la contraseña para continuar.");
                return;
            }
        }
        FrmPrincipal principal = new FrmPrincipal(app, resultado.sesion());
        principal.setVisible(true);
        dispose();
    }
}
