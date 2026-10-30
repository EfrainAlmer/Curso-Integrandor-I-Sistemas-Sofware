package pe.edu.pitagoras.sga.view.director;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridLayout;
import java.util.Locale;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;
import pe.edu.pitagoras.sga.controller.DirectorController;
import pe.edu.pitagoras.sga.model.academico.Periodo;
import pe.edu.pitagoras.sga.model.gestion.AlertaAlumno;
import pe.edu.pitagoras.sga.model.gestion.AvanceDocente;
import pe.edu.pitagoras.sga.model.gestion.Indicadores;
import pe.edu.pitagoras.sga.view.comun.Combos;
import pe.edu.pitagoras.sga.view.comun.ModeloTabla;
import pe.edu.pitagoras.sga.view.comun.PanelModulo;
import pe.edu.pitagoras.sga.view.comun.Tarea;
import pe.edu.pitagoras.sga.view.comun.Tarjeta;
import pe.edu.pitagoras.sga.view.comun.Tema;

/**
 * RF08: panel de indicadores de la Dirección: asistencia, avance del registro de notas por
 * docente, distribución de calificativos por grado y alertas de alumnos con C.
 */
public class PnlDashboard extends PanelModulo {

    private final transient DirectorController controller;
    private final JComboBox<Periodo> cboPeriodo = Combos.nuevo();
    private final JLabel kAlumnos = new JLabel("-");
    private final JLabel kAsistencia = new JLabel("-");
    private final JLabel kAvance = new JLabel("-");
    private final JLabel kRiesgo = new JLabel("-");
    private final GraficoDistribucion grafico = new GraficoDistribucion();
    private final ModeloTabla<AvanceDocente> modeloAvance = new ModeloTabla<AvanceDocente>()
            .columna("Docente", AvanceDocente::docente)
            .columna("Registrados", a -> a.registradas() + " / " + a.esperados())
            .columna("Avance", Double.class, AvanceDocente::porcentaje);
    private final ModeloTabla<AlertaAlumno> modeloAlertas = new ModeloTabla<AlertaAlumno>()
            .columna("Alumno", AlertaAlumno::alumno)
            .columna("Sección", AlertaAlumno::seccion)
            .columna("Áreas con C", a -> String.join(", ", a.areas()));
    private final JLabel lblAlertas = Tema.seccion("Alertas: alumnos con C");
    private final JComboBox<String> cboAgrupar = Combos.nuevo();
    private Indicadores ultimo;
    private boolean cargando;

    public PnlDashboard(DirectorController controller) {
        super("Panel de indicadores", "Avance académico del colegio en el bimestre seleccionado (RF08)");
        this.controller = controller;
        cboPeriodo.setPreferredSize(new java.awt.Dimension(240, 36));
        accion(Tema.etiqueta("Bimestre:"));
        accion(cboPeriodo);

        JPanel kpis = new JPanel(new GridLayout(1, 4, 16, 0));
        kpis.setOpaque(false);
        kpis.add(Tema.kpi("Alumnos activos", kAlumnos, null));
        Combos.llenar(cboAgrupar, java.util.List.of("Por grado", "Por sección", "Por área"));
        kpis.add(Tema.kpi("Asistencia del bimestre", kAsistencia, Tema.EXITO));
        kpis.add(Tema.kpi("Avance del registro de notas", kAvance, Tema.INFO));
        kpis.add(Tema.kpi("Alumnos con alguna área en C", kRiesgo, Tema.PELIGRO));

        Tarjeta cardGrafico = new Tarjeta();
        cardGrafico.setLayout(new BorderLayout(0, 10));
        JPanel cabGrafico = new JPanel(new BorderLayout());
        cabGrafico.setOpaque(false);
        cabGrafico.add(Tema.seccion("Distribución de calificativos de área"), BorderLayout.WEST);
        cabGrafico.add(cboAgrupar, BorderLayout.EAST);
        cardGrafico.add(cabGrafico, BorderLayout.NORTH);
        javax.swing.JScrollPane spGrafico = new javax.swing.JScrollPane(grafico);
        spGrafico.setBorder(null);
        spGrafico.getViewport().setBackground(java.awt.Color.WHITE);
        spGrafico.getVerticalScrollBar().setUnitIncrement(16);
        cardGrafico.add(spGrafico, BorderLayout.CENTER);

        JTable tablaAvance = new JTable(modeloAvance);
        tablaAvance.getColumnModel().getColumn(2).setCellRenderer(new RenderBarra());
        Tarjeta cardAvance = new Tarjeta();
        cardAvance.setLayout(new BorderLayout(0, 10));
        cardAvance.add(Tema.seccion("Avance del registro de notas por docente"), BorderLayout.NORTH);
        cardAvance.add(Tema.tabla(tablaAvance), BorderLayout.CENTER);

        JPanel medio = new JPanel(new GridLayout(1, 2, 16, 0));
        medio.setOpaque(false);
        medio.add(cardGrafico);
        medio.add(cardAvance);

        JTable tablaAlertas = new JTable(modeloAlertas);
        tablaAlertas.getColumnModel().getColumn(0).setPreferredWidth(250);
        tablaAlertas.getColumnModel().getColumn(2).setPreferredWidth(380);
        Tarjeta cardAlertas = new Tarjeta();
        cardAlertas.setLayout(new BorderLayout(0, 10));
        cardAlertas.add(lblAlertas, BorderLayout.NORTH);
        cardAlertas.add(Tema.tabla(tablaAlertas), BorderLayout.CENTER);

        JPanel cuerpo = new JPanel(new GridLayout(2, 1, 0, 16));
        cuerpo.setOpaque(false);
        cuerpo.add(medio);
        cuerpo.add(cardAlertas);
        contenido.add(kpis, BorderLayout.NORTH);
        contenido.add(cuerpo, BorderLayout.CENTER);
        cboPeriodo.addActionListener(e -> cargar());
        cboAgrupar.addActionListener(e -> mostrarGrafico());
    }

    private void mostrarGrafico() {
        if (ultimo == null) {
            return;
        }
        int i = cboAgrupar.getSelectedIndex();
        grafico.setDatos(i == 1 ? ultimo.distribucionPorSeccion()
                : i == 2 ? ultimo.distribucionPorArea() : ultimo.distribucionPorGrado());
    }

    @Override
    protected void alMostrar(boolean primeraVez) {
        if (!primeraVez) {
            cargar();
            return;
        }
        cargando = true;
        Tarea.ejecutar(this, controller::periodos, periodos -> {
            Combos.llenar(cboPeriodo, periodos);
            Combos.seleccionar(cboPeriodo, p -> !p.cerrado());
            cargando = false;
            cargar();
        });
    }

    private void cargar() {
        Periodo p = Combos.seleccionado(cboPeriodo);
        if (cargando || p == null) {
            return;
        }
        Tarea.ejecutar(this, () -> controller.indicadores(p), this::mostrar);
    }

    private void mostrar(Indicadores i) {
        ultimo = i;
        kAlumnos.setText(String.valueOf(i.totalAlumnos()));
        setCabecera("Panel de indicadores", "Avance académico del " + i.periodo().etiqueta() + " · "
                + i.totalSecciones() + " secciones con alumnos (RF08)");
        kAsistencia.setText(String.format(Locale.ROOT, "%.1f%%", i.porcentajeAsistencia()));
        kAvance.setText(String.format(Locale.ROOT, "%.0f%%", i.avanceGlobal()));
        kRiesgo.setText(String.valueOf(i.alertas().size()));
        mostrarGrafico();
        modeloAvance.setFilas(i.avanceDocentes());
        modeloAlertas.setFilas(i.alertas());
        lblAlertas.setText("Alertas: alumnos con C en una o más áreas (" + i.alertas().size() + ")");
    }

    /** Barra de progreso dentro de la celda. */
    private static class RenderBarra implements TableCellRenderer {
        private final JProgressBar barra = new JProgressBar(0, 100);
        private final JPanel panel = new JPanel(new BorderLayout());

        RenderBarra() {
            barra.setStringPainted(true);
            barra.setForeground(Tema.ACENTO);
            panel.setBorder(javax.swing.BorderFactory.createEmptyBorder(10, 12, 10, 12));
            panel.add(barra);
        }

        @Override
        public Component getTableCellRendererComponent(JTable t, Object v, boolean s, boolean f, int r, int c) {
            double pct = v instanceof Double d ? d : 0;
            barra.setValue((int) Math.round(pct));
            barra.setString(String.format(Locale.ROOT, "%.0f%%", pct));
            barra.setForeground(pct >= 100 ? Tema.EXITO : Tema.ACENTO);
            panel.setBackground(s ? t.getSelectionBackground() : t.getBackground());
            return panel;
        }
    }
}
