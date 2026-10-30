package pe.edu.pitagoras.sga.view.director;

import com.formdev.flatlaf.FlatClientProperties;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import pe.edu.pitagoras.sga.model.academico.Curso;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.RowFilter;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.TableRowSorter;
import pe.edu.pitagoras.sga.controller.DirectorController;
import pe.edu.pitagoras.sga.model.auth.Rol;
import pe.edu.pitagoras.sga.model.gestion.CuentaUsuario;
import pe.edu.pitagoras.sga.view.comun.Combos;
import pe.edu.pitagoras.sga.view.comun.DialogoCredencial;
import pe.edu.pitagoras.sga.view.comun.DialogoFormulario;
import pe.edu.pitagoras.sga.view.comun.Mensajes;
import pe.edu.pitagoras.sga.view.comun.ModeloTabla;
import pe.edu.pitagoras.sga.view.comun.PanelModulo;
import pe.edu.pitagoras.sga.view.comun.RenderInsignia;
import pe.edu.pitagoras.sga.view.comun.Tarea;
import pe.edu.pitagoras.sga.view.comun.Tarjeta;
import pe.edu.pitagoras.sga.view.comun.Tema;

/**
 * RF03: gestión de cuentas. Crear docentes y directivos, activar/desactivar y restablecer claves.
 * Las cuentas de apoderado se crean al inscribir al alumno.
 */
public class PnlUsuarios extends PanelModulo {

    private static final String TODOS = "Todos los perfiles";

    private final transient DirectorController controller;
    private Map<Integer, List<Curso>> areas = Map.of();
    private List<Curso> cursos = List.of();
    private final ModeloTabla<CuentaUsuario> modelo = new ModeloTabla<CuentaUsuario>()
            .columna("Usuario", CuentaUsuario::username)
            .columna("Nombre", CuentaUsuario::nombre)
            .columna("Perfil", c -> perfil(c.rol()))
            .columna("Áreas que dicta", c -> String.join(", ", this.areas.getOrDefault(c.usuarioId(), List.of()).stream()
                    .map(Curso::nombre).toList()))
            .columna("Estado", String.class, PnlUsuarios::estado);
    private final JTable tabla = new JTable(modelo);
    private final TableRowSorter<ModeloTabla<CuentaUsuario>> orden = new TableRowSorter<>(modelo);
    private final JTextField txtBuscar = new JTextField();
    private final JComboBox<String> cboRol = Combos.nuevo();

    public PnlUsuarios(DirectorController controller) {
        super("Usuarios", "Cuentas de acceso del personal y de los apoderados (RF03)");
        this.controller = controller;
        JButton nuevoDocente = Tema.botonPrimario("+ Nuevo docente");
        JButton nuevoDirector = Tema.botonSecundario("+ Nuevo directivo");
        accion(nuevoDirector);
        accion(nuevoDocente);

        txtBuscar.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Buscar por usuario o nombre");
        txtBuscar.putClientProperty(FlatClientProperties.STYLE, "arc:6");
        txtBuscar.setPreferredSize(new java.awt.Dimension(320, 36));
        Combos.llenar(cboRol, List.of(TODOS, "Director / Coordinador", "Docente", "Alumno / Apoderado"));
        cboRol.setPreferredSize(new java.awt.Dimension(220, 36));
        JButton editar = Tema.botonSecundario("Editar datos");
        JButton restablecer = Tema.botonSecundario("Restablecer clave");
        JButton estado = Tema.botonPeligro("Activar / desactivar");
        JPanel barra = new JPanel(new BorderLayout());
        barra.setOpaque(false);
        JPanel izq = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        izq.setOpaque(false);
        izq.add(txtBuscar);
        izq.add(cboRol);
        JPanel der = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        der.setOpaque(false);
        der.add(editar);
        der.add(restablecer);
        der.add(estado);
        barra.add(izq, BorderLayout.WEST);
        barra.add(der, BorderLayout.EAST);

        tabla.setRowSorter(orden);
        tabla.getColumnModel().getColumn(4).setCellRenderer(new RenderInsignia(true));
        tabla.getColumnModel().getColumn(1).setPreferredWidth(330);
        tabla.getColumnModel().getColumn(3).setPreferredWidth(260);
        Tarjeta card = new Tarjeta();
        card.setLayout(new BorderLayout(0, 14));
        card.add(barra, BorderLayout.NORTH);
        card.add(Tema.tabla(tabla), BorderLayout.CENTER);
        card.add(Tema.subtitulo("Las claves se guardan cifradas con BCrypt. Las cuentas nuevas o restablecidas "
                + "deben cambiar su clave en el primer ingreso."), BorderLayout.SOUTH);
        contenido.add(card, BorderLayout.CENTER);

        txtBuscar.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { filtrar(); }
            public void removeUpdate(DocumentEvent e) { filtrar(); }
            public void changedUpdate(DocumentEvent e) { filtrar(); }
        });
        cboRol.addActionListener(e -> filtrar());
        nuevoDocente.addActionListener(e -> nuevoDocente());
        nuevoDirector.addActionListener(e -> nuevoDirector());
        restablecer.addActionListener(e -> restablecer());
        editar.addActionListener(e -> editar());
        estado.addActionListener(e -> cambiarEstado());
    }

    @Override
    protected void alMostrar(boolean primeraVez) {
        Tarea.ejecutar(this, () -> new Object[]{controller.usuarios(), controller.areasDocentes(), controller.cursos()},
                d -> {
                    @SuppressWarnings("unchecked")
                    Map<Integer, List<Curso>> a = (Map<Integer, List<Curso>>) d[1];
                    @SuppressWarnings("unchecked")
                    List<Curso> c = (List<Curso>) d[2];
                    @SuppressWarnings("unchecked")
                    List<CuentaUsuario> u = (List<CuentaUsuario>) d[0];
                    areas = a;
                    cursos = c;
                    modelo.setFilas(u);
                });
    }

    private void filtrar() {
        List<RowFilter<ModeloTabla<CuentaUsuario>, Integer>> filtros = new ArrayList<>();
        String texto = txtBuscar.getText().trim();
        if (!texto.isEmpty()) {
            filtros.add(RowFilter.regexFilter("(?i)" + java.util.regex.Pattern.quote(texto), 0, 1));
        }
        String rol = (String) cboRol.getSelectedItem();
        if (rol != null && !TODOS.equals(rol)) {
            filtros.add(RowFilter.regexFilter("^" + java.util.regex.Pattern.quote(rol) + "$", 2));
        }
        orden.setRowFilter(filtros.isEmpty() ? null : RowFilter.andFilter(filtros));
    }

    private CuentaUsuario seleccionada() {
        int f = tabla.getSelectedRow();
        if (f < 0) {
            Mensajes.aviso(this, "Seleccione una cuenta de la lista.");
            return null;
        }
        return modelo.getFila(tabla.convertRowIndexToModel(f));
    }

    private void nuevoDocente() {
        JTextField usuario = new JTextField();
        JTextField dni = new JTextField();
        JTextField nombre = new JTextField();
        JTextField apellido = new JTextField();
        JTextField email = new JTextField();
        SelectorAreas selector = new SelectorAreas(cursos, List.of());
        new DialogoFormulario(this, "Nuevo docente", "Se genera una clave temporal que se mostrará una sola vez.")
                .campo("Usuario", usuario).campo("DNI", dni).campo("Nombres", nombre).campo("Apellidos", apellido)
                .campo("Correo (opcional)", email).campoLibre("Áreas que dicta (1 a 3)", selector)
                .mostrar(() -> controller.crearDocente(usuario.getText(), dni.getText(), nombre.getText(),
                        apellido.getText(), email.getText(), selector.seleccionadas()), c -> {
                    DialogoCredencial.mostrar(this, "Cuenta creada", c);
                    alMostrar(false);
                });
    }

    private void nuevoDirector() {
        JTextField usuario = new JTextField();
        new DialogoFormulario(this, "Nuevo directivo", "Cuenta con perfil Director / Coordinador.")
                .campo("Usuario", usuario)
                .mostrar(() -> controller.crearDirector(usuario.getText()), c -> {
                    DialogoCredencial.mostrar(this, "Cuenta creada", c);
                    alMostrar(false);
                });
    }

    private void editar() {
        CuentaUsuario c = seleccionada();
        if (c == null) {
            return;
        }
        Tarea.ejecutar(this, () -> controller.docenteDeCuenta(c), d -> {
            JTextField dni = new JTextField(d.dni());
            JTextField nombre = new JTextField(d.nombre());
            JTextField apellido = new JTextField(d.apellido());
            JTextField email = new JTextField(d.email() == null ? "" : d.email());
            SelectorAreas selector = new SelectorAreas(cursos, areas.getOrDefault(c.usuarioId(), List.of()));
            new DialogoFormulario(this, "Editar docente", "Cuenta: " + c.username())
                    .campo("DNI", dni).campo("Nombres", nombre).campo("Apellidos", apellido).campo("Correo", email)
                    .campoLibre("Áreas que dicta (1 a 3)", selector)
                    .mostrar(() -> {
                        controller.actualizarDocente(d, dni.getText(), nombre.getText(), apellido.getText(),
                                email.getText(), selector.seleccionadas());
                        return Boolean.TRUE;
                    }, ok -> alMostrar(false));
        });
    }

    private void restablecer() {
        CuentaUsuario c = seleccionada();
        if (c != null && Mensajes.confirmar(this, "¿Generar una clave temporal nueva para " + c.username() + "?")) {
            Tarea.ejecutar(this, () -> controller.restablecerClave(c), cred -> {
                DialogoCredencial.mostrar(this, "Clave restablecida", cred);
                alMostrar(false);
            });
        }
    }

    private void cambiarEstado() {
        CuentaUsuario c = seleccionada();
        if (c == null) {
            return;
        }
        boolean activar = !c.activo();
        if (Mensajes.confirmar(this, (activar ? "¿Activar" : "¿Desactivar") + " la cuenta " + c.username() + "?")) {
            Tarea.ejecutarAccion(this, () -> controller.cambiarEstado(c, activar), () -> alMostrar(false));
        }
    }

    private static String perfil(Rol rol) {
        return switch (rol) {
            case DIRECTOR -> "Director / Coordinador";
            case DOCENTE -> "Docente";
            case APODERADO -> "Alumno / Apoderado";
        };
    }

    private static String estado(CuentaUsuario c) {
        if (!c.activo()) {
            return "Inactivo";
        }
        if (c.bloqueadoHasta() != null && c.bloqueadoHasta().isAfter(LocalDateTime.now())) {
            return "Bloqueado";
        }
        return c.debeCambiarPassword() ? "Debe cambiar clave" : "Activo";
    }
}
