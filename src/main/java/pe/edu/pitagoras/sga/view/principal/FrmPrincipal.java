package pe.edu.pitagoras.sga.view.principal;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import pe.edu.pitagoras.sga.config.Aplicacion;
import pe.edu.pitagoras.sga.controller.ApoderadoController;
import pe.edu.pitagoras.sga.controller.DirectorController;
import pe.edu.pitagoras.sga.controller.DocenteController;
import pe.edu.pitagoras.sga.model.auth.Rol;
import pe.edu.pitagoras.sga.model.auth.Sesion;
import pe.edu.pitagoras.sga.view.apoderado.PnlAsistenciaApoderado;
import pe.edu.pitagoras.sga.view.apoderado.PnlLibreta;
import pe.edu.pitagoras.sga.view.apoderado.PnlNotasApoderado;
import pe.edu.pitagoras.sga.view.auth.CambiarClaveDialog;
import pe.edu.pitagoras.sga.view.auth.LoginFrame;
import pe.edu.pitagoras.sga.view.comun.Mensajes;
import pe.edu.pitagoras.sga.view.comun.PanelModulo;
import pe.edu.pitagoras.sga.view.comun.Tarea;
import pe.edu.pitagoras.sga.view.comun.Tema;
import pe.edu.pitagoras.sga.view.director.PnlAuditoriaRespaldo;
import pe.edu.pitagoras.sga.view.director.PnlCargaAcademica;
import pe.edu.pitagoras.sga.view.director.PnlConfigAcademica;
import pe.edu.pitagoras.sga.view.director.PnlDashboard;
import pe.edu.pitagoras.sga.view.director.PnlInscripcion;
import pe.edu.pitagoras.sga.view.director.PnlPeriodos;
import pe.edu.pitagoras.sga.view.director.PnlReportes;
import pe.edu.pitagoras.sga.view.director.PnlUsuarios;
import pe.edu.pitagoras.sga.view.docente.PnlMiCarga;
import pe.edu.pitagoras.sga.view.docente.PnlRegistroAsistencia;
import pe.edu.pitagoras.sga.view.docente.PnlRegistroNotas;
import pe.edu.pitagoras.sga.view.docente.PnlReportesDocente;

/**
 * Ventana principal con barra lateral. El menú se arma según el rol de la sesión (RF01, RNF03):
 * cada perfil ve solo sus módulos. Los paneles se crean la primera vez que se abren.
 */
public class FrmPrincipal extends JFrame {

    private final transient Aplicacion app;
    private final transient Sesion sesion;
    private final CardLayout tarjetas = new CardLayout();
    private final JPanel contenedor = new JPanel(tarjetas);
    private final JPanel menu = new JPanel();
    private final Map<String, ItemMenu> items = new LinkedHashMap<>();
    private final Map<String, Supplier<PanelModulo>> fabricas = new LinkedHashMap<>();
    private final Map<String, PanelModulo> paneles = new LinkedHashMap<>();
    private String actual;

    public FrmPrincipal(Aplicacion app, Sesion sesion) {
        this.app = app;
        this.sesion = sesion;
        setTitle("Gestión Académica - Colegio Pitágoras");
        setSize(1366, 800);
        setMinimumSize(new Dimension(1150, 680));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (puedeSalirDelModulo()) {
                    dispose();
                    System.exit(0);
                }
            }
        });

        setLayout(new BorderLayout());
        add(crearBarraLateral(), BorderLayout.WEST);
        contenedor.setBackground(Tema.FONDO);
        add(contenedor, BorderLayout.CENTER);
        add(crearBarraEstado(), BorderLayout.SOUTH);

        registrarModulos();
        abrir(fabricas.keySet().iterator().next());
    }

    private void registrarModulos() {
        switch (sesion.getRol()) {
            case DIRECTOR -> {
                DirectorController c = app.directorController(sesion);
                modulo("Panel de indicadores", () -> new PnlDashboard(c));
                modulo("Usuarios", () -> new PnlUsuarios(c));
                modulo("Inscripción de alumnos", () -> new PnlInscripcion(c));
                modulo("Configuración académica", () -> new PnlConfigAcademica(c));
                modulo("Carga académica", () -> new PnlCargaAcademica(c));
                modulo("Periodos", () -> new PnlPeriodos(c));
                modulo("Reportes y exportación", () -> new PnlReportes(c));
                modulo("Auditoría y respaldo", () -> new PnlAuditoriaRespaldo(c));
            }
            case DOCENTE -> {
                DocenteController c = app.docenteController(sesion);
                modulo("Mi carga académica", () -> new PnlMiCarga(c));
                modulo("Calificaciones", () -> new PnlRegistroNotas(c));
                modulo("Asistencia", () -> new PnlRegistroAsistencia(c));
                modulo("Reportes", () -> new PnlReportesDocente(c));
            }
            case APODERADO -> {
                ApoderadoController c = app.apoderadoController(sesion);
                modulo("Calificaciones", () -> new PnlNotasApoderado(c));
                modulo("Asistencia", () -> new PnlAsistenciaApoderado(c));
                modulo("Libreta de notas", () -> new PnlLibreta(c));
            }
        }
        menu.add(new ItemMenu("Cambiar contraseña", this::cambiarClave));
        menu.add(new ItemMenu("Cerrar sesión", this::cerrarSesion));
        menu.add(Box.createVerticalGlue());
    }

    private void modulo(String nombre, Supplier<PanelModulo> fabrica) {
        ItemMenu item = new ItemMenu(nombre, () -> abrir(nombre));
        items.put(nombre, item);
        fabricas.put(nombre, fabrica);
        menu.add(item);
    }

    private void abrir(String nombre) {
        if (nombre.equals(actual) || !puedeSalirDelModulo()) {
            return;
        }
        PanelModulo panel = paneles.computeIfAbsent(nombre, n -> {
            PanelModulo p = fabricas.get(n).get();
            contenedor.add(p, n);
            return p;
        });
        actual = nombre;
        items.forEach((n, item) -> item.setActivo(n.equals(nombre)));
        tarjetas.show(contenedor, nombre);
        panel.mostrar();
    }

    /** Alerta de cambios sin guardar (RF13) antes de cambiar de módulo o salir. */
    private boolean puedeSalirDelModulo() {
        PanelModulo p = actual == null ? null : paneles.get(actual);
        return p == null || !p.tieneCambiosSinGuardar()
                || Mensajes.confirmar(this, "Hay cambios sin guardar en \"" + actual + "\".\n¿Desea descartarlos?");
    }

    private final JLabel lblPersona = new JLabel(" ");

    private JComponent crearBarraLateral() {
        JPanel barra = new JPanel(new BorderLayout());
        barra.setBackground(Tema.MARINO);
        barra.setPreferredSize(new Dimension(250, 0));
        JPanel cab = new JPanel();
        cab.setOpaque(false);
        cab.setLayout(new BoxLayout(cab, BoxLayout.Y_AXIS));
        cab.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(0x34507A)),
                BorderFactory.createEmptyBorder(20, 22, 18, 16)));
        JLabel colegio = new JLabel("Colegio Pitágoras");
        colegio.setForeground(Color.WHITE);
        colegio.setFont(colegio.getFont().deriveFont(Font.BOLD, 18f));
        javax.swing.ImageIcon escudo = Tema.escudo(56);
        if (escudo != null) {
            JLabel logo = new JLabel(escudo);
            logo.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
            cab.add(logo);
        }
        JLabel rol = new JLabel(textoRol(sesion.getRol()));
        rol.setForeground(new Color(0xB8C7DB));
        rol.setFont(rol.getFont().deriveFont(13f));
        lblPersona.setForeground(new Color(0xB8C7DB));
        lblPersona.setFont(lblPersona.getFont().deriveFont(12f));
        cab.add(colegio);
        cab.add(Box.createVerticalStrut(4));
        cab.add(rol);
        cab.add(Box.createVerticalStrut(2));
        cab.add(lblPersona);
        menu.setOpaque(false);
        menu.setLayout(new BoxLayout(menu, BoxLayout.Y_AXIS));
        menu.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
        barra.add(cab, BorderLayout.NORTH);
        barra.add(menu, BorderLayout.CENTER);
        // El nombre real se consulta en segundo plano para no demorar la apertura.
        Tarea.ejecutar(this, () -> app.nombreParaMostrar(sesion), n -> lblPersona.setText(n.contains(" (") ? n.substring(0, n.indexOf(" (")) : n));
        return barra;
    }

    /** Barra de estado inferior, típica de una aplicación de escritorio. */
    private JComponent crearBarraEstado() {
        JPanel estado = new JPanel(new BorderLayout());
        estado.setBackground(Color.WHITE);
        estado.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, Tema.BORDE),
                BorderFactory.createEmptyBorder(5, 14, 5, 14)));
        JLabel izq = Tema.subtitulo("Conectado a la base de datos del colegio (red local)  ·  Usuario: "
                + sesion.getUsername());
        JLabel der = Tema.subtitulo("Sistema de Gestión Académica v1.0  ·  "
                + java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        estado.add(izq, BorderLayout.WEST);
        estado.add(der, BorderLayout.EAST);
        return estado;
    }

    private static String textoRol(Rol rol) {
        return switch (rol) {
            case DIRECTOR -> "Director / Coordinador";
            case DOCENTE -> "Docente";
            case APODERADO -> "Alumno / Apoderado";
        };
    }

    private void cambiarClave() {
        new CambiarClaveDialog(this, app.authController(), sesion, false).setVisible(true);
    }

    private void cerrarSesion() {
        if (puedeSalirDelModulo() && Mensajes.confirmar(this, "¿Desea cerrar la sesión?")) {
            dispose();
            new LoginFrame(app).setVisible(true);
        }
    }
}
