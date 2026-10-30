package pe.edu.utp.pitagoras.view.docente;

import pe.edu.utp.pitagoras.config.EstilosUI;
import pe.edu.utp.pitagoras.controller.DocenteController;
import pe.edu.utp.pitagoras.model.CalificacionCompetencia;
import pe.edu.utp.pitagoras.model.Estudiante;
import pe.edu.utp.pitagoras.model.RegistroAsistencia;
import pe.edu.utp.pitagoras.model.ReglaCalificativo;
import pe.edu.utp.pitagoras.util.ExportadorExcelUtil;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.print.PrinterException;
import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Vista de Reportes del Rol Docente (RF18 / CU18 - Figura 6, pág. 29 y Tabla 4, pág. 31):
 * Presenta los 3 documentos oficiales de sección con los nombres completos de los estudiantes:
 * 1. Acta Consolidada de Calificaciones por Sección (CU18)
 * 2. Consolidado de Asistencia de la Sección (CU18)
 * 3. Nómina Oficial de Estudiantes Matriculados (CU18)
 */
public class PnlReportesDocente extends JPanel {
    private final DocenteController controller = DocenteController.getInstancia();
    private JComboBox<String> cboTipoReporte;
    private JComboBox<String> cboPeriodo;
    private JComboBox<String> cboSeccion;
    private JComboBox<String> cboArea;
    private JEditorPane visorVistaPrevia;

    public PnlReportesDocente() {
        setLayout(new BorderLayout(0, 16));
        setBackground(EstilosUI.FONDO_GRIS);
        setBorder(new EmptyBorder(26, 34, 26, 34));
        inicializarUI();
        actualizarVistaPrevia();
    }

    private void inicializarUI() {
        JPanel norte = new JPanel();
        norte.setBackground(EstilosUI.FONDO_GRIS);
        norte.setLayout(new BoxLayout(norte, BoxLayout.Y_AXIS));

        JLabel lblTitulo = new JLabel("Reportes académicos y vista previa (PDF)");
        lblTitulo.setFont(new Font("SansSerif", Font.BOLD, 24));
        lblTitulo.setForeground(EstilosUI.TEXTO_OSCURO);
        lblTitulo.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel lblSub = new JLabel("Emisión de actas de calificaciones, consolidado de asistencia y nómina oficial · "
                + controller.getNombreDocenteActivo());
        lblSub.setFont(new Font("SansSerif", Font.PLAIN, 13));
        lblSub.setForeground(EstilosUI.TEXTO_SECUNDARIO);
        lblSub.setAlignmentX(Component.LEFT_ALIGNMENT);

        norte.add(lblTitulo);
        norte.add(Box.createVerticalStrut(4));
        norte.add(lblSub);
        norte.add(Box.createVerticalStrut(14));

        // Barra de filtros y acciones (Solo los 3 documentos requeridos, grados 1.° A a 5.° A y cursos del docente)
        JPanel filtros = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        filtros.setBackground(EstilosUI.FONDO_GRIS);
        filtros.setAlignmentX(Component.LEFT_ALIGNMENT);

        cboTipoReporte = new JComboBox<>(new String[]{
                "Acta Consolidada de Calificaciones por Sección (CU18)",
                "Consolidado de Asistencia de la Sección (CU18)",
                "Nómina Oficial de Estudiantes Matriculados (CU18)"
        });
        cboTipoReporte.addActionListener(e -> actualizarVistaPrevia());

        cboPeriodo = new JComboBox<>(new String[]{"Bimestre 1", "Bimestre 2", "Bimestre 3", "Bimestre 4"});
        cboPeriodo.setSelectedItem("Bimestre 2");
        cboPeriodo.addActionListener(e -> actualizarVistaPrevia());

        cboSeccion = new JComboBox<>(controller.obtenerSeccionesDocenteActivo());
        cboSeccion.setSelectedItem("3.° A");
        cboSeccion.addActionListener(e -> actualizarVistaPrevia());

        cboArea = new JComboBox<>(controller.obtenerAreasDocenteActivo());
        if (cboArea.getItemCount() > 0) cboArea.setSelectedIndex(0);
        cboArea.addActionListener(e -> actualizarVistaPrevia());

        filtros.add(crearCampoFiltro("Tipo de Documento", cboTipoReporte, 320));
        filtros.add(Box.createHorizontalStrut(12));
        filtros.add(crearCampoFiltro("Periodo", cboPeriodo, 110));
        filtros.add(Box.createHorizontalStrut(12));
        filtros.add(crearCampoFiltro("Sección", cboSeccion, 85));
        filtros.add(Box.createHorizontalStrut(12));
        filtros.add(crearCampoFiltro("Área (Docente)", cboArea, 175));

        norte.add(filtros);
        add(norte, BorderLayout.NORTH);

        // Visor central de Vista Previa del documento oficial (RF18)
        visorVistaPrevia = new JEditorPane();
        visorVistaPrevia.setContentType("text/html");
        visorVistaPrevia.setEditable(false);
        visorVistaPrevia.setBackground(Color.WHITE);

        JScrollPane scroll = new JScrollPane(visorVistaPrevia);
        scroll.setBorder(new LineBorder(EstilosUI.BORDE_GRIS, 1));
        add(scroll, BorderLayout.CENTER);

        // Pie de página con botones de actualización, exportación Excel e impresión PDF nativa
        JPanel sur = new JPanel(new BorderLayout());
        sur.setBackground(EstilosUI.FONDO_GRIS);

        JLabel lblInfo = new JLabel("Escala oficial RVM 094-2020-MINEDU: AD = logro destacado | A = logro esperado | B = en proceso | C = en inicio.");
        lblInfo.setFont(new Font("SansSerif", Font.PLAIN, 12));
        lblInfo.setForeground(EstilosUI.TEXTO_SECUNDARIO);

        JPanel botones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        botones.setBackground(EstilosUI.FONDO_GRIS);

        JButton btnRefrescar = EstilosUI.crearBotonSecundario("Actualizar vista previa");
        btnRefrescar.addActionListener(e -> actualizarVistaPrevia());

        JButton btnExportarExcel = EstilosUI.crearBotonSecundario("Exportar Planilla Excel (.csv)");
        btnExportarExcel.addActionListener(e -> exportarPlanillaExcel());

        JButton btnImprimirPDF = EstilosUI.crearBotonPrimario("Imprimir / Guardar PDF (CU18)");
        btnImprimirPDF.addActionListener(e -> imprimirDocumentoPDF());

        botones.add(btnRefrescar);
        botones.add(btnExportarExcel);
        botones.add(btnImprimirPDF);

        sur.add(lblInfo, BorderLayout.WEST);
        sur.add(botones, BorderLayout.EAST);
        add(sur, BorderLayout.SOUTH);
    }

    private JPanel crearCampoFiltro(String etiqueta, JComboBox<String> combo, int ancho) {
        JPanel p = new JPanel(new BorderLayout(0, 4));
        p.setBackground(EstilosUI.FONDO_GRIS);
        p.add(new JLabel(etiqueta), BorderLayout.NORTH);
        combo.setPreferredSize(new Dimension(ancho, 36));
        combo.setBackground(Color.WHITE);
        p.add(combo, BorderLayout.CENTER);
        return p;
    }

    private void actualizarVistaPrevia() {
        int indice = cboTipoReporte.getSelectedIndex();
        String periodo = (String) cboPeriodo.getSelectedItem();
        String seccion = (String) cboSeccion.getSelectedItem();
        String area = (String) cboArea.getSelectedItem();
        String fechaActual = LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        List<Estudiante> alumnos = controller.obtenerEstudiantesPorSeccion(seccion);

        StringBuilder html = new StringBuilder();
        html.append("<html><body style='font-family:SansSerif; margin:24px; color:#141c26;'>");
        html.append("<div style='border-bottom:2px solid #173256; padding-bottom:10px; margin-bottom:16px;'>");
        html.append("<h2 style='margin:0; color:#173256;'>COLEGIO PARTICULAR PITÁGORAS — YANACANCHA, PASCO</h2>");
        html.append("<p style='margin:4px 0 0 0; font-size:11px; color:#6c757d;'>")
            .append("Sistema de Gestión Académica · Nivel Secundario · Fecha de emisión: ").append(fechaActual)
            .append(" · Docente: ").append(controller.getNombreDocenteActivo()).append("</p>");
        html.append("</div>");

        if (indice == 0) {
            // 1. Acta Consolidada de Calificaciones por Sección y Área (con Nombres Completos)
            String[] comp = controller.obtenerCompetenciasArea(area);
            List<CalificacionCompetencia> notas = controller.obtenerCalificaciones(periodo, seccion, area);

            html.append("<h3 style='margin:0 0 8px 0; color:#173256;'>ACTA CONSOLIDADA DE CALIFICACIONES — ")
                .append(periodo.toUpperCase()).append(" (SECCIÓN ").append(seccion).append(" · ÁREA: ").append(area.toUpperCase()).append(")</h3>");
            html.append("<table width='100%' cellspacing='0' cellpadding='7' style='border-collapse:collapse; font-size:11px;'>");
            html.append("<tr style='background-color:#173256; color:white;'>")
                .append("<th align='left' width='22%'>Apellidos y Nombres del Estudiante</th>");
            for (int i = 0; i < comp.length; i++) {
                html.append("<th align='center'>C").append(i + 1).append(": ").append(comp[i]).append("</th>");
            }
            html.append("<th align='center'>Logro del Área</th>")
                .append("<th align='center'>Calificativo Anual (RF14)</th>")
                .append("</tr>");

            for (CalificacionCompetencia c : notas) {
                String logroArea = ReglaCalificativo.calcularCalificativoArea(c.getCalificativos());
                String anual = ReglaCalificativo.determinarCalificativoAnual(logroArea, "Bimestre 4".equals(periodo));
                html.append("<tr style='border-bottom:1px solid #dee2e6;'>")
                    .append("<td><b>").append(c.getEstudianteNombre()).append("</b></td>");
                for (int i = 0; i < comp.length; i++) {
                    html.append("<td align='center'><b>").append(c.getCalificativo(i)).append("</b></td>");
                }
                html.append("<td align='center' style='color:#2470b0;'><b>").append(logroArea).append("</b></td>")
                    .append("<td align='center'>").append(anual).append("</td>")
                    .append("</tr>");
            }
            html.append("</table>");
            html.append("<p style='font-size:10px; color:#6c757d; margin-top:12px;'>")
                .append("Escala literal MINEDU: AD (Logro destacado), A (Logro esperado), B (En proceso), C (En inicio). ")
                .append("Conforme al RF14, el calificativo anual del área toma el calificativo del último bimestre sin promediar.</p>");

        } else if (indice == 1) {
            // 2. Consolidado de Asistencia de la Sección (con Nombres Completos)
            List<RegistroAsistencia> asistencias = controller.obtenerAsistencia(seccion, fechaActual);
            html.append("<h3 style='margin:0 0 8px 0; color:#173256;'>REPORTE CONSOLIDADO DE ASISTENCIA — SECCIÓN ")
                .append(seccion).append(" (FECHA: ").append(fechaActual).append(")</h3>");
            html.append("<table width='100%' cellspacing='0' cellpadding='7' style='border-collapse:collapse; font-size:11px;'>");
            html.append("<tr style='background-color:#173256; color:white;'>")
                .append("<th align='left'>Apellidos y Nombres del Estudiante</th>")
                .append("<th align='center'>Estado de Asistencia</th>")
                .append("<th align='center'>Falta Justificada</th>")
                .append("<th align='left'>Observación Registrada</th>")
                .append("</tr>");
            for (RegistroAsistencia r : asistencias) {
                html.append("<tr style='border-bottom:1px solid #dee2e6;'>")
                    .append("<td><b>").append(r.getEstudianteNombre()).append("</b></td>")
                    .append("<td align='center'><b>").append(r.getEstado()).append("</b></td>")
                    .append("<td align='center'>").append(r.isJustificada() ? "Sí (Justificada)" : "No").append("</td>")
                    .append("<td>").append(r.getObservacion().isEmpty() ? "-" : r.getObservacion()).append("</td>")
                    .append("</tr>");
            }
            html.append("</table>");

        } else {
            // 3. Nómina Oficial de Estudiantes Matriculados (con Nombres Completos)
            html.append("<h3 style='margin:0 0 8px 0; color:#173256;'>NÓMINA OFICIAL DE ESTUDIANTES MATRICULADOS — SECCIÓN ")
                .append(seccion).append(" (AÑO ESCOLAR 2026)</h3>");
            html.append("<table width='100%' cellspacing='0' cellpadding='7' style='border-collapse:collapse; font-size:11px;'>");
            html.append("<tr style='background-color:#173256; color:white;'>")
                .append("<th align='left'>Código</th>")
                .append("<th align='left'>DNI</th>")
                .append("<th align='left'>Apellidos y Nombres del Estudiante</th>")
                .append("<th align='left'>Apoderado Vinculado</th>")
                .append("<th align='left'>Teléfono</th>")
                .append("<th align='center'>Estado</th>")
                .append("</tr>");
            for (Estudiante e : alumnos) {
                html.append("<tr style='border-bottom:1px solid #dee2e6;'>")
                    .append("<td>").append(e.getCodigoModular()).append("</td>")
                    .append("<td>").append(e.getDni()).append("</td>")
                    .append("<td><b>").append(e.getNombresApellidos()).append("</b></td>")
                    .append("<td>").append(e.getApoderadoNombre()).append("</td>")
                    .append("<td>").append(e.getTelefonoApoderado()).append("</td>")
                    .append("<td align='center'>").append(e.getEstadoMatricula()).append("</td>")
                    .append("</tr>");
            }
            html.append("</table>");
        }

        html.append("</body></html>");
        visorVistaPrevia.setText(html.toString());
        visorVistaPrevia.setCaretPosition(0);
    }

    private void imprimirDocumentoPDF() {
        try {
            boolean impreso = visorVistaPrevia.print();
            if (impreso) {
                JOptionPane.showMessageDialog(this,
                        "Documento enviado correctamente a la impresora / generador PDF local (RF18 / CU18).",
                        "Reporte PDF Emitido", JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (PrinterException ex) {
            JOptionPane.showMessageDialog(this,
                    "No se pudo iniciar el servicio de impresión local: " + ex.getMessage(),
                    "Error de Impresión", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void exportarPlanillaExcel() {
        String periodo = (String) cboPeriodo.getSelectedItem();
        String seccion = (String) cboSeccion.getSelectedItem();
        String area = (String) cboArea.getSelectedItem();

        if (periodo == null || seccion == null || area == null) {
            // El docente aún no tiene carga académica asignada por el Director (RF06).
            visorVistaPrevia.setText("<html><body style='font-family:SansSerif; margin:24px; color:#6c757d;'>"
                    + "Aún no tienes carga académica asignada. Solicita a Dirección que te asigne "
                    + "grado, sección y área.</body></html>");
            return;
        }

        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Exportar Planilla a Microsoft Excel (.csv)");
        chooser.setSelectedFile(new File("Planilla_" + seccion.replace(".", "").replace("°", "").replace(" ", "_")
                + "_" + area.replace(" ", "_") + "_" + periodo.replace(" ", "") + ".csv"));

        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File archivo = chooser.getSelectedFile();
            try {
                if (cboTipoReporte.getSelectedIndex() == 1) {
                    String fecha = LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
                    ExportadorExcelUtil.exportarAsistenciaExcel(
                            archivo, seccion, fecha, controller.obtenerAsistencia(seccion, fecha));
                } else {
                    ExportadorExcelUtil.exportarPlanillaNotasExcel(
                            archivo, periodo, seccion, area,
                            controller.obtenerCompetenciasArea(area),
                            controller.obtenerCalificaciones(periodo, seccion, area));
                }
                JOptionPane.showMessageDialog(this,
                        "Archivo exportado exitosamente en:\n" + archivo.getAbsolutePath(),
                        "Exportación Exitosa", JOptionPane.INFORMATION_MESSAGE);
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this,
                        "Error al exportar archivo: " + ex.getMessage(),
                        "Error de Exportación", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}
