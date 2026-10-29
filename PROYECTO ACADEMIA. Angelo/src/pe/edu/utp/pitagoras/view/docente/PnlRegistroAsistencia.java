package pe.edu.utp.pitagoras.view.docente;

import pe.edu.utp.pitagoras.config.EstilosUI;
import pe.edu.utp.pitagoras.controller.DocenteController;
import pe.edu.utp.pitagoras.model.RegistroAsistencia;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumnModel;
import java.awt.*;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Prototipo Figura 12 (pág. 35) — Marcación de asistencias (RF12 / CU12).
 * Incluye mini calendario desplegable interactivo (Date Picker) en el filtro Fecha,
 * nombres completos de los estudiantes, botón "Marcar todos presentes",
 * regla de negocio de falta justificada y auditoría automática (RF09).
 */
public class PnlRegistroAsistencia extends JPanel {
    private static final DateTimeFormatter FMT_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final DocenteController controller = DocenteController.getInstancia();
    private final AsistenciaModel model = new AsistenciaModel();
    private JTable tabla;
    private JComboBox<String> cboSeccion;
    private JTextField txtFecha;
    private JLabel lblLeyenda;
    private LocalDate fechaSeleccionada = LocalDate.now();

    public PnlRegistroAsistencia() {
        setLayout(new BorderLayout(0, 16));
        setBackground(EstilosUI.FONDO_GRIS);
        setBorder(new EmptyBorder(28, 36, 28, 36));
        inicializarComponentes();
        recargarDatos();
    }

    private void inicializarComponentes() {
        JPanel panelNorte = new JPanel();
        panelNorte.setBackground(EstilosUI.FONDO_GRIS);
        panelNorte.setLayout(new BoxLayout(panelNorte, BoxLayout.Y_AXIS));

        JLabel lblTitulo = new JLabel("Registro de asistencia");
        lblTitulo.setFont(new Font("SansSerif", Font.BOLD, 24));
        lblTitulo.setForeground(EstilosUI.TEXTO_OSCURO);
        lblTitulo.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel lblSub = new JLabel("Guardado en lote por sección y fecha · Docente: " + controller.getNombreDocenteActivo());
        lblSub.setFont(new Font("SansSerif", Font.PLAIN, 13));
        lblSub.setForeground(EstilosUI.TEXTO_SECUNDARIO);
        lblSub.setAlignmentX(Component.LEFT_ALIGNMENT);

        panelNorte.add(lblTitulo);
        panelNorte.add(Box.createVerticalStrut(4));
        panelNorte.add(lblSub);
        panelNorte.add(Box.createVerticalStrut(16));

        // Barra de filtros (Sección 1.° A a 5.° A, Fecha con Calendario desplegable, Marcar todos presentes)
        JPanel filtros = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        filtros.setBackground(EstilosUI.FONDO_GRIS);
        filtros.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel pSec = new JPanel(new BorderLayout(0, 4));
        pSec.setBackground(EstilosUI.FONDO_GRIS);
        pSec.add(new JLabel("Sección"), BorderLayout.NORTH);
        cboSeccion = new JComboBox<>(new String[]{"1.° A", "2.° A", "3.° A", "4.° A", "5.° A"});
        cboSeccion.setSelectedItem("3.° A");
        cboSeccion.setPreferredSize(new Dimension(90, 36));
        cboSeccion.setBackground(Color.WHITE);
        cboSeccion.addActionListener(e -> recargarDatos());
        pSec.add(cboSeccion, BorderLayout.CENTER);

        // Campo Fecha con botón de Calendario pequeño emergente (Date Picker)
        JPanel pFec = new JPanel(new BorderLayout(0, 4));
        pFec.setBackground(EstilosUI.FONDO_GRIS);
        pFec.setBorder(new EmptyBorder(0, 14, 0, 14));
        pFec.add(new JLabel("Fecha"), BorderLayout.NORTH);

        JPanel contenedorFecha = new JPanel(new BorderLayout(0, 0));
        contenedorFecha.setBackground(Color.WHITE);
        contenedorFecha.setPreferredSize(new Dimension(155, 36));
        contenedorFecha.setBorder(new LineBorder(EstilosUI.BORDE_GRIS, 1, true));

        txtFecha = new JTextField(fechaSeleccionada.format(FMT_FECHA));
        txtFecha.setEditable(false);
        txtFecha.setBackground(Color.WHITE);
        txtFecha.setFont(new Font("SansSerif", Font.PLAIN, 13));
        txtFecha.setBorder(new EmptyBorder(0, 10, 0, 4));
        txtFecha.setCursor(new Cursor(Cursor.HAND_CURSOR));

        JButton btnCalendario = new JButton("📅");
        btnCalendario.setToolTipText("Seleccionar fecha en el calendario");
        btnCalendario.setFont(new Font("SansSerif", Font.PLAIN, 13));
        btnCalendario.setBackground(new Color(240, 244, 248));
        btnCalendario.setForeground(EstilosUI.SIDEBAR_BG);
        btnCalendario.setFocusPainted(false);
        btnCalendario.setBorder(BorderFactory.createMatteBorder(0, 1, 0, 0, EstilosUI.BORDE_GRIS));
        btnCalendario.setPreferredSize(new Dimension(38, 36));
        btnCalendario.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnCalendario.addActionListener(e -> mostrarCalendarioEmergente(contenedorFecha));

        txtFecha.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                mostrarCalendarioEmergente(contenedorFecha);
            }
        });

        contenedorFecha.add(txtFecha, BorderLayout.CENTER);
        contenedorFecha.add(btnCalendario, BorderLayout.EAST);
        pFec.add(contenedorFecha, BorderLayout.CENTER);

        JPanel pBtn = new JPanel(new BorderLayout(0, 4));
        pBtn.setBackground(EstilosUI.FONDO_GRIS);
        pBtn.add(new JLabel(" "), BorderLayout.NORTH);
        JButton btnMarcarTodos = EstilosUI.crearBotonSecundario("Marcar todos presentes");
        btnMarcarTodos.setPreferredSize(new Dimension(185, 36));
        btnMarcarTodos.addActionListener(e -> {
            if (tabla.isEditing()) {
                tabla.getCellEditor().stopCellEditing();
            }
            model.marcarTodosPresentes();
            lblLeyenda.setText("Todos los estudiantes fueron marcados como 'Asistió' (Pendiente de guardar lote)");
        });
        pBtn.add(btnMarcarTodos, BorderLayout.CENTER);

        filtros.add(pSec);
        filtros.add(pFec);
        filtros.add(pBtn);
        panelNorte.add(filtros);
        add(panelNorte, BorderLayout.NORTH);

        // Tabla de asistencia con Nombres Completos de los estudiantes
        tabla = new JTable(model);
        EstilosUI.estilizarTabla(tabla);
        tabla.setRowHeight(54);

        TableColumnModel cm = tabla.getColumnModel();
        cm.getColumn(0).setPreferredWidth(250);
        cm.getColumn(1).setPreferredWidth(140);
        cm.getColumn(2).setPreferredWidth(130);
        cm.getColumn(3).setPreferredWidth(360);

        String[] estados = {"Asistió", "Falta", "Tardanza"};
        cm.getColumn(1).setCellRenderer(new ComboRenderer(estados));
        cm.getColumn(1).setCellEditor(new DefaultCellEditor(new JComboBox<>(estados)));
        cm.getColumn(2).setCellRenderer(new CheckJustificadaRenderer());
        cm.getColumn(2).setCellEditor(new CheckJustificadaEditor());
        cm.getColumn(3).setCellRenderer(new ObservacionRenderer());
        cm.getColumn(3).setCellEditor(new ObservacionEditor());

        JScrollPane scroll = new JScrollPane(tabla);
        scroll.setBorder(new LineBorder(EstilosUI.BORDE_GRIS, 1));
        scroll.getViewport().setBackground(Color.WHITE);
        add(scroll, BorderLayout.CENTER);

        // Pie de página (Figura 12)
        JPanel panelSur = new JPanel(new BorderLayout());
        panelSur.setBackground(EstilosUI.FONDO_GRIS);
        lblLeyenda = new JLabel("Asistió · Falta · Tardanza (una falta justificada conserva el estado Falta)");
        lblLeyenda.setFont(new Font("SansSerif", Font.PLAIN, 13));
        lblLeyenda.setForeground(EstilosUI.ALERTA_DORADA);

        JButton btnGuardar = EstilosUI.crearBotonPrimario("Guardar asistencia");
        btnGuardar.addActionListener(e -> guardarAsistenciaEnLote());

        panelSur.add(lblLeyenda, BorderLayout.WEST);
        panelSur.add(btnGuardar, BorderLayout.EAST);
        add(panelSur, BorderLayout.SOUTH);
    }

    /**
     * Muestra el mini calendario desplegable (Date Picker) debajo del campo Fecha.
     */
    private void mostrarCalendarioEmergente(Component ancla) {
        JPopupMenu popup = new JPopupMenu();
        popup.setBorder(new LineBorder(EstilosUI.SIDEBAR_BG, 1));
        popup.setBackground(Color.WHITE);

        JPanel contenedorCalendario = new JPanel(new BorderLayout(0, 8));
        contenedorCalendario.setBackground(Color.WHITE);
        contenedorCalendario.setBorder(new EmptyBorder(10, 12, 10, 12));
        contenedorCalendario.setPreferredSize(new Dimension(265, 240));

        final YearMonth[] mesVisible = {YearMonth.from(fechaSeleccionada)};

        // Cabecera del calendario: [<] Octubre 2026 [>]
        JPanel headerMes = new JPanel(new BorderLayout());
        headerMes.setBackground(EstilosUI.SIDEBAR_BG);
        headerMes.setBorder(new EmptyBorder(4, 6, 4, 6));

        JButton btnPrev = new JButton("◀");
        estilizarBotonNavegacionMes(btnPrev);
        JButton btnNext = new JButton("▶");
        estilizarBotonNavegacionMes(btnNext);

        JLabel lblMesAnio = new JLabel("", SwingConstants.CENTER);
        lblMesAnio.setForeground(Color.WHITE);
        lblMesAnio.setFont(new Font("SansSerif", Font.BOLD, 13));

        headerMes.add(btnPrev, BorderLayout.WEST);
        headerMes.add(lblMesAnio, BorderLayout.CENTER);
        headerMes.add(btnNext, BorderLayout.EAST);

        // Grilla de días (Lun a Dom)
        JPanel gridDias = new JPanel(new GridLayout(7, 7, 3, 3));
        gridDias.setBackground(Color.WHITE);

        Runnable renderizarMes = () -> {
            gridDias.removeAll();
            String nombreMes = mesVisible[0].getMonth().getDisplayName(TextStyle.FULL, new Locale("es", "PE"));
            nombreMes = nombreMes.substring(0, 1).toUpperCase() + nombreMes.substring(1);
            lblMesAnio.setText(nombreMes + " " + mesVisible[0].getYear());

            String[] diasSemana = {"Lu", "Ma", "Mi", "Ju", "Vi", "Sá", "Do"};
            for (String d : diasSemana) {
                JLabel lblDia = new JLabel(d, SwingConstants.CENTER);
                lblDia.setFont(new Font("SansSerif", Font.BOLD, 11));
                lblDia.setForeground(EstilosUI.TEXTO_SECUNDARIO);
                gridDias.add(lblDia);
            }

            LocalDate primeroDelMes = mesVisible[0].atDay(1);
            int offset = primeroDelMes.getDayOfWeek().getValue() - 1; // Lunes = 0
            int totalDias = mesVisible[0].lengthOfMonth();

            for (int i = 0; i < offset; i++) {
                gridDias.add(new JLabel(""));
            }

            for (int dia = 1; dia <= totalDias; dia++) {
                final LocalDate fechaBoton = mesVisible[0].atDay(dia);
                JButton btnDia = new JButton(String.valueOf(dia));
                btnDia.setMargin(new Insets(2, 2, 2, 2));
                btnDia.setFont(new Font("SansSerif",
                        fechaBoton.equals(fechaSeleccionada) ? Font.BOLD : Font.PLAIN, 12));
                btnDia.setFocusPainted(false);
                btnDia.setCursor(new Cursor(Cursor.HAND_CURSOR));

                if (fechaBoton.equals(fechaSeleccionada)) {
                    btnDia.setBackground(EstilosUI.AZUL_PRIMARIO);
                    btnDia.setForeground(Color.WHITE);
                    btnDia.setOpaque(true);
                    btnDia.setBorderPainted(false);
                } else if (fechaBoton.equals(LocalDate.now())) {
                    btnDia.setBackground(new Color(230, 240, 250));
                    btnDia.setForeground(EstilosUI.SIDEBAR_BG);
                    btnDia.setBorder(new LineBorder(EstilosUI.AZUL_PRIMARIO, 1));
                } else {
                    btnDia.setBackground(Color.WHITE);
                    btnDia.setForeground(EstilosUI.TEXTO_OSCURO);
                    btnDia.setBorder(new LineBorder(new Color(235, 238, 242), 1));
                }

                btnDia.addActionListener(ev -> {
                    fechaSeleccionada = fechaBoton;
                    txtFecha.setText(fechaSeleccionada.format(FMT_FECHA));
                    popup.setVisible(false);
                    recargarDatos();
                });
                gridDias.add(btnDia);
            }

            int celdasUsadas = 7 + offset + totalDias;
            for (int i = celdasUsadas; i < 49; i++) {
                gridDias.add(new JLabel(""));
            }

            gridDias.revalidate();
            gridDias.repaint();
        };

        btnPrev.addActionListener(e -> {
            mesVisible[0] = mesVisible[0].minusMonths(1);
            renderizarMes.run();
        });
        btnNext.addActionListener(e -> {
            mesVisible[0] = mesVisible[0].plusMonths(1);
            renderizarMes.run();
        });

        // Botón inferior "Hoy"
        JButton btnHoy = new JButton("Seleccionar Hoy (" + LocalDate.now().format(FMT_FECHA) + ")");
        btnHoy.setFont(new Font("SansSerif", Font.BOLD, 11));
        btnHoy.setForeground(EstilosUI.AZUL_PRIMARIO);
        btnHoy.setBackground(Color.WHITE);
        btnHoy.setFocusPainted(false);
        btnHoy.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnHoy.addActionListener(e -> {
            fechaSeleccionada = LocalDate.now();
            txtFecha.setText(fechaSeleccionada.format(FMT_FECHA));
            popup.setVisible(false);
            recargarDatos();
        });

        renderizarMes.run();

        contenedorCalendario.add(headerMes, BorderLayout.NORTH);
        contenedorCalendario.add(gridDias, BorderLayout.CENTER);
        contenedorCalendario.add(btnHoy, BorderLayout.SOUTH);

        popup.add(contenedorCalendario);
        popup.show(ancla, 0, ancla.getHeight() + 2);
    }

    private void estilizarBotonNavegacionMes(JButton btn) {
        btn.setForeground(Color.WHITE);
        btn.setBackground(EstilosUI.SIDEBAR_BG);
        btn.setBorder(new EmptyBorder(2, 8, 2, 8));
        btn.setFocusPainted(false);
        btn.setContentAreaFilled(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
    }

    private void recargarDatos() {
        if (tabla != null && tabla.isEditing()) {
            tabla.getCellEditor().stopCellEditing();
        }
        String seccion = (String) cboSeccion.getSelectedItem();
        String fecha = txtFecha.getText().trim();
        model.setFilas(controller.obtenerAsistencia(seccion, fecha));
        if (lblLeyenda != null) {
            lblLeyenda.setText("Asistió · Falta · Tardanza (una falta justificada conserva el estado Falta)");
        }
    }

    private void guardarAsistenciaEnLote() {
        if (tabla.isEditing()) {
            tabla.getCellEditor().stopCellEditing();
        }
        String seccion = (String) cboSeccion.getSelectedItem();
        String fecha = txtFecha.getText().trim();
        int eventos = controller.guardarAsistenciaLote(seccion, fecha, model.getFilas());
        lblLeyenda.setText("Lote guardado (" + seccion + " - " + fecha + ") · Eventos en bitácora: " + eventos);
        JOptionPane.showMessageDialog(this,
                "Asistencia de la sección " + seccion + " (" + fecha + ") guardada en lote correctamente (RF12 / CU12)\n"
                + "y registrada en la Bitácora de Auditoría (RF09).",
                "Guardado en Lote Exitoso", JOptionPane.INFORMATION_MESSAGE);
    }

    private static class AsistenciaModel extends AbstractTableModel {
        private final String[] cols = {"Estudiante", "Estado", "Falta", "Observación"};
        private List<RegistroAsistencia> filas = new ArrayList<>();

        void setFilas(List<RegistroAsistencia> nuevasFilas) {
            this.filas = new ArrayList<>(nuevasFilas);
            fireTableDataChanged();
        }

        List<RegistroAsistencia> getFilas() {
            return filas;
        }

        void marcarTodosPresentes() {
            for (RegistroAsistencia f : filas) {
                f.setEstado("Asistió");
            }
            fireTableDataChanged();
        }

        @Override public int getRowCount() { return filas.size(); }
        @Override public int getColumnCount() { return cols.length; }
        @Override public String getColumnName(int c) { return cols[c]; }
        @Override public boolean isCellEditable(int r, int c) { return c > 0; }

        @Override
        public Object getValueAt(int r, int c) {
            RegistroAsistencia f = filas.get(r);
            return switch (c) {
                case 0 -> "   " + f.getEstudianteNombre();
                case 1 -> f.getEstado();
                case 2 -> f.isJustificada();
                case 3 -> f.getObservacion();
                default -> "";
            };
        }

        @Override
        public void setValueAt(Object val, int r, int c) {
            RegistroAsistencia f = filas.get(r);
            if (c == 1) {
                f.setEstado((String) val);
            } else if (c == 2) {
                f.setJustificada(Boolean.TRUE.equals(val));
            } else if (c == 3) {
                f.setObservacion(val != null ? val.toString() : "");
            }
            fireTableRowsUpdated(r, r);
        }
    }

    static class ComboRenderer implements TableCellRenderer {
        private final JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
        private final JComboBox<String> combo;
        ComboRenderer(String[] items) {
            panel.setBackground(Color.WHITE);
            combo = new JComboBox<>(items);
            combo.setPreferredSize(new Dimension(95, 34));
            combo.setBackground(Color.WHITE);
            panel.add(combo);
        }
        @Override
        public Component getTableCellRendererComponent(JTable t, Object v, boolean s, boolean f, int r, int c) {
            combo.setSelectedItem(v);
            combo.setEnabled(t.isCellEditable(r, c));
            return panel;
        }
    }

    private static class CheckJustificadaRenderer implements TableCellRenderer {
        private final JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 14));
        private final JCheckBox chk = new JCheckBox("Justificada");
        CheckJustificadaRenderer() {
            panel.setBackground(Color.WHITE);
            chk.setBackground(Color.WHITE);
            panel.add(chk);
        }
        @Override
        public Component getTableCellRendererComponent(JTable t, Object v, boolean s, boolean f, int r, int c) {
            chk.setSelected(Boolean.TRUE.equals(v));
            return panel;
        }
    }

    private static class CheckJustificadaEditor extends AbstractCellEditor implements TableCellEditor {
        private final JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 14));
        private final JCheckBox chk = new JCheckBox("Justificada");
        CheckJustificadaEditor() {
            panel.setBackground(Color.WHITE);
            chk.setBackground(Color.WHITE);
            chk.addActionListener(e -> stopCellEditing());
            panel.add(chk);
        }
        @Override
        public Component getTableCellEditorComponent(JTable t, Object v, boolean s, int r, int c) {
            chk.setSelected(Boolean.TRUE.equals(v));
            return panel;
        }
        @Override public Object getCellEditorValue() { return chk.isSelected(); }
    }

    private static class ObservacionRenderer implements TableCellRenderer {
        private final JPanel panel = new JPanel(new BorderLayout());
        private final JTextField txt = new JTextField();

        ObservacionRenderer() {
            panel.setBackground(Color.WHITE);
            panel.setBorder(new EmptyBorder(9, 10, 9, 14));
            txt.setBorder(BorderFactory.createCompoundBorder(
                    new LineBorder(EstilosUI.BORDE_GRIS, 1, true),
                    new EmptyBorder(0, 8, 0, 8)
            ));
            panel.add(txt, BorderLayout.CENTER);
        }

        @Override
        public Component getTableCellRendererComponent(JTable t, Object v, boolean s, boolean f, int r, int c) {
            txt.setText(v != null ? v.toString() : "");
            return panel;
        }
    }

    private static class ObservacionEditor extends AbstractCellEditor implements TableCellEditor {
        private final JPanel panel = new JPanel(new BorderLayout());
        private final JTextField txt = new JTextField();

        ObservacionEditor() {
            panel.setBackground(Color.WHITE);
            panel.setBorder(new EmptyBorder(9, 10, 9, 14));
            txt.setBorder(BorderFactory.createCompoundBorder(
                    new LineBorder(EstilosUI.AZUL_PRIMARIO, 1, true),
                    new EmptyBorder(0, 8, 0, 8)
            ));
            panel.add(txt, BorderLayout.CENTER);
        }

        @Override
        public Component getTableCellEditorComponent(JTable t, Object v, boolean s, int r, int c) {
            txt.setText(v != null ? v.toString() : "");
            return panel;
        }

        @Override
        public Object getCellEditorValue() {
            return txt.getText();
        }
    }
}
