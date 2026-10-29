package pe.edu.utp.pitagoras.view;

import pe.edu.utp.pitagoras.config.EstilosUI;
import pe.edu.utp.pitagoras.controller.AuthController;
import pe.edu.utp.pitagoras.model.UsuarioSesion;
import pe.edu.utp.pitagoras.view.apoderado.PnlConsultaApoderado;
import pe.edu.utp.pitagoras.view.director.*;
import pe.edu.utp.pitagoras.view.docente.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.LinkedHashMap;
import java.util.Map;

public class FrmPrincipal extends JFrame {
    private final AuthController authController;
    private final UsuarioSesion sesion;
    private final CardLayout cardLayout = new CardLayout();
    private final JPanel panelContenedorVistas = new JPanel(cardLayout);
    private final Map<String, JPanel> itemsMenu = new LinkedHashMap<>();

    public FrmPrincipal(AuthController authController, UsuarioSesion sesion) {
        this.authController = authController;
        this.sesion = sesion;

        setTitle("Sistema de Gestión Académica - Colegio Particular Pitágoras [" + sesion.getRol() + "]");
        setSize(1180, 660);
        setMinimumSize(new Dimension(1020, 580));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        inicializarLayout();
    }

    private void inicializarLayout() {
        // 1. SIDEBAR IZQUIERDO (Idéntico a Figuras 11, 12, 13 y 14)
        JPanel sidebar = new JPanel();
        sidebar.setBackground(EstilosUI.SIDEBAR_BG);
        sidebar.setPreferredSize(new Dimension(215, 0));
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));

        JPanel headerSidebar = new JPanel();
        headerSidebar.setBackground(EstilosUI.SIDEBAR_BG);
        headerSidebar.setLayout(new BoxLayout(headerSidebar, BoxLayout.Y_AXIS));
        headerSidebar.setBorder(new EmptyBorder(22, 20, 22, 20));
        headerSidebar.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel lblColegio = new JLabel("Colegio Pitágoras");
        lblColegio.setForeground(Color.WHITE);
        lblColegio.setFont(new Font("SansSerif", Font.BOLD, 16));

        JLabel lblRol = new JLabel(sesion.getRol());
        lblRol.setForeground(new Color(175, 195, 218));
        lblRol.setFont(new Font("SansSerif", Font.PLAIN, 12));

        headerSidebar.add(lblColegio);
        headerSidebar.add(Box.createVerticalStrut(4));
        headerSidebar.add(lblRol);
        sidebar.add(headerSidebar);

        // 2. CARGA DINÁMICA DE MÓDULOS SEGÚN PERFIL (RF01 / RNF03)
        switch (sesion.getRol()) {
            case "Director / Coordinador" -> configurarMenuDirector(sidebar);
            case "Docente" -> configurarMenuDocente(sidebar);
            case "Alumno / Apoderado" -> configurarMenuApoderado(sidebar);
        }

        // Opciones transversales (CU02 Cambiar contraseña y Cerrar sesión)
        agregarItemAccion(sidebar, "Cambiar contraseña", this::mostrarDialogoCambioPassword);
        agregarItemAccion(sidebar, "Cerrar sesión", this::cerrarSesion);
        sidebar.add(Box.createVerticalGlue());

        add(sidebar, BorderLayout.WEST);
        add(panelContenedorVistas, BorderLayout.CENTER);
    }

    private void configurarMenuDirector(JPanel sidebar) {
        // Exactamente los ítems de la Figura 13 (pág. 35)
        registrarModulo(sidebar, "Panel de indicadores", new PnlDashboardDirector(), true);
        registrarModulo(sidebar, "Usuarios", new PnlUsuarios(), false);
        registrarModulo(sidebar, "Inscripción de alumnos", new PnlInscripcion(), false);
        registrarModulo(sidebar, "Configuración académica", new PnlConfigAcademica(), false);
        registrarModulo(sidebar, "Carga académica", EstilosUI.envolver(new PnlCargaAcademica()), false);
        registrarModulo(sidebar, "Periodos", EstilosUI.envolver(new PnlPeriodos()), false);
        registrarModulo(sidebar, "Reportes y exportación", EstilosUI.envolver(new PnlReportesSIAGIE()), false);
        registrarModulo(sidebar, "Auditoría y respaldo", EstilosUI.envolver(new PnlAuditoriaRespaldo()), false);
    }

    private void configurarMenuDocente(JPanel sidebar) {
        // Exactamente los ítems de las Figuras 11 y 12 (págs. 34 y 35)
        registrarModulo(sidebar, "Mi carga académica", new PnlMiCargaDocente(), false);
        registrarModulo(sidebar, "Calificaciones", new PnlRegistroNotas(), false);
        registrarModulo(sidebar, "Asistencia", new PnlRegistroAsistencia(), true);
        registrarModulo(sidebar, "Reportes", new PnlReportesDocente(), false);
    }

    private void configurarMenuApoderado(JPanel sidebar) {
        // Exactamente los ítems de la Figura 14 (pág. 36)
        PnlConsultaApoderado pnlApoderado = new PnlConsultaApoderado();
        registrarModulo(sidebar, "Calificaciones", pnlApoderado, true);
        registrarModulo(sidebar, "Asistencia", new PnlConsultaApoderado(), false);
        registrarModulo(sidebar, "Libreta de notas", new PnlConsultaApoderado(), false);
    }

    private void registrarModulo(JPanel sidebar, String nombreMenu, JComponent vista, boolean activoPorDefecto) {
        panelContenedorVistas.add(vista, nombreMenu);

        JPanel item = new JPanel(new BorderLayout());
        item.setBackground(activoPorDefecto ? EstilosUI.SIDEBAR_ACTIVO : EstilosUI.SIDEBAR_BG);
        item.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        item.setPreferredSize(new Dimension(215, 42));
        item.setBorder(new EmptyBorder(0, 20, 0, 15));
        item.setAlignmentX(Component.LEFT_ALIGNMENT);
        item.setCursor(new Cursor(Cursor.HAND_CURSOR));

        JLabel lbl = new JLabel(nombreMenu);
        lbl.setForeground(Color.WHITE);
        lbl.setFont(new Font("SansSerif", activoPorDefecto ? Font.BOLD : Font.PLAIN, 13));
        item.add(lbl, BorderLayout.CENTER);

        item.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                activarItemMenu(nombreMenu);
            }
        });

        itemsMenu.put(nombreMenu, item);
        sidebar.add(item);

        if (activoPorDefecto) {
            cardLayout.show(panelContenedorVistas, nombreMenu);
        }
    }

    private void agregarItemAccion(JPanel sidebar, String texto, Runnable accion) {
        JPanel item = new JPanel(new BorderLayout());
        item.setBackground(EstilosUI.SIDEBAR_BG);
        item.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        item.setPreferredSize(new Dimension(215, 42));
        item.setBorder(new EmptyBorder(0, 20, 0, 15));
        item.setAlignmentX(Component.LEFT_ALIGNMENT);
        item.setCursor(new Cursor(Cursor.HAND_CURSOR));

        JLabel lbl = new JLabel(texto);
        lbl.setForeground(new Color(210, 220, 235));
        lbl.setFont(new Font("SansSerif", Font.PLAIN, 13));
        item.add(lbl, BorderLayout.CENTER);

        item.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                accion.run();
            }
        });
        sidebar.add(item);
    }

    public void activarItemMenu(String nombreMenu) {
        itemsMenu.forEach((nombre, panel) -> {
            boolean activo = nombre.equals(nombreMenu);
            panel.setBackground(activo ? EstilosUI.SIDEBAR_ACTIVO : EstilosUI.SIDEBAR_BG);
            JLabel lbl = (JLabel) panel.getComponent(0);
            lbl.setFont(new Font("SansSerif", activo ? Font.BOLD : Font.PLAIN, 13));
        });
        cardLayout.show(panelContenedorVistas, nombreMenu);
    }

    private void mostrarDialogoCambioPassword() {
        String nueva = JOptionPane.showInputDialog(this,
                "Ingrese su nueva contraseña (RF02 / CU02):",
                "Cambiar Contraseña", JOptionPane.PLAIN_MESSAGE);
        if (nueva != null && nueva.trim().length() >= 4) {
            authController.actualizarPassword(sesion, nueva.trim());
            JOptionPane.showMessageDialog(this, "Contraseña actualizada con hash BCrypt correctamente.");
        }
    }

    private void cerrarSesion() {
        int resp = JOptionPane.showConfirmDialog(this,
                "¿Desea cerrar la sesión actual?", "Cerrar Sesión", JOptionPane.YES_NO_OPTION);
        if (resp == JOptionPane.YES_OPTION) {
            new FrmLogin(authController).setVisible(true);
            this.dispose();
        }
    }
}
