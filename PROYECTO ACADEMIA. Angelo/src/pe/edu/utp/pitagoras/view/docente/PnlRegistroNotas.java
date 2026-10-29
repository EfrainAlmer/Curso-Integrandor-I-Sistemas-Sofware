package pe.edu.utp.pitagoras.view.docente;

import pe.edu.utp.pitagoras.config.EstilosUI;
import pe.edu.utp.pitagoras.controller.DocenteController;
import pe.edu.utp.pitagoras.model.CalificacionCompetencia;
import pe.edu.utp.pitagoras.model.ReglaCalificativo;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableColumnModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Prototipo Figura 11 (pág. 34) — Registro de calificaciones (RF13, RF14 / CU13, CU14).
 * <p>
 * Adaptado al Informe de Progreso del Aprendizaje del Estudiante 2026 (SIAGIE / MINEDU):
 * <ul>
 *   <li>Filtra automáticamente las áreas que enseña el docente autenticado:
 *       {@code docente} (Matemática y Ciencia y Tecnología) vs {@code docente2} (Comunicación y Ciencias Sociales).</li>
 *   <li>Incluye el grado {@code 5.° A}.</li>
 *   <li>Ajusta las columnas a las competencias oficiales del área seleccionada (3 o 4 competencias según CNEB)
 *       y permite registrar las <b>Conclusiones Descriptivas por Periodo</b> (RVM 094-2020-MINEDU).</li>
 * </ul>
 */
public class PnlRegistroNotas extends JPanel {
    private final DocenteController controller = DocenteController.getInstancia();
    private JLabel lblAvisoCambios;
    private NotasTableModel model;
    private JTable tabla;
    private JComboBox<String> cboPeriodo;
    private JComboBox<String> cboSeccion;
    private JComboBox<String> cboArea;
    private JButton btnConclusiones;
    private JButton btnDescartar;
    private JButton btnGuardar;
    private boolean hayCambiosPendientes = false;

    public PnlRegistroNotas() {
        setLayout(new BorderLayout(0, 16));
        setBackground(EstilosUI.FONDO_GRIS);
        setBorder(new EmptyBorder(28, 36, 28, 36));
        inicializarUI();
        cargarGrillaSeleccionada();
    }

    private void inicializarUI() {
        JPanel norte = new JPanel();
        norte.setBackground(EstilosUI.FONDO_GRIS);
        norte.setLayout(new BoxLayout(norte, BoxLayout.Y_AXIS));

        JLabel lblTitulo = new JLabel("Registro de calificaciones");
        lblTitulo.setFont(new Font("SansSerif", Font.BOLD, 24));
        lblTitulo.setForeground(EstilosUI.TEXTO_OSCURO);
        lblTitulo.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel lblSub = new JLabel("Un calificativo (AD, A, B, C) por estudiante, competencia y periodo · Docente: "
                + controller.getNombreDocenteActivo());
        lblSub.setFont(new Font("SansSerif", Font.PLAIN, 13));
        lblSub.setForeground(EstilosUI.TEXTO_SECUNDARIO);
        lblSub.setAlignmentX(Component.LEFT_ALIGNMENT);

        norte.add(lblTitulo);
        norte.add(Box.createVerticalStrut(4));
        norte.add(lblSub);
        norte.add(Box.createVerticalStrut(16));

        // Filtros Periodo, Sección (1.° A a 5.° A) y Área (solo los cursos que enseña el docente activo)
        JPanel filtros = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        filtros.setBackground(EstilosUI.FONDO_GRIS);
        filtros.setAlignmentX(Component.LEFT_ALIGNMENT);

        cboPeriodo = new JComboBox<>(new String[]{"Bimestre 1", "Bimestre 2", "Bimestre 3", "Bimestre 4"});
        cboPeriodo.setSelectedItem("Bimestre 2");
        cboPeriodo.addActionListener(e -> cargarGrillaSeleccionada());

        cboSeccion = new JComboBox<>(new String[]{"1.° A", "2.° A", "3.° A", "4.° A", "5.° A"});
        cboSeccion.setSelectedItem("3.° A");
        cboSeccion.addActionListener(e -> cargarGrillaSeleccionada());

        String[] areasDelDocente = controller.obtenerAreasDocenteActivo();
        cboArea = new JComboBox<>(areasDelDocente);
        cboArea.setSelectedIndex(0);
        cboArea.addActionListener(e -> cargarGrillaSeleccionada());

        filtros.add(envolverFiltro("Periodo", cboPeriodo, 115));
        filtros.add(Box.createHorizontalStrut(14));
        filtros.add(envolverFiltro("Sección", cboSeccion, 85));
        filtros.add(Box.createHorizontalStrut(14));
        filtros.add(envolverFiltro("Área (Asignada al Docente)", cboArea, 195));

        norte.add(filtros);
        add(norte, BorderLayout.NORTH);

        // Tabla de Competencias con selectores AD, A, B, C (Figura 11)
        lblAvisoCambios = new JLabel("Hay cambios sin guardar");
        lblAvisoCambios.setFont(new Font("SansSerif", Font.PLAIN, 13));
        lblAvisoCambios.setForeground(EstilosUI.ALERTA_DORADA);

        model = new NotasTableModel(() -> {
            hayCambiosPendientes = true;
            lblAvisoCambios.setForeground(EstilosUI.ALERTA_DORADA);
            lblAvisoCambios.setText("Hay cambios sin guardar");
        });

        tabla = new JTable(model);
        EstilosUI.estilizarTabla(tabla);
        tabla.setRowHeight(54);

        JScrollPane scroll = new JScrollPane(tabla);
        scroll.setBorder(new LineBorder(EstilosUI.BORDE_GRIS, 1));
        scroll.getViewport().setBackground(Color.WHITE);
        add(scroll, BorderLayout.CENTER);

        // Pie con aviso y botones Conclusiones Descriptivas / Descartar / Guardar calificativos
        JPanel sur = new JPanel(new BorderLayout());
        sur.setBackground(EstilosUI.FONDO_GRIS);

        JPanel botones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        botones.setBackground(EstilosUI.FONDO_GRIS);

        btnConclusiones = EstilosUI.crearBotonSecundario("Conclusiones descriptivas (SIAGIE)");
        btnConclusiones.addActionListener(e -> editarConclusionDescriptiva());

        btnDescartar = EstilosUI.crearBotonSecundario("Descartar");
        btnDescartar.addActionListener(e -> {
            if (tabla.isEditing()) {
                tabla.getCellEditor().stopCellEditing();
            }
            cargarGrillaSeleccionada();
            hayCambiosPendientes = false;
            lblAvisoCambios.setText("Cambios descartados.");
        });

        btnGuardar = EstilosUI.crearBotonPrimario("Guardar calificativos");
        btnGuardar.addActionListener(e -> guardarCalificaciones());

        botones.add(btnConclusiones);
        botones.add(btnDescartar);
        botones.add(btnGuardar);

        sur.add(lblAvisoCambios, BorderLayout.WEST);
        sur.add(botones, BorderLayout.EAST);
        add(sur, BorderLayout.SOUTH);
    }

    private JPanel envolverFiltro(String etiqueta, JComboBox<String> cbo, int ancho) {
        JPanel p = new JPanel(new BorderLayout(0, 4));
        p.setBackground(EstilosUI.FONDO_GRIS);
        p.add(new JLabel(etiqueta), BorderLayout.NORTH);
        cbo.setPreferredSize(new Dimension(ancho, 36));
        cbo.setBackground(Color.WHITE);
        p.add(cbo, BorderLayout.CENTER);
        return p;
    }

    private void configurarRenderersEscala() {
        String[] escala = {"AD", "A", "B", "C"};
        TableColumnModel cm = tabla.getColumnModel();
        cm.getColumn(0).setPreferredWidth(220);
        for (int i = 1; i < cm.getColumnCount(); i++) {
            cm.getColumn(i).setCellRenderer(new PnlRegistroAsistencia.ComboRenderer(escala));
            cm.getColumn(i).setCellEditor(new DefaultCellEditor(new JComboBox<>(escala)));
        }
    }

    private void cargarGrillaSeleccionada() {
        if (tabla != null && tabla.isEditing()) {
            tabla.getCellEditor().stopCellEditing();
        }
        String periodo = (String) cboPeriodo.getSelectedItem();
        String seccion = (String) cboSeccion.getSelectedItem();
        String area = (String) cboArea.getSelectedItem();

        boolean abierto = controller.esPeriodoAbierto(periodo);
        String[] competencias = controller.obtenerCompetenciasArea(area);
        List<CalificacionCompetencia> filas = controller.obtenerCalificaciones(periodo, seccion, area);

        model.actualizarDatos(competencias, filas, abierto);
        configurarRenderersEscala();

        if (btnGuardar != null && btnDescartar != null) {
            btnGuardar.setEnabled(abierto);
            btnDescartar.setEnabled(abierto);
        }

        if (!abierto) {
            lblAvisoCambios.setForeground(EstilosUI.COLOR_C);
            lblAvisoCambios.setText(periodo + " cerrado por Dirección (RF07): consulta en modo solo lectura.");
        } else {
            lblAvisoCambios.setForeground(EstilosUI.ALERTA_DORADA);
            lblAvisoCambios.setText("Hay cambios sin guardar");
        }
        hayCambiosPendientes = false;
    }

    private void editarConclusionDescriptiva() {
        int fila = tabla.getSelectedRow();
        if (fila < 0) {
            fila = 0;
        }
        List<CalificacionCompetencia> filas = model.getFilas();
        if (filas.isEmpty()) {
            return;
        }
        CalificacionCompetencia actual = filas.get(fila);
        String logro = ReglaCalificativo.calcularCalificativoArea(actual.getCalificativos());

        String nueva = (String) JOptionPane.showInputDialog(
                this,
                "Estudiante: " + actual.getEstudianteNombre() + " (" + actual.getSeccion() + ")\n"
                + "Área: " + actual.getArea() + " · " + actual.getBimestre() + " · Nivel de logro: " + logro + "\n\n"
                + "Conclusión descriptiva (logros, dificultades y recomendaciones - RVM 094-2020-MINEDU):",
                "Conclusiones Descriptivas por Periodo (SIAGIE)",
                JOptionPane.PLAIN_MESSAGE,
                null,
                null,
                actual.getConclusionDescriptiva()
        );

        if (nueva != null) {
            actual.setConclusionDescriptiva(nueva.trim());
            hayCambiosPendientes = true;
            lblAvisoCambios.setForeground(EstilosUI.ALERTA_DORADA);
            lblAvisoCambios.setText("Conclusión descriptiva actualizada para " + actual.getEstudianteNombre() + " (Pendiente de guardar)");
        }
    }

    private void guardarCalificaciones() {
        if (tabla.isEditing()) {
            tabla.getCellEditor().stopCellEditing();
        }
        String periodo = (String) cboPeriodo.getSelectedItem();
        String seccion = (String) cboSeccion.getSelectedItem();
        String area = (String) cboArea.getSelectedItem();

        if (!controller.esPeriodoAbierto(periodo)) {
            JOptionPane.showMessageDialog(this,
                    "El " + periodo + " se encuentra cerrado (RF07). No se permiten modificaciones.",
                    "Periodo Cerrado", JOptionPane.WARNING_MESSAGE);
            return;
        }

        List<CalificacionCompetencia> filas = model.getFilas();
        int cambiosAuditados = controller.guardarCalificaciones(periodo, seccion, area, filas);
        hayCambiosPendientes = false;

        StringBuilder resumenAnual = new StringBuilder();
        if ("Bimestre 4".equals(periodo)) {
            resumenAnual.append("\n\nNivel de logro al finalizar el periodo lectivo (RF14 / CU14 - Último Bimestre):\n");
            for (CalificacionCompetencia c : filas) {
                String logroArea = ReglaCalificativo.calcularCalificativoArea(c.getCalificativos());
                String anual = ReglaCalificativo.determinarCalificativoAnual(logroArea, true);
                resumenAnual.append(" • ").append(c.getEstudianteNombre()).append(": ").append(anual)
                        .append(" — ").append(ReglaCalificativo.obtenerDescripcionEscala(logroArea)).append("\n");
            }
        }

        lblAvisoCambios.setForeground(EstilosUI.COLOR_AD);
        lblAvisoCambios.setText("Calificativos guardados (" + seccion + " · " + area + " · " + periodo + ") — Cambios auditados: " + cambiosAuditados);

        JOptionPane.showMessageDialog(this,
                "Calificativos guardados en escala literal MINEDU (AD, A, B, C) [RF13 / CU13].\n"
                + "• Área curricular: " + area + " (" + seccion + ")\n"
                + "• Modificaciones registradas en Bitácora de Auditoría (RF09): " + cambiosAuditados
                + "\n• Regla RF14 / RVM 094-2020-MINEDU: El nivel de logro al finalizar el periodo lectivo corresponde al último bimestre."
                + resumenAnual,
                "Evaluación Guardada (CU13)", JOptionPane.INFORMATION_MESSAGE);
    }

    private static class NotasTableModel extends AbstractTableModel {
        private String[] cols = {"Estudiante", "Competencia 1", "Competencia 2", "Competencia 3"};
        private List<CalificacionCompetencia> filas = new ArrayList<>();
        private boolean editable = true;
        private final Runnable onChange;

        NotasTableModel(Runnable onChange) {
            this.onChange = onChange;
        }

        void actualizarDatos(String[] competencias, List<CalificacionCompetencia> nuevasFilas, boolean periodoAbierto) {
            this.cols = new String[competencias.length + 1];
            this.cols[0] = "Estudiante";
            System.arraycopy(competencias, 0, this.cols, 1, competencias.length);
            this.filas = new ArrayList<>(nuevasFilas);
            this.editable = periodoAbierto;
            fireTableStructureChanged();
        }

        List<CalificacionCompetencia> getFilas() {
            return filas;
        }

        @Override public int getRowCount() { return filas.size(); }
        @Override public int getColumnCount() { return cols.length; }
        @Override public String getColumnName(int c) { return cols[c]; }
        @Override public boolean isCellEditable(int r, int c) { return editable && c >= 1; }

        @Override
        public Object getValueAt(int r, int c) {
            CalificacionCompetencia fila = filas.get(r);
            if (c == 0) {
                return "   " + fila.getEstudianteNombre();
            }
            return fila.getCalificativo(c - 1);
        }

        @Override
        public void setValueAt(Object val, int r, int c) {
            if (c >= 1 && val != null) {
                String nuevo = val.toString();
                if (ReglaCalificativo.esCalificativoValido(nuevo)) {
                    filas.get(r).setCalificativo(c - 1, nuevo);
                    fireTableCellUpdated(r, c);
                    onChange.run();
                }
            }
        }
    }
}
