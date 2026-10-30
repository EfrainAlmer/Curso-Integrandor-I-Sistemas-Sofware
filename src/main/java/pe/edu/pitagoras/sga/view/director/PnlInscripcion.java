package pe.edu.pitagoras.sga.view.director;

import com.formdev.flatlaf.FlatClientProperties;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.RowFilter;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.TableRowSorter;
import pe.edu.pitagoras.sga.controller.DirectorController;
import pe.edu.pitagoras.sga.model.academico.Alumno;
import pe.edu.pitagoras.sga.model.academico.Seccion;
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
 * RF04: registro de estudiantes, datos del apoderado e inscripción en grado y sección. Al
 * inscribir se crea la cuenta del apoderado (RF03). La baja es lógica (RETIRADO).
 */
public class PnlInscripcion extends PanelModulo {

    private static final String TODAS = "Todas las secciones";

    private final transient DirectorController controller;
    private final JComboBox<Object> cboSeccion = Combos.nuevo();
    private final JCheckBox chkRetirados = new JCheckBox("Mostrar retirados");
    private final JTextField txtBuscar = new JTextField();
    private final JLabel lblTotal = Tema.subtitulo(" ");
    private final ModeloTabla<Alumno> modelo = new ModeloTabla<Alumno>()
            .columna("DNI", Alumno::dni)
            .columna("Apellidos y nombres", Alumno::nombreCompleto)
            .columna("Sección", a -> a.seccion().etiqueta())
            .columna("Apoderado", Alumno::nombreApoderado)
            .columna("Teléfono", a -> a.telefonoApoderado() == null ? "-" : a.telefonoApoderado())
            .columna("Cuenta apoderado", a -> a.usuarioApoderado() == null ? "-" : a.usuarioApoderado())
            .columna("Estado", String.class, a -> a.isActivo() ? "Activo" : "Retirado");
    private final JTable tabla = new JTable(modelo);
    private final TableRowSorter<ModeloTabla<Alumno>> orden = new TableRowSorter<>(modelo);
    private List<Seccion> secciones = List.of();
    private boolean cargando;

    public PnlInscripcion(DirectorController controller) {
        super("Inscripción de alumnos", "Datos del estudiante y su apoderado, grado y sección del año escolar (RF04)");
        this.controller = controller;
        JButton nuevo = Tema.botonPrimario("+ Inscribir alumno");
        accion(nuevo);

        txtBuscar.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Buscar por DNI, nombre o apoderado");
        txtBuscar.putClientProperty(FlatClientProperties.STYLE, "arc:6");
        txtBuscar.setPreferredSize(new java.awt.Dimension(300, 36));
        cboSeccion.setPreferredSize(new java.awt.Dimension(200, 36));
        chkRetirados.setOpaque(false);
        JButton editar = Tema.botonSecundario("Editar");
        JButton estado = Tema.botonPeligro("Retirar / reactivar");
        JPanel izq = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        izq.setOpaque(false);
        izq.add(cboSeccion);
        izq.add(txtBuscar);
        izq.add(chkRetirados);
        izq.add(lblTotal);
        JPanel der = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        der.setOpaque(false);
        der.add(editar);
        der.add(estado);
        JPanel barra = new JPanel(new BorderLayout());
        barra.setOpaque(false);
        barra.add(izq, BorderLayout.WEST);
        barra.add(der, BorderLayout.EAST);

        tabla.setRowSorter(orden);
        tabla.getColumnModel().getColumn(6).setCellRenderer(new RenderInsignia(true));
        tabla.getColumnModel().getColumn(1).setPreferredWidth(260);
        tabla.getColumnModel().getColumn(3).setPreferredWidth(220);
        tabla.getColumnModel().getColumn(5).setPreferredWidth(130);
        Tarjeta card = new Tarjeta();
        card.setLayout(new BorderLayout(0, 14));
        card.add(barra, BorderLayout.NORTH);
        card.add(Tema.tabla(tabla), BorderLayout.CENTER);
        contenido.add(card, BorderLayout.CENTER);

        cboSeccion.addActionListener(e -> cargar());
        chkRetirados.addActionListener(e -> cargar());
        txtBuscar.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { filtrar(); }
            public void removeUpdate(DocumentEvent e) { filtrar(); }
            public void changedUpdate(DocumentEvent e) { filtrar(); }
        });
        nuevo.addActionListener(e -> formulario(null));
        editar.addActionListener(e -> {
            Alumno a = seleccionado();
            if (a != null) {
                formulario(a);
            }
        });
        estado.addActionListener(e -> cambiarEstado());
    }

    @Override
    protected void alMostrar(boolean primeraVez) {
        if (!primeraVez) {
            cargar();
            return;
        }
        cargando = true;
        Tarea.ejecutar(this, controller::secciones, lista -> {
            secciones = lista;
            List<Object> opciones = new ArrayList<>();
            opciones.add(TODAS);
            opciones.addAll(lista);
            Combos.llenar(cboSeccion, opciones);
            cargando = false;
            cargar();
        });
    }

    private void cargar() {
        if (cargando) {
            return;
        }
        Seccion s = cboSeccion.getSelectedItem() instanceof Seccion sec ? sec : null;
        boolean soloActivos = !chkRetirados.isSelected();
        Tarea.ejecutar(this, () -> controller.alumnos(s, soloActivos), lista -> {
            modelo.setFilas(lista);
            lblTotal.setText(lista.size() + " alumnos");
        });
    }

    private void filtrar() {
        String t = txtBuscar.getText().trim();
        orden.setRowFilter(t.isEmpty() ? null
                : RowFilter.regexFilter("(?i)" + java.util.regex.Pattern.quote(t), 0, 1, 3));
    }

    private Alumno seleccionado() {
        int f = tabla.getSelectedRow();
        if (f < 0) {
            Mensajes.aviso(this, "Seleccione un alumno de la lista.");
            return null;
        }
        return modelo.getFila(tabla.convertRowIndexToModel(f));
    }

    /** @param a alumno a editar, o null para inscribir uno nuevo. */
    private void formulario(Alumno a) {
        JComboBox<Seccion> seccion = Combos.nuevo();
        Combos.llenar(seccion, secciones);
        JTextField dni = new JTextField();
        JTextField nombre = new JTextField();
        JTextField apellido = new JTextField();
        JTextField apoderado = new JTextField();
        JTextField telefono = new JTextField();
        if (a != null) {
            Combos.seleccionar(seccion, s -> s.seccionId() == a.seccion().seccionId());
            dni.setText(a.dni());
            nombre.setText(a.nombre());
            apellido.setText(a.apellido());
            apoderado.setText(a.nombreApoderado());
            telefono.setText(a.telefonoApoderado());
        } else if (cboSeccion.getSelectedItem() instanceof Seccion s) {
            seccion.setSelectedItem(s);
        }
        DialogoFormulario d = new DialogoFormulario(this, a == null ? "Inscribir alumno" : "Editar alumno",
                a == null ? "Se creará la cuenta del apoderado (usuario apo + DNI)." : null)
                .campo("Grado y sección", seccion).campo("DNI del alumno", dni).campo("Nombres", nombre)
                .campo("Apellidos", apellido).campo("Apoderado", apoderado).campo("Teléfono (opcional)", telefono);
        if (a == null) {
            d.mostrar(() -> controller.inscribir(Combos.seleccionado(seccion), dni.getText(), nombre.getText(),
                    apellido.getText(), apoderado.getText(), telefono.getText()), cred -> {
                DialogoCredencial.mostrar(this, "Alumno inscrito: cuenta del apoderado", cred);
                cargar();
            });
        } else {
            d.mostrar(() -> {
                controller.actualizarAlumno(a, Combos.seleccionado(seccion), dni.getText(), nombre.getText(),
                        apellido.getText(), apoderado.getText(), telefono.getText());
                return Boolean.TRUE;
            }, ok -> cargar());
        }
    }

    private void cambiarEstado() {
        Alumno a = seleccionado();
        if (a == null) {
            return;
        }
        boolean activar = !a.isActivo();
        String pregunta = activar ? "¿Reactivar a " + a.nombreCompleto() + "?"
                : "¿Retirar a " + a.nombreCompleto() + "?\nSu apoderado ya no podrá ingresar al sistema.";
        if (Mensajes.confirmar(this, pregunta)) {
            Tarea.ejecutarAccion(this, () -> controller.cambiarEstado(a, activar), this::cargar);
        }
    }
}
