package pe.edu.utp.pitagoras.view.director;

import pe.edu.utp.pitagoras.config.EstilosUI;
import pe.edu.utp.pitagoras.temporal.EstadoSistema;
import pe.edu.utp.pitagoras.temporal.ExportadorXlsx;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.print.PrinterException;
import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;

/** CU18 / CU19 · RF18 / RF19: reportes en PDF (impresión) y exportación a Excel para el SIAGIE. */
public class PnlReportesSIAGIE extends JPanel {

    // ── Datos de ejemplo (3.° A) hasta conectar con la base de datos ──
    private record Alumno(String dni, String nombre, String[] notas, int asistencias, int faltas, int tardanzas) {
        /** Regla RF08: cualquier C = en riesgo. "Destacado" (supuesto): sin B ni C y al menos la mitad en AD. */
        String estado() {
            int ad = 0;
            for (String n : notas) {
                if (n.equals("C")) return "En Riesgo (C)";
                if (n.equals("AD")) ad++;
            }
            for (String n : notas) if (n.equals("B")) return "Regular";
            return ad * 2 >= notas.length ? "Destacado" : "Regular";
        }
        String porcentajeAsistencia() {
            int total = asistencias + faltas;
            return total == 0 ? "—" : String.format("%.1f %%", asistencias * 100.0 / total);
        }
    }

    private static final String[] AREAS = {"Matemática", "Comunicación", "Ciencia y Tec.", "C. Sociales", "DPCC", "Ed. Trabajo"};

    private static final Alumno[] ALUMNOS_3A = {
        new Alumno("74859612", "Agapito Perez, Mateo",    new String[]{"A",  "AD", "A",  "A",  "A",  "A"},  38, 0, 0),
        new Alumno("75123698", "Almerco Najera, Sofia",   new String[]{"AD", "A",  "AD", "A",  "AD", "AD"}, 37, 1, 0),
        new Alumno("76321458", "Balvin Chacchi, Diego",   new String[]{"B",  "C",  "B",  "B",  "B",  "A"},  34, 4, 2),
        new Alumno("71258963", "Paredes Quispe, Valeria", new String[]{"A",  "A",  "B",  "A",  "A",  "A"},  38, 0, 1),
        new Alumno("73698521", "Ureta Diaz, Sebastian",   new String[]{"AD", "AD", "A",  "AD", "A",  "AD"}, 38, 0, 0)
    };

    private static final String T_LIBRETA = "Libreta de Notas Bimestral";
    private static final String T_NOMINA  = "Nómina de Alumnos Inscritos";
    private static final String T_ASIST   = "Reporte Consolidado de Asistencia";
    private static final String T_SIAGIE  = "Plantilla Oficial para Carga SIAGIE";

    private JComboBox<String> cboTipoReporte, cboBimestreRep, cboGradoRep, cboSeccionRep;
    private JLabel lblSubPrevia;
    private DefaultTableModel modeloPrevia;
    private JTable tablaRep;

    public PnlReportesSIAGIE() {
        initComponents();
        EstadoSistema.addOyentePeriodos(() -> SwingUtilities.invokeLater(this::actualizarVista));
    }

    private void initComponents() {
        setLayout(null);
        setBackground(EstilosUI.BG_MAIN);
        setPreferredSize(new Dimension(980, 700));

        EstilosUI.encabezado(this, "Reportes Institucionales y Exportación SIAGIE",
                "Emisión de libretas de notas y nóminas en PDF · Exportación estructurada en Excel para SIAGIE (RF18, RF19)");

        // ── Filtros ───────────────────────────────────────────────
        JPanel cardFiltros = EstilosUI.card(32, 92, 916, 150);

        cardFiltros.add(EstilosUI.etiquetaCampo("TIPO DE DOCUMENTO", 20, 16, 180));
        cboTipoReporte = EstilosUI.combo(new String[]{T_LIBRETA, T_NOMINA, T_ASIST, T_SIAGIE});
        cboTipoReporte.setBounds(20, 36, 260, 36);
        cardFiltros.add(cboTipoReporte);

        cardFiltros.add(EstilosUI.etiquetaCampo("PERIODO", 300, 16, 120));
        cboBimestreRep = EstilosUI.combo(new String[]{"Bimestre 1", "Bimestre 2", "Bimestre 3", "Bimestre 4", "Anual 2026"});
        EstadoSistema.Periodo enCurso = EstadoSistema.periodoEnCurso();
        cboBimestreRep.setSelectedItem(enCurso != null ? enCurso.nombre : "Bimestre 1");
        cboBimestreRep.setBounds(300, 36, 150, 36);
        cardFiltros.add(cboBimestreRep);

        cardFiltros.add(EstilosUI.etiquetaCampo("GRADO", 470, 16, 100));
        cboGradoRep = EstilosUI.combo(new String[]{"1.° Secundaria", "2.° Secundaria", "3.° Secundaria", "4.° Secundaria", "5.° Secundaria"});
        cboGradoRep.setSelectedIndex(2);
        cboGradoRep.setBounds(470, 36, 150, 36);
        cardFiltros.add(cboGradoRep);

        cardFiltros.add(EstilosUI.etiquetaCampo("SECCIÓN", 640, 16, 100));
        cboSeccionRep = EstilosUI.combo(new String[]{"Sección A", "Sección B", "Todas"});
        cboSeccionRep.setBounds(640, 36, 120, 36);
        cardFiltros.add(cboSeccionRep);

        JButton btnFiltrar = EstilosUI.botonSecundario("Actualizar vista");
        btnFiltrar.setBounds(775, 36, 121, 36);
        btnFiltrar.addActionListener(e -> actualizarVista());
        cardFiltros.add(btnFiltrar);

        JButton btnGenerarPDF = EstilosUI.botonPrimario("Generar e Imprimir PDF");
        btnGenerarPDF.setBounds(20, 92, 230, 38);
        btnGenerarPDF.setToolTipText("Abre el diálogo de impresión. Elige «Microsoft Print to PDF» para guardar un archivo PDF.");
        btnGenerarPDF.addActionListener(e -> imprimirVistaPrevia());
        cardFiltros.add(btnGenerarPDF);

        JButton btnExportarSIAGIE = EstilosUI.botonPrimario("Exportar Excel para SIAGIE (.xlsx)", EstilosUI.SUCCESS_GREEN, EstilosUI.SUCCESS_HOVER);
        btnExportarSIAGIE.setBounds(265, 92, 260, 38);
        btnExportarSIAGIE.addActionListener(e -> exportarVistaPrevia());
        cardFiltros.add(btnExportarSIAGIE);

        JButton btnResumenApoderado = EstilosUI.botonSecundario("Generar Resumen Bimestral Apoderados");
        btnResumenApoderado.setBounds(540, 92, 320, 38);
        btnResumenApoderado.addActionListener(e -> exportarResumenApoderados());
        cardFiltros.add(btnResumenApoderado);

        add(cardFiltros);

        // ── Vista previa ──────────────────────────────────────────
        JPanel cardPrevia = EstilosUI.card(32, 258, 916, 392);
        cardPrevia.add(EstilosUI.label("Vista previa de datos consolidados", 20, 14, 876, 22, 15, Font.BOLD, EstilosUI.TEXT_DARK));
        lblSubPrevia = EstilosUI.label("", 20, 38, 876, 18, 12, Font.PLAIN, EstilosUI.TEXT_GRAY);
        cardPrevia.add(lblSubPrevia);

        modeloPrevia = new DefaultTableModel() {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        tablaRep = EstilosUI.tabla(modeloPrevia);
        cardPrevia.add(EstilosUI.scroll(tablaRep, 20, 66, 876, 304));
        add(cardPrevia);

        // Cualquier cambio de filtro refresca la vista (así la exportación siempre coincide con lo que se ve).
        java.awt.event.ActionListener refrescar = e -> actualizarVista();
        cboTipoReporte.addActionListener(refrescar);
        cboBimestreRep.addActionListener(refrescar);
        cboGradoRep.addActionListener(refrescar);
        cboSeccionRep.addActionListener(refrescar);
        actualizarVista();
    }

    // ── Vista previa ──────────────────────────────────────────────
    private void actualizarVista() {
        String tipo = (String) cboTipoReporte.getSelectedItem();
        String periodo = (String) cboBimestreRep.getSelectedItem();
        String grado = (String) cboGradoRep.getSelectedItem();
        String seccion = (String) cboSeccionRep.getSelectedItem();

        List<Alumno> alumnos = alumnosDeEjemplo(grado, seccion);
        String nota = motivoSinDatos(periodo, alumnos);
        if (nota != null) alumnos = List.of();

        List<Object[]> filas = new ArrayList<>();
        String[] cols = estructura(tipo, alumnos, grado, seccion, filas);
        modeloPrevia.setDataVector(filas.toArray(new Object[0][]), cols);
        aplicarAnchos(tipo);

        lblSubPrevia.setText(tipo + "  ·  " + periodo + "  ·  " + grado + "  ·  " + seccion
                + (nota != null ? "   —   " + nota : "   (datos de ejemplo)"));
        lblSubPrevia.setForeground(nota != null ? EstilosUI.WARNING_AMBER : EstilosUI.TEXT_GRAY);
    }

    /** Devuelve el motivo por el cual no hay datos, o null si sí los hay. */
    private String motivoSinDatos(String periodo, List<Alumno> alumnos) {
        if (periodo.startsWith("Anual")) {
            EstadoSistema.Periodo b4 = EstadoSistema.buscarPeriodo("Bimestre 4");
            if (b4 == null || !EstadoSistema.CERRADO.equals(b4.estado)) {
                return "el calificativo anual se determina al cerrar el Bimestre 4";
            }
        } else {
            EstadoSistema.Periodo p = EstadoSistema.buscarPeriodo(periodo);
            if (p != null && EstadoSistema.PENDIENTE.equals(p.estado)) return periodo + " aún no tiene registros";
        }
        return alumnos.isEmpty() ? "sin registros de ejemplo para este grado/sección" : null;
    }

    private List<Alumno> alumnosDeEjemplo(String grado, String seccion) {
        boolean hay = grado.startsWith("3.°") && !seccion.equals("Sección B");
        return hay ? List.of(ALUMNOS_3A) : List.of();
    }

    /** Define columnas y filas según el tipo de documento. */
    private String[] estructura(String tipo, List<Alumno> alumnos, String grado, String seccion, List<Object[]> filas) {
        String gradoCorto = grado.substring(0, 3);
        String secCorta = seccion.equals("Sección B") ? "B" : "A";
        int n = 1;
        switch (tipo) {
            case T_NOMINA -> {
                for (Alumno a : alumnos) filas.add(new Object[]{ n++, a.dni(), a.nombre(), gradoCorto, secCorta, "Matriculado" });
                return new String[]{"N.°", "DNI", "Estudiante", "Grado", "Sección", "Condición"};
            }
            case T_ASIST -> {
                for (Alumno a : alumnos) filas.add(new Object[]{ a.dni(), a.nombre(), a.asistencias(), a.faltas(), a.tardanzas(), a.porcentajeAsistencia() });
                return new String[]{"DNI", "Estudiante", "Asistencias", "Faltas", "Tardanzas", "% Asistencia"};
            }
            case T_SIAGIE -> {
                for (Alumno a : alumnos) {
                    Object[] f = new Object[4 + AREAS.length];
                    f[0] = a.dni(); f[1] = a.nombre(); f[2] = gradoCorto; f[3] = secCorta;
                    System.arraycopy(a.notas(), 0, f, 4, AREAS.length);
                    filas.add(f);
                }
                return concat(new String[]{"DNI", "Estudiante", "Grado", "Sección"}, AREAS);
            }
            default -> {   // Libreta
                for (Alumno a : alumnos) {
                    Object[] f = new Object[3 + AREAS.length];
                    f[0] = a.dni(); f[1] = a.nombre();
                    System.arraycopy(a.notas(), 0, f, 2, AREAS.length);
                    f[2 + AREAS.length] = a.estado();
                    filas.add(f);
                }
                return concat(concat(new String[]{"DNI", "Estudiante"}, AREAS), new String[]{"Estado"});
            }
        }
    }

    private void aplicarAnchos(String tipo) {
        switch (tipo) {
            case T_NOMINA -> { EstilosUI.anchos(tablaRep, 50, 100, 280, 80, 80, 120); EstilosUI.centrar(tablaRep, 0, 1, 3, 4); }
            case T_ASIST  -> { EstilosUI.anchos(tablaRep, 100, 280, 100, 80, 100, 110); EstilosUI.centrar(tablaRep, 0, 2, 3, 4, 5); }
            case T_SIAGIE -> { EstilosUI.anchos(tablaRep, 80, 180, 55, 65, 95, 110, 105, 95, 60, 90); EstilosUI.centrar(tablaRep, 0, 2, 3, 4, 5, 6, 7, 8, 9); }
            default       -> { EstilosUI.anchos(tablaRep, 80, 190, 95, 110, 105, 95, 60, 90, 110); EstilosUI.centrar(tablaRep, 0, 2, 3, 4, 5, 6, 7); }
        }
    }

    // ── Acciones de salida ────────────────────────────────────────
    /** PDF vía impresión del JTable (sin dependencias): el usuario elige impresora o «Microsoft Print to PDF». */
    private void imprimirVistaPrevia() {
        if (modeloPrevia.getRowCount() == 0) {
            EstilosUI.aviso(this, "Sin datos", "La vista previa no tiene registros para imprimir. Ajusta los filtros.");
            return;
        }
        String titulo = cboTipoReporte.getSelectedItem() + " — " + cboBimestreRep.getSelectedItem()
                + " — " + cboGradoRep.getSelectedItem() + " — " + cboSeccionRep.getSelectedItem();
        try {
            boolean impreso = tablaRep.print(JTable.PrintMode.FIT_WIDTH,
                    new MessageFormat(titulo.replace("'", "''")), new MessageFormat("Colegio Pitágoras · Página {0}"));
            if (impreso) EstilosUI.info(this, "Impresión", "El documento se envió a la impresora / archivo PDF seleccionado.");
        } catch (PrinterException ex) {
            EstilosUI.error(this, "Error de impresión", "No se pudo imprimir el documento:\n" + ex.getMessage());
        }
    }

    private void exportarVistaPrevia() {
        if (modeloPrevia.getRowCount() == 0) {
            EstilosUI.aviso(this, "Sin datos", "La vista previa no tiene registros para exportar. Ajusta los filtros.");
            return;
        }
        String base = nombreSeguro(cboTipoReporte.getSelectedItem() + "_" + cboBimestreRep.getSelectedItem()
                + "_" + cboGradoRep.getSelectedItem().toString().substring(0, 2));
        guardarXlsx(base, (String) cboTipoReporte.getSelectedItem(), modeloPrevia);
    }

    /** RF19: resumen por estudiante (calificativos + asistencia) para entregar al apoderado al cierre del bimestre. */
    private void exportarResumenApoderados() {
        String periodo = (String) cboBimestreRep.getSelectedItem();
        if (periodo.startsWith("Anual")) {
            EstilosUI.aviso(this, "Periodo no válido", "El resumen para apoderados se genera por bimestre. Selecciona un bimestre.");
            return;
        }
        List<Alumno> alumnos = alumnosDeEjemplo((String) cboGradoRep.getSelectedItem(), (String) cboSeccionRep.getSelectedItem());
        String motivo = motivoSinDatos(periodo, alumnos);
        if (motivo != null) {
            EstilosUI.aviso(this, "Sin datos", "No se puede generar el resumen: " + motivo + ".");
            return;
        }

        String sufijo = "";
        EstadoSistema.Periodo p = EstadoSistema.buscarPeriodo(periodo);
        if (p != null && !EstadoSistema.CERRADO.equals(p.estado)) {
            int r = JOptionPane.showConfirmDialog(this,
                    "El " + periodo + " aún está abierto: las notas pueden cambiar.\n"
                    + "El resumen oficial se genera al cerrar el bimestre.\n\n¿Generar un resumen PROVISIONAL?",
                    "Periodo abierto", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (r != JOptionPane.YES_OPTION) return;
            sufijo = "_PROVISIONAL";
        }

        String[] cols = concat(concat(new String[]{"DNI", "Estudiante"}, AREAS),
                new String[]{"Asistencias", "Faltas", "Tardanzas", "Estado"});
        DefaultTableModel resumen = new DefaultTableModel(cols, 0);
        for (Alumno a : alumnos) {
            Object[] f = new Object[cols.length];
            f[0] = a.dni(); f[1] = a.nombre();
            System.arraycopy(a.notas(), 0, f, 2, AREAS.length);
            f[2 + AREAS.length] = a.asistencias(); f[3 + AREAS.length] = a.faltas();
            f[4 + AREAS.length] = a.tardanzas();   f[5 + AREAS.length] = a.estado();
            resumen.addRow(f);
        }
        String base = nombreSeguro("Resumen_" + periodo + "_" + cboGradoRep.getSelectedItem().toString().substring(0, 2)
                + "_Apoderados" + sufijo);
        guardarXlsx(base, "Resumen " + periodo, resumen);
    }

    private void guardarXlsx(String nombreBase, String hoja, DefaultTableModel modelo) {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Guardar archivo de Excel");
        chooser.setFileFilter(new FileNameExtensionFilter("Libro de Excel (*.xlsx)", "xlsx"));
        chooser.setSelectedFile(new File(nombreBase + ".xlsx"));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;

        File destino = chooser.getSelectedFile();
        if (!destino.getName().toLowerCase().endsWith(".xlsx")) {
            destino = new File(destino.getParentFile(), destino.getName() + ".xlsx");
        }
        if (destino.exists()) {
            int r = JOptionPane.showConfirmDialog(this, "El archivo ya existe:\n" + destino.getName() + "\n¿Deseas reemplazarlo?",
                    "Confirmar reemplazo", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (r != JOptionPane.YES_OPTION) return;
        }
        try {
            ExportadorXlsx.escribir(destino, hoja, modelo);
            EstilosUI.info(this, "Exportación completada",
                    "Documento exportado exitosamente en:\n" + destino.getAbsolutePath()
                    + "\n(" + modelo.getRowCount() + " registros)");
        } catch (IOException ex) {
            EstilosUI.error(this, "Error al exportar",
                    "No se pudo guardar el archivo (¿está abierto en Excel?):\n" + ex.getMessage());
        }
    }

    // ── Utilidades ────────────────────────────────────────────────
    private static String nombreSeguro(String s) {
        String sinTildes = Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return sinTildes.replaceAll("[^A-Za-z0-9_\\-]+", "_");
    }

    private static String[] concat(String[] a, String[] b) {
        String[] r = new String[a.length + b.length];
        System.arraycopy(a, 0, r, 0, a.length);
        System.arraycopy(b, 0, r, a.length, b.length);
        return r;
    }
}
