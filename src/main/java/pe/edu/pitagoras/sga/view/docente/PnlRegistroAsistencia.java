package pe.edu.pitagoras.sga.view.docente;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.swing.AbstractCellEditor;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.DefaultCellEditor;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import pe.edu.pitagoras.sga.controller.DocenteController;
import pe.edu.pitagoras.sga.model.academico.CargaAcademica;
import pe.edu.pitagoras.sga.model.evaluacion.EstadoAsistencia;
import pe.edu.pitagoras.sga.model.evaluacion.FilaAsistencia;
import pe.edu.pitagoras.sga.view.comun.Fechas;
import pe.edu.pitagoras.sga.view.comun.Mensajes;
import pe.edu.pitagoras.sga.view.comun.PanelModulo;
import pe.edu.pitagoras.sga.view.comun.RenderCombo;
import pe.edu.pitagoras.sga.view.comun.Tarea;
import pe.edu.pitagoras.sga.view.comun.Tema;

/**
 * RF12: asistencia diaria por sección (asistió, falta, tardanza y falta justificada) con guardado
 * en lote. La falta justificada se marca con la casilla "Justificada" y conserva el estado Falta.
 */
public class PnlRegistroAsistencia extends PanelModulo {

    /** Estados del combo; la justificación va aparte, en la casilla. */
    private static final EstadoAsistencia[] ESTADOS = {EstadoAsistencia.ASISTIO, EstadoAsistencia.FALTA,
        EstadoAsistencia.TARDANZA};

    private final transient DocenteController controller;
    private final SelectorCarga selector = new SelectorCarga();
    private final JSpinner spFecha = Fechas.selector(LocalDate.now());
    private final Modelo modelo = new Modelo();
    private final JTable tabla = new JTable(modelo);
    private final JLabel lblResumen = new JLabel(" ");
    private final JLabel lblRegistro = new JLabel(" ");
    private final JButton btnGuardar = Tema.botonPrimario("Guardar asistencia");
    private boolean cargando;
    private CargaAcademica cargaMostrada;
    private LocalDate fechaMostrada;

    public PnlRegistroAsistencia(DocenteController controller) {
        super("Registro de asistencia", "Guardado en lote por sección y fecha");
        this.controller = controller;
        JButton btnTodos = Tema.botonSecundario("Marcar todos presentes");
        spFecha.setPreferredSize(new Dimension(150, 36));

        JPanel filtros = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        filtros.setOpaque(false);
        filtros.add(Tema.campo("Sección", selector.cboSeccion));
        filtros.add(Box.createHorizontalStrut(16));
        filtros.add(Tema.campo("Área", selector.cboArea));
        filtros.add(Box.createHorizontalStrut(16));
        filtros.add(Tema.campo("Fecha", spFecha));
        filtros.add(Box.createHorizontalStrut(16));
        filtros.add(Tema.campo(" ", btnTodos));
        JPanel norte = new JPanel(new BorderLayout(0, 12));
        norte.setOpaque(false);
        norte.add(filtros, BorderLayout.NORTH);
        norte.add(lblRegistro, BorderLayout.SOUTH);

        lblResumen.setForeground(Tema.ALERTA);
        JPanel sur = new JPanel(new BorderLayout());
        sur.setOpaque(false);
        sur.add(lblResumen, BorderLayout.WEST);
        sur.add(btnGuardar, BorderLayout.EAST);

        tabla.setRowHeight(46);
        var scroll = Tema.tabla(tabla);
        tabla.getColumnModel().getColumn(0).setPreferredWidth(300);
        tabla.getColumnModel().getColumn(1).setPreferredWidth(160);
        tabla.getColumnModel().getColumn(1).setCellRenderer(new RenderCombo(110, ""));
        tabla.getColumnModel().getColumn(1).setCellEditor(new DefaultCellEditor(new JComboBox<>(ESTADOS)));
        tabla.getColumnModel().getColumn(2).setPreferredWidth(150);
        CeldaJustificada justificada = new CeldaJustificada();
        tabla.getColumnModel().getColumn(2).setCellRenderer(justificada);
        tabla.getColumnModel().getColumn(2).setCellEditor(justificada);
        tabla.getColumnModel().getColumn(3).setPreferredWidth(420);
        tabla.getColumnModel().getColumn(3).setCellRenderer(new RenderTexto());
        JTextField txtObs = new JTextField();
        tabla.getColumnModel().getColumn(3).setCellEditor(new DefaultCellEditor(txtObs));

        contenido.add(norte, BorderLayout.NORTH);
        contenido.add(scroll, BorderLayout.CENTER);
        contenido.add(sur, BorderLayout.SOUTH);

        selector.setAlCambiar(this::cambiarFiltro);
        spFecha.addChangeListener(e -> cambiarFiltro());
        btnTodos.addActionListener(e -> {
            if (tabla.isEditing()) {
                tabla.getCellEditor().cancelCellEditing();
            }
            modelo.filas.forEach(f -> f.setEstado(EstadoAsistencia.ASISTIO));
            modelo.fireTableDataChanged();
            actualizarResumen();
        });
        btnGuardar.addActionListener(e -> guardar());
    }

    @Override
    protected void alMostrar(boolean primeraVez) {
        if (!primeraVez) {
            return;
        }
        cargando = true;
        Tarea.ejecutar(this, controller::misCargas, cargas -> {
            selector.setCargas(cargas);
            cargando = false;
            cargar();
        });
    }

    @Override
    public boolean tieneCambiosSinGuardar() {
        return modelo.hayCambios();
    }

    private void cambiarFiltro() {
        if (cargando) {
            return;
        }
        if (modelo.hayCambios() && !Mensajes.confirmar(this, "Hay asistencia sin guardar. ¿Desea descartarla?")) {
            cargando = true;
            selector.seleccionar(cargaMostrada);
            Fechas.poner(spFecha, fechaMostrada);
            cargando = false;
            return;
        }
        cargar();
    }

    private void cargar() {
        CargaAcademica carga = selector.getCarga();
        LocalDate fecha = Fechas.leer(spFecha);
        if (carga == null) {
            return;
        }
        Tarea.ejecutar(this, () -> controller.asistencia(carga, fecha), filas -> {
            cargaMostrada = carga;
            fechaMostrada = fecha;
            modelo.cargar(filas);
            boolean registrada = filas.stream().anyMatch(FilaAsistencia::isRegistrada);
            lblRegistro.setOpaque(true);
            lblRegistro.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));
            lblRegistro.setText(registrada
                    ? "Lista ya registrada para el " + fecha.format(Fechas.DIA_SEMANA) + ". Los cambios quedan en la bitácora."
                    : "Lista sin registrar para el " + fecha.format(Fechas.DIA_SEMANA) + ".");
            lblRegistro.setBackground(registrada ? Tema.EXITO_SUAVE : Tema.INFO_SUAVE);
            lblRegistro.setForeground(registrada ? Tema.EXITO : Tema.INFO);
            actualizarResumen();
        });
    }

    private void actualizarResumen() {
        Map<EstadoAsistencia, Integer> c = new EnumMap<>(EstadoAsistencia.class);
        modelo.filas.forEach(f -> c.merge(f.getEstado(), 1, Integer::sum));
        lblResumen.setText("Asistió " + c.getOrDefault(EstadoAsistencia.ASISTIO, 0)
                + " · Falta " + c.getOrDefault(EstadoAsistencia.FALTA, 0)
                + " · Tardanza " + c.getOrDefault(EstadoAsistencia.TARDANZA, 0)
                + " · Justificada " + c.getOrDefault(EstadoAsistencia.FALTA_JUSTIFICADA, 0)
                + "   (una falta justificada conserva el estado Falta)"
                + (modelo.hayCambios() ? "   —   cambios sin guardar" : ""));
    }

    private void guardar() {
        if (tabla.isEditing()) {
            tabla.getCellEditor().stopCellEditing();
        }
        CargaAcademica carga = cargaMostrada;
        LocalDate fecha = fechaMostrada;
        List<FilaAsistencia> filas = List.copyOf(modelo.filas);
        btnGuardar.setEnabled(false);
        Tarea.ejecutar(this, () -> controller.guardarAsistencia(carga, fecha, filas), n -> {
            Mensajes.info(this, n == 0 ? "No había cambios: la lista ya estaba guardada."
                    : "Asistencia guardada (" + n + " registro(s)).");
            cargar();
        }, () -> btnGuardar.setEnabled(true));
    }

    private static boolean esFalta(EstadoAsistencia e) {
        return e == EstadoAsistencia.FALTA || e == EstadoAsistencia.FALTA_JUSTIFICADA;
    }

    /** Columnas: Estudiante, Estado, Falta (justificada), Observación. */
    private class Modelo extends AbstractTableModel {
        private final List<FilaAsistencia> filas = new ArrayList<>();
        private final List<String> originales = new ArrayList<>();

        void cargar(List<FilaAsistencia> nuevas) {
            filas.clear();
            filas.addAll(nuevas);
            originales.clear();
            nuevas.forEach(f -> originales.add(firma(f)));
            fireTableDataChanged();
        }

        boolean hayCambios() {
            for (int i = 0; i < filas.size(); i++) {
                if (!Objects.equals(firma(filas.get(i)), originales.get(i))) {
                    return true;
                }
            }
            return false;
        }

        private String firma(FilaAsistencia f) {
            return f.getEstado() + "|" + (f.getObservacion() == null ? "" : f.getObservacion().trim());
        }

        @Override
        public int getRowCount() {
            return filas.size();
        }

        @Override
        public int getColumnCount() {
            return 4;
        }

        @Override
        public String getColumnName(int c) {
            return new String[]{"Estudiante", "Estado", "Falta", "Observación"}[c];
        }

        @Override
        public boolean isCellEditable(int f, int c) {
            return c == 1 || c == 3 || (c == 2 && esFalta(filas.get(f).getEstado()));
        }

        @Override
        public Object getValueAt(int f, int c) {
            FilaAsistencia fila = filas.get(f);
            return switch (c) {
                case 0 -> fila.getAlumno();
                case 1 -> fila.getEstado() == EstadoAsistencia.FALTA_JUSTIFICADA ? EstadoAsistencia.FALTA : fila.getEstado();
                case 2 -> fila.getEstado() == EstadoAsistencia.FALTA_JUSTIFICADA;
                default -> fila.getObservacion();
            };
        }

        @Override
        public void setValueAt(Object v, int f, int c) {
            FilaAsistencia fila = filas.get(f);
            if (c == 1 && v instanceof EstadoAsistencia e) {
                // Si sigue siendo falta, conserva la justificación marcada.
                boolean justificada = fila.getEstado() == EstadoAsistencia.FALTA_JUSTIFICADA;
                fila.setEstado(e == EstadoAsistencia.FALTA && justificada ? EstadoAsistencia.FALTA_JUSTIFICADA : e);
            } else if (c == 2 && v instanceof Boolean b && esFalta(fila.getEstado())) {
                fila.setEstado(b ? EstadoAsistencia.FALTA_JUSTIFICADA : EstadoAsistencia.FALTA);
            } else if (c == 3) {
                String texto = v == null ? null : v.toString();
                fila.setObservacion(texto != null && texto.length() > 150 ? texto.substring(0, 150) : texto);
            }
            fireTableRowsUpdated(f, f);
            actualizarResumen();
        }
    }

    /** Casilla "Justificada": solo se puede marcar cuando el estado es Falta. */
    private class CeldaJustificada extends AbstractCellEditor implements TableCellRenderer, TableCellEditor {
        private final JCheckBox render = caja();
        private final JCheckBox editor = caja();

        CeldaJustificada() {
            editor.addActionListener(e -> stopCellEditing());
        }

        private JCheckBox caja() {
            JCheckBox c = new JCheckBox("Justificada");
            c.setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 0));
            return c;
        }

        @Override
        public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foco, int f, int c) {
            render.setSelected(Boolean.TRUE.equals(v));
            render.setEnabled(esFalta(modelo.filas.get(t.convertRowIndexToModel(f)).getEstado()));
            render.setBackground(sel ? t.getSelectionBackground() : t.getBackground());
            return render;
        }

        @Override
        public Component getTableCellEditorComponent(JTable t, Object v, boolean sel, int f, int c) {
            editor.setSelected(Boolean.TRUE.equals(v));
            editor.setBackground(t.getSelectionBackground());
            return editor;
        }

        @Override
        public Object getCellEditorValue() {
            return editor.isSelected();
        }
    }

    /** Observación con aspecto de caja de texto. */
    private static class RenderTexto implements TableCellRenderer {
        private final JPanel panel = new JPanel(new BorderLayout());
        private final JTextField campo = new JTextField();

        RenderTexto() {
            panel.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 12));
            panel.add(campo);
        }

        @Override
        public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foco, int f, int c) {
            campo.setText(v == null ? "" : v.toString());
            panel.setBackground(sel ? t.getSelectionBackground() : t.getBackground());
            return panel;
        }
    }
}
