package pe.edu.utp.pitagoras.view;

import pe.edu.utp.pitagoras.config.EstilosUI;
import pe.edu.utp.pitagoras.controller.AuthController;
import pe.edu.utp.pitagoras.model.UsuarioSesion;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;

public class FrmLogin extends JFrame {
    private final AuthController authController;
    private JTextField txtUsuario;
    private JPasswordField txtPassword;

    public FrmLogin(AuthController authController) {
        this.authController = authController;
        setTitle("Sistema de Gestión Académica - Autenticación (CU01)");
        setSize(960, 600);
        setMinimumSize(new Dimension(800, 520));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        inicializarUI();
    }

    private void inicializarUI() {
        JPanel fondo = new JPanel(new GridBagLayout());
        fondo.setBackground(EstilosUI.FONDO_GRIS);

        // Tarjeta central idéntica a la Figura 10
        JPanel card = new JPanel();
        card.setBackground(Color.WHITE);
        card.setPreferredSize(new Dimension(380, 410));
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(EstilosUI.BORDE_GRIS, 1, true),
                new EmptyBorder(32, 34, 32, 34)
        ));

        JLabel lblTitulo1 = new JLabel("Sistema de Gestión", SwingConstants.CENTER);
        lblTitulo1.setFont(new Font("SansSerif", Font.BOLD, 20));
        lblTitulo1.setForeground(EstilosUI.TEXTO_OSCURO);
        lblTitulo1.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel lblTitulo2 = new JLabel("Académica", SwingConstants.CENTER);
        lblTitulo2.setFont(new Font("SansSerif", Font.BOLD, 20));
        lblTitulo2.setForeground(EstilosUI.TEXTO_OSCURO);
        lblTitulo2.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel lblSub = new JLabel("Colegio Particular Pitágoras", SwingConstants.CENTER);
        lblSub.setFont(new Font("SansSerif", Font.PLAIN, 12));
        lblSub.setForeground(EstilosUI.TEXTO_SECUNDARIO);
        lblSub.setAlignmentX(Component.CENTER_ALIGNMENT);

        card.add(lblTitulo1);
        card.add(lblTitulo2);
        card.add(Box.createVerticalStrut(4));
        card.add(lblSub);
        card.add(Box.createVerticalStrut(24));

        // Campo Usuario
        JPanel pUser = new JPanel(new BorderLayout(0, 6));
        pUser.setBackground(Color.WHITE);
        pUser.setMaximumSize(new Dimension(Integer.MAX_VALUE, 62));
        JLabel lblUser = new JLabel("Usuario");
        lblUser.setFont(new Font("SansSerif", Font.PLAIN, 12));
        lblUser.setForeground(EstilosUI.TEXTO_OSCURO);
        txtUsuario = new JTextField("docente");
        txtUsuario.setPreferredSize(new Dimension(0, 36));
        txtUsuario.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(EstilosUI.BORDE_GRIS, 1, true),
                new EmptyBorder(0, 10, 0, 10)
        ));
        pUser.add(lblUser, BorderLayout.NORTH);
        pUser.add(txtUsuario, BorderLayout.CENTER);
        card.add(pUser);
        card.add(Box.createVerticalStrut(14));

        // Campo Contraseña
        JPanel pPass = new JPanel(new BorderLayout(0, 6));
        pPass.setBackground(Color.WHITE);
        pPass.setMaximumSize(new Dimension(Integer.MAX_VALUE, 62));
        JLabel lblPass = new JLabel("Contraseña");
        lblPass.setFont(new Font("SansSerif", Font.PLAIN, 12));
        lblPass.setForeground(EstilosUI.TEXTO_OSCURO);
        txtPassword = new JPasswordField("123456");
        txtPassword.setPreferredSize(new Dimension(0, 36));
        txtPassword.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(EstilosUI.BORDE_GRIS, 1, true),
                new EmptyBorder(0, 10, 0, 10)
        ));
        pPass.add(lblPass, BorderLayout.NORTH);
        pPass.add(txtPassword, BorderLayout.CENTER);
        card.add(pPass);
        card.add(Box.createVerticalStrut(20));

        // Botón Ingresar
        JButton btnIngresar = EstilosUI.crearBotonPrimario("Ingresar");
        btnIngresar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        btnIngresar.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnIngresar.addActionListener(e -> intentarLogin());
        card.add(btnIngresar);
        card.add(Box.createVerticalStrut(16));

        // Nota inferior de la Figura 10
        JLabel lblNota = new JLabel(
                "<html><div style='color:#6c757d;font-size:9px;'>" +
                "Tras tres intentos fallidos la cuenta se bloquea temporalmente. " +
                "En el primer ingreso se solicita cambiar la contraseña.<br><br>" +
                "<b>Demo rápida:</b> director / docente / docente2 / apoderado (Clave: 123456)</div></html>"
        );
        lblNota.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(lblNota);

        fondo.add(card);
        setContentPane(fondo);
        getRootPane().setDefaultButton(btnIngresar);
    }

    private void intentarLogin() {
        String user = txtUsuario.getText();
        String pass = new String(txtPassword.getPassword());

        AuthController.ResultadoLogin res = authController.autenticar(user, pass);
        if (!res.exito()) {
            JOptionPane.showMessageDialog(this, res.mensaje(), "Control de Acceso (RF01/RF02)", JOptionPane.WARNING_MESSAGE);
            return;
        }

        UsuarioSesion sesion = res.usuario();

        // Extensión CU02: Cambio obligatorio de contraseña en el primer ingreso (RF02)
        if (sesion.isDebeCambiarPassword()) {
            String nueva = JOptionPane.showInputDialog(this,
                    "Primer ingreso detectado (RF02 / CU02).\nPor seguridad, ingrese su nueva contraseña:",
                    "Cambio Obligatorio de Contraseña", JOptionPane.INFORMATION_MESSAGE);
            if (nueva == null || nueva.trim().length() < 4) {
                JOptionPane.showMessageDialog(this, "Debe establecer una contraseña válida (mínimo 4 caracteres) para continuar.");
                return;
            }
            authController.actualizarPassword(sesion, nueva.trim());
        }

        // Abrir el contenedor principal con los módulos correspondientes al Rol
        FrmPrincipal frmPrincipal = new FrmPrincipal(authController, sesion);
        frmPrincipal.setVisible(true);
        this.dispose();
    }
}
