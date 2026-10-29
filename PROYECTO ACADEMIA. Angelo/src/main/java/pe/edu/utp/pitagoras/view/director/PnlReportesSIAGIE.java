package pe.edu.utp.pitagoras.view.director;

import pe.edu.utp.pitagoras.config.EstilosUI;
import pe.edu.utp.pitagoras.model.AreaCurricular;
import pe.edu.utp.pitagoras.model.CurriculoSecundaria;
import pe.edu.utp.pitagoras.model.ReglaCalificativo;
import pe.edu.utp.pitagoras.temporal.EstadoSistema;
import pe.edu.utp.pitagoras.temporal.ExportadorXlsx;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.print.PrinterException;
import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * CU18 / CU19 · RF18 / RF19: reportes en PDF (impresión) y exportación a Excel para el SIAGIE.
 * La libreta sigue el Informe de Progreso del Aprendizaje: cada área con sus competencias, el
 * calificativo del bimestre y la nota del área (promedio de sus competencias en la escala AD/A/B/C).
 * Turno único: {@link CurriculoSecundaria#TURNO}.
 */
public class PnlReportesSIAGIE extends JPanel {

    private static final String[] LITERAL = {"", "C", "B", "A", "AD"};   // índice = puntaje 1..4
    private static final List<AreaCurricular> AREAS = CurriculoSecundaria.AREAS;
    private static final String ESCALA = "Escala: AD = logro destacado  |  A = logro esperado  |  B = en proceso  |  C = en inicio.";

    // ── Datos de ejemplo (3.° A) hasta conectar con la base de datos ──
    /**
     * @param base    nivel típico del alumno (1 = C … 4 = AD)
     * @param semilla variación determinista de ejemplo entre competencias
     */
    private record Alumno(String dni, String nombre, int base, int semilla, int asistencias, int faltas, int tardanzas) {

        /** Calificativos de las 28 competencias en el bimestre dado (1..4), en el orden del currículo. */
        String[] competencias(int bimestre) {
            String[] r = new String[CurriculoSecundaria.totalCompetencias()];
            for (int i = 0; i < r.length; i++) {
                int d = switch ((i * 5 + bimestre * 2 + semilla) % 7) { case 0 -> -1; case 6 -> 1; default -> 0; };
                r[i] = LITERAL[Math.max(1, Math.min(4, base + d))];
            }
            return r;
        }

        /** Nota de cada área = promedio de sus competencias (ReglaCalificativo, escala MINEDU). */
        String[] notasAreas(int bimestre) {
            return PnlReportesSIAGIE.calcularNotasAreas(competencias(bimestre));
        }

        /** Regla RF08: cualquier C = en riesgo. "Destacado" (supuesto): sin B ni C y al menos la mitad en AD. */
        String estado(int bimestre) {
            String[] notas = notasAreas(bimestre);
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

    private static final Alumno[] ALUMNOS_3A = {
        new Alumno("74859612", "Agapito Perez, Mateo",    3, 0, 38, 0, 0),
        new Alumno("75123698", "Almerco Najera, Sofia",   4, 1, 37, 1, 0),
        new Alumno("76321458", "Balvin Chacchi, Diego",   2, 2, 34, 4, 2),
        new Alumno("71258963", "Paredes Quispe, Valeria", 3, 3, 38, 0, 1),
        new Alumno("73698521", "Ureta Diaz, Sebastian",   4, 4, 38, 0, 0)
    };

    private static final String T_LIBRETA = "Libreta de Notas Bimestral";
    private static final String T_NOMINA  = "Nómina de Alumnos Inscritos";
    private static final String T_ASIST   = "Reporte Consolidado de Asistencia";
    private static final String T_SIAGIE  = "Plantilla Oficial para Carga SIAGIE";

    private JComboBox<String> cboTipoReporte, cboBimestreRep, cboGradoRep, cboSeccionRep, cboEstudianteRep;
    private JLabel lblSubPrevia, lblEstudiante, lblLeyenda;
    private DefaultTableModel modeloPrevia;
    private JTable tablaRep;
    private Alumno alumnoVista;          // alumno mostrado en la libreta
    private boolean cargando;            // evita refrescos en cascada al repoblar el combo de estudiantes

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

        // Selector de estudiante: solo visible en la libreta (que es individual).
        lblEstudiante = EstilosUI.etiquetaCampo("ESTUDIANTE", 586, 14, 90);
        cboEstudianteRep = EstilosUI.combo(new String[]{});
        cboEstudianteRep.setBounds(676, 8, 220, 28);
        cardPrevia.add(lblEstudiante);
        cardPrevia.add(cboEstudianteRep);

        cardPrevia.add(EstilosUI.label("Vista previa de datos consolidados", 20, 14, 540, 22, 15, Font.BOLD, EstilosUI.TEXT_DARK));
        lblSubPrevia = EstilosUI.label("", 20, 38, 876, 18, 12, Font.PLAIN, EstilosUI.TEXT_GRAY);
        cardPrevia.add(lblSubPrevia);

        modeloPrevia = new DefaultTableModel() {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        tablaRep = EstilosUI.tabla(modeloPrevia);
        cardPrevia.add(EstilosUI.scroll(tablaRep, 20, 66, 876, 302));

        lblLeyenda = EstilosUI.label("", 20, 372, 876, 16, 11, Font.PLAIN, EstilosUI.TEXT_GRAY);
        cardPrevia.add(lblLeyenda);
        add(cardPrevia);

        // Cualquier cambio de filtro refresca la vista (así la exportación siempre coincide con lo que se ve).
        java.awt.event.ActionListener refrescar = e -> actualizarVista();
        cboTipoReporte.addActionListener(refrescar);
        cboBimestreRep.addActionListener(refrescar);
        cboGradoRep.addActionListener(refrescar);
        cboSeccionRep.addActionListener(refrescar);
        cboEstudianteRep.addActionListener(refrescar);
        actualizarVista();
    }

    // ── Vista previa ──────────────────────────────────────────────
    private void actualizarVista() {
        if (cargando) return;
        String tipo = (String) cboTipoReporte.getSelectedItem();
        String periodo = (String) cboBimestreRep.getSelectedItem();
        String grado = (String) cboGradoRep.getSelectedItem();
        String seccion = (String) cboSeccionRep.getSelectedItem();
        boolean esLibreta = T_LIBRETA.equals(tipo);

        List<Alumno> alumnos = alumnosDeEjemplo(grado, seccion);
        String nota = motivoSinDatos(periodo, alumnos);
        if (nota != null) alumnos = List.of();

        lblEstudiante.setVisible(esLibreta);
        cboEstudianteRep.setVisible(esLibreta);
        alumnoVista = esLibreta ? sincronizarEstudiantes(alumnos) : null;

        List<Object[]> filas = new ArrayList<>();
        String[] cols = estructura(tipo, alumnos, grado, seccion, periodo, alumnoVista, filas);
        modeloPrevia.setDataVector(filas.toArray(new Object[0][]), cols);
        aplicarAnchos(tipo, numeroBimestre(periodo) == 0);

        String detalle = (esLibreta && alumnoVista != null)
                ? periodo + "  ·  " + alumnoVista.nombre() + " (DNI " + alumnoVista.dni() + ")  ·  "
                  + grado.substring(0, 3) + " " + letraSeccion(seccion)
                : periodo + "  ·  " + grado + "  ·  " + seccion;
        lblSubPrevia.setText(tipo + "  ·  " + detalle + "  ·  Turno " + CurriculoSecundaria.TURNO
                + (nota != null ? "   —   " + nota : "   (datos de ejemplo)"));
        lblSubPrevia.setForeground(nota != null ? EstilosUI.WARNING_AMBER : EstilosUI.TEXT_GRAY);

        lblLeyenda.setText(!esLibreta || alumnoVista == null ? ""
                : ESCALA + (numeroBimestre(periodo) == 0
                    ? "  La nota final del área es el promedio de sus competencias al último bimestre (RF14)."
                    : "  La nota del área es el promedio de sus competencias."));
    }

    /** Repuebla el combo de estudiantes (solo si cambió la lista) y devuelve el alumno elegido, o null si no hay. */
    private Alumno sincronizarEstudiantes(List<Alumno> alumnos) {
        boolean igual = cboEstudianteRep.getItemCount() == alumnos.size();
        for (int i = 0; igual && i < alumnos.size(); i++) {
            igual = alumnos.get(i).nombre().equals(cboEstudianteRep.getItemAt(i));
        }
        if (!igual) {
            cargando = true;
            try {
                cboEstudianteRep.removeAllItems();
                for (Alumno a : alumnos) cboEstudianteRep.addItem(a.nombre());
            } finally {
                cargando = false;
            }
        }
        int idx = cboEstudianteRep.getSelectedIndex();
        return (idx >= 0 && idx < alumnos.size()) ? alumnos.get(idx) : null;
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
    private String[] estructura(String tipo, List<Alumno> alumnos, String grado, String seccion,
                                String periodo, Alumno elegido, List<Object[]> filas) {
        String gradoCorto = grado.substring(0, 3);
        String secCorta = letraSeccion(seccion);
        int bim = numeroBimestre(periodo);
        int n = 1;
        switch (tipo) {
            case T_NOMINA -> {
                for (Alumno a : alumnos) filas.add(new Object[]{ n++, a.dni(), a.nombre(), gradoCorto, secCorta, CurriculoSecundaria.TURNO, "Matriculado" });
                return new String[]{"N.°", "DNI", "Estudiante", "Grado", "Sección", "Turno", "Condición"};
            }
            case T_ASIST -> {
                for (Alumno a : alumnos) filas.add(new Object[]{ a.dni(), a.nombre(), a.asistencias(), a.faltas(), a.tardanzas(), a.porcentajeAsistencia() });
                return new String[]{"DNI", "Estudiante", "Asistencias", "Faltas", "Tardanzas", "% Asistencia"};
            }
            case T_SIAGIE -> {
                for (Alumno a : alumnos) {
                    String[] notas = a.notasAreas(bim == 0 ? 4 : bim);
                    Object[] f = new Object[5 + notas.length];
                    f[0] = a.dni(); f[1] = a.nombre(); f[2] = gradoCorto; f[3] = secCorta; f[4] = CurriculoSecundaria.TURNO;
                    System.arraycopy(notas, 0, f, 5, notas.length);
                    filas.add(f);
                }
                return concat(new String[]{"DNI", "Estudiante", "Grado", "Sección", "Turno"}, nombresAreas());
            }
            default -> {   // Libreta individual: áreas → competencias → calificativo y nota del área
                return elegido == null ? columnasLibreta(bim) : filasLibreta(elegido, bim, filas);
            }
        }
    }

    private static String[] columnasLibreta(int bim) {
        return bim == 0
                ? new String[]{"Área curricular", "Competencia", "1.er Bim.", "2.° Bim.", "3.er Bim.", "4.° Bim.", "Nota final del área"}
                : new String[]{"Área curricular", "Competencia", "Calificativo", "Nota del área"};
    }

    /**
     * Arma las filas de la libreta. La nota del área solo se escribe en la primera fila de cada área.
     * Bimestre (1..4): un calificativo por competencia. Anual (0): los 4 bimestres y la nota final del
     * área con el último bimestre, sin promediar bimestres (RF14).
     */
    private String[] filasLibreta(Alumno a, int bim, List<Object[]> filas) {
        String[][] porBim = new String[4][];
        for (int b = 1; b <= 4; b++) porBim[b - 1] = a.competencias(b);
        String[] actual = bim == 0 ? porBim[3] : porBim[bim - 1];

        int idx = 0;
        for (AreaCurricular area : AREAS) {
            int total = area.competencias().size();
            String notaArea = ReglaCalificativo.calcularCalificativoArea(Arrays.copyOfRange(actual, idx, idx + total));
            for (int k = 0; k < total; k++) {
                String nombreArea = k == 0 ? area.nombre() : "";
                String notaFila = k == 0 ? notaArea : "";
                String competencia = area.competencias().get(k);
                if (bim == 0) {
                    filas.add(new Object[]{ nombreArea, competencia,
                            porBim[0][idx + k], porBim[1][idx + k], porBim[2][idx + k], porBim[3][idx + k], notaFila });
                } else {
                    filas.add(new Object[]{ nombreArea, competencia, actual[idx + k], notaFila });
                }
            }
            idx += total;
        }
        return columnasLibreta(bim);
    }

    private void aplicarAnchos(String tipo, boolean anual) {
        tablaRep.setRowHeight(T_LIBRETA.equals(tipo) ? 28 : 36);
        tablaRep.setAutoResizeMode(T_SIAGIE.equals(tipo) ? JTable.AUTO_RESIZE_OFF : JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);
        switch (tipo) {
            case T_NOMINA -> { EstilosUI.anchos(tablaRep, 50, 100, 260, 70, 70, 80, 110); EstilosUI.centrar(tablaRep, 0, 1, 3, 4, 5); }
            case T_ASIST  -> { EstilosUI.anchos(tablaRep, 100, 280, 100, 80, 100, 110); EstilosUI.centrar(tablaRep, 0, 2, 3, 4, 5); }
            case T_SIAGIE -> {
                int[] w = new int[5 + AREAS.size()];
                Arrays.fill(w, 125);
                w[0] = 80; w[1] = 190; w[2] = 55; w[3] = 65; w[4] = 75;
                EstilosUI.anchos(tablaRep, w);
                int[] centradas = new int[AREAS.size() + 4];
                centradas[0] = 0;
                for (int i = 1; i < centradas.length; i++) centradas[i] = i + 1;   // 2,3,4 y las áreas
                EstilosUI.centrar(tablaRep, centradas);
            }
            default -> {
                if (tablaRep.getColumnCount() == 0) return;
                if (anual) EstilosUI.anchos(tablaRep, 150, 330, 70, 70, 70, 70, 110);
                else       EstilosUI.anchos(tablaRep, 200, 470, 90, 100);
                for (int i = 0; i < tablaRep.getColumnCount(); i++) {
                    tablaRep.getColumnModel().getColumn(i).setCellRenderer(new LibretaRenderer());
                }
            }
        }
    }

    /** Render de la libreta: línea al inicio de cada área, área y nota del área en negrita, calificativos centrados. */
    private static final class LibretaRenderer extends DefaultTableCellRenderer {
        private static final Border SEPARADOR = new MatteBorder(1, 0, 0, 0, new Color(150, 160, 175));

        @Override
        public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foc, int r, int c) {
            super.getTableCellRendererComponent(t, v, sel, foc, r, c);
            boolean inicioArea = !String.valueOf(t.getValueAt(r, 0)).isBlank();
            Border sep = (inicioArea && r > 0) ? SEPARADOR : new EmptyBorder(0, 0, 0, 0);
            setBorder(BorderFactory.createCompoundBorder(sep, new EmptyBorder(0, 10, 0, 10)));

            int ultima = t.getColumnCount() - 1;
            boolean esCalificativo = c >= 2;
            setHorizontalAlignment(esCalificativo ? SwingConstants.CENTER : SwingConstants.LEFT);
            setFont(EstilosUI.font(c == 0 || c == ultima ? Font.BOLD : Font.PLAIN, 13));
            setForeground("C".equals(v) ? EstilosUI.DANGER_RED : EstilosUI.TEXT_DARK);
            setToolTipText((c == 0 || c == 1) && v != null ? v.toString() : null);
            return this;
        }
    }

    // ── Acciones de salida ────────────────────────────────────────
    /** PDF vía impresión del JTable (sin dependencias): el usuario elige impresora o «Microsoft Print to PDF». */
    private void imprimirVistaPrevia() {
        if (modeloPrevia.getRowCount() == 0) {
            EstilosUI.aviso(this, "Sin datos", "La vista previa no tiene registros para imprimir. Ajusta los filtros.");
            return;
        }
        boolean libreta = T_LIBRETA.equals(cboTipoReporte.getSelectedItem());
        String periodo = (String) cboBimestreRep.getSelectedItem();
        String grado = (String) cboGradoRep.getSelectedItem();
        String titulo = libreta && alumnoVista != null
                ? "INFORME DE PROGRESO DEL APRENDIZAJE — " + periodo + " — " + alumnoVista.nombre()
                    + " (DNI " + alumnoVista.dni() + ") — " + grado.substring(0, 3) + " " + letraSeccion((String) cboSeccionRep.getSelectedItem())
                    + " — Turno " + CurriculoSecundaria.TURNO
                : cboTipoReporte.getSelectedItem() + " — " + periodo + " — " + grado + " — " + cboSeccionRep.getSelectedItem()
                    + " — Turno " + CurriculoSecundaria.TURNO;
        String pie = libreta ? "Colegio Pitágoras · AD logro destacado | A logro esperado | B en proceso | C en inicio · Página {0}"
                             : "Colegio Pitágoras · Página {0}";
        try {
            boolean impreso = tablaRep.print(JTable.PrintMode.FIT_WIDTH,
                    new MessageFormat(titulo.replace("'", "''").replace("{", "'{'")), new MessageFormat(pie));
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
        String tipo = (String) cboTipoReporte.getSelectedItem();
        String base = tipo + "_" + cboBimestreRep.getSelectedItem() + "_" + cboGradoRep.getSelectedItem().toString().substring(0, 2);
        DefaultTableModel modelo = modeloPrevia;
        if (T_LIBRETA.equals(tipo) && alumnoVista != null) {
            base += "_" + alumnoVista.dni();
            modelo = libretaParaExcel();
        }
        guardarXlsx(nombreSeguro(base), tipo, modelo);
    }

    /** En Excel cada fila debe ser autosuficiente: se repiten el área y su nota en todas las filas del área. */
    private DefaultTableModel libretaParaExcel() {
        int cols = modeloPrevia.getColumnCount();
        String[] nombres = new String[cols];
        for (int c = 0; c < cols; c++) nombres[c] = modeloPrevia.getColumnName(c);
        DefaultTableModel copia = new DefaultTableModel(nombres, 0);
        Object area = "", nota = "";
        for (int r = 0; r < modeloPrevia.getRowCount(); r++) {
            Object[] fila = new Object[cols];
            for (int c = 0; c < cols; c++) fila[c] = modeloPrevia.getValueAt(r, c);
            if (!String.valueOf(fila[0]).isBlank()) { area = fila[0]; nota = fila[cols - 1]; }
            fila[0] = area;
            fila[cols - 1] = nota;
            copia.addRow(fila);
        }
        return copia;
    }

    /** RF19: resumen por estudiante (nota por área + asistencia) para entregar al apoderado al cierre del bimestre. */
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

        int bim = numeroBimestre(periodo);
        String[] cols = concat(concat(new String[]{"DNI", "Estudiante"}, nombresAreas()),
                new String[]{"Asistencias", "Faltas", "Tardanzas", "Estado"});
        DefaultTableModel resumen = new DefaultTableModel(cols, 0);
        for (Alumno a : alumnos) {
            String[] notas = a.notasAreas(bim);
            Object[] f = new Object[cols.length];
            f[0] = a.dni(); f[1] = a.nombre();
            System.arraycopy(notas, 0, f, 2, notas.length);
            int o = 2 + notas.length;
            f[o] = a.asistencias(); f[o + 1] = a.faltas(); f[o + 2] = a.tardanzas(); f[o + 3] = a.estado(bim);
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
    private static String[] nombresAreas() {
        return AREAS.stream().map(AreaCurricular::nombreCorto).toArray(String[]::new);
    }

    /** Notas por área a partir de los calificativos de todas las competencias (en orden del currículo). */
    private static String[] calcularNotasAreas(String[] competencias) {
        String[] notas = new String[AREAS.size()];
        int idx = 0;
        for (int i = 0; i < notas.length; i++) {
            int total = AREAS.get(i).competencias().size();
            notas[i] = ReglaCalificativo.calcularCalificativoArea(Arrays.copyOfRange(competencias, idx, idx + total));
            idx += total;
        }
        return notas;
    }

    /** "Bimestre 2" → 2; "Anual 2026" → 0. */
    private static int numeroBimestre(String periodo) {
        return periodo.startsWith("Anual") ? 0 : Character.getNumericValue(periodo.charAt(periodo.length() - 1));
    }

    private static String letraSeccion(String seccion) {
        return seccion.equals("Sección B") ? "B" : "A";
    }

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
