package pe.edu.pitagoras.sga.view.director;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.util.List;
import java.util.Locale;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;
import pe.edu.pitagoras.sga.controller.DirectorController;
import pe.edu.pitagoras.sga.model.academico.Alumno;
import pe.edu.pitagoras.sga.model.academico.Periodo;
import pe.edu.pitagoras.sga.model.academico.Seccion;
import pe.edu.pitagoras.sga.model.evaluacion.Calificativo;
import pe.edu.pitagoras.sga.model.evaluacion.Consolidado;
import pe.edu.pitagoras.sga.model.evaluacion.FilaConsolidado;
import pe.edu.pitagoras.sga.view.comun.Archivos;
import pe.edu.pitagoras.sga.view.comun.Combos;
import pe.edu.pitagoras.sga.view.comun.PanelModulo;
import pe.edu.pitagoras.sga.view.comun.Tarea;
import pe.edu.pitagoras.sga.view.comun.Tarjeta;
import pe.edu.pitagoras.sga.view.comun.Tema;

/**
 * RF17, RF18 y RF19: consolidado bimestral por sección (vista previa, PDF y Excel para el SIAGIE),
 * libretas por alumno y nómina de matrícula.
 */
public class PnlReportes extends PanelModulo {

    private final transient DirectorController controller;
    private final JComboBox<Seccion> cboSeccion = Combos.nuevo();
    private final JComboBox<Periodo> cboPeriodo = Combos.nuevo();
    private final JComboBox<Alumno> cboAlumno = Combos.nuevo();
    private final ModeloConsolidado modelo = new ModeloConsolidado();
    private final JTable tabla = new JTable(modelo);
    private final JLabel lblVista = Tema.seccion("Vista previa del consolidado");
    private boolean cargando;

    public PnlReportes(DirectorController controller) {
        super("Reportes y exportación", "Consolidado bimestral, libretas y nóminas en PDF y Excel (RF17, RF18, RF19)");
        this.controller = controller;

        JPanel filtros = new JPanel(new GridLayout(1, 3, 14, 0));
        filtros.setOpaque(false);
        filtros.add(Tema.campo("Grado y sección", cboSeccion));
        filtros.add(Tema.campo("Bimestre", cboPeriodo));
        filtros.add(Tema.campo("Alumno (para la libreta)", cboAlumno));

        JButton consPdf = Tema.botonPrimario("Consolidado PDF");
        JButton consXls = Tema.botonSecundario("Consolidado Excel (SIAGIE)");
        JButton libreta = Tema.botonSecundario("Libreta PDF");
        JButton nominaPdf = Tema.botonSecundario("Nómina PDF");
        JButton nominaXls = Tema.botonSecundario("Nómina completa Excel");
        JPanel botones = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        botones.setOpaque(false);
        botones.add(consPdf);
        botones.add(consXls);
        botones.add(libreta);
        botones.add(nominaPdf);
        botones.add(nominaXls);
        Tarjeta cardFiltros = new Tarjeta();
        cardFiltros.setLayout(new BorderLayout(0, 14));
        cardFiltros.add(filtros, BorderLayout.CENTER);
        cardFiltros.add(botones, BorderLayout.SOUTH);

        Tarjeta cardTabla = new Tarjeta();
        cardTabla.setLayout(new BorderLayout(0, 10));
        cardTabla.add(lblVista, BorderLayout.NORTH);
        cardTabla.add(Tema.tabla(tabla), BorderLayout.CENTER);
        cardTabla.add(Tema.subtitulo("Calificativo de cada área en el bimestre y resumen de asistencia del alumno, "
                + "listo para entregar al apoderado o cargar en el SIAGIE."), BorderLayout.SOUTH);
        contenido.add(cardFiltros, BorderLayout.NORTH);
        contenido.add(cardTabla, BorderLayout.CENTER);

        cboSeccion.addActionListener(e -> cargar());
        cboPeriodo.addActionListener(e -> cargar());
        consPdf.addActionListener(e -> exportar(0));
        consXls.addActionListener(e -> exportar(1));
        libreta.addActionListener(e -> exportar(2));
        nominaPdf.addActionListener(e -> exportar(3));
        nominaXls.addActionListener(e -> exportar(4));
    }

    @Override
    protected void alMostrar(boolean primeraVez) {
        if (!primeraVez) {
            cargar();
            return;
        }
        cargando = true;
        Tarea.ejecutar(this, () -> new Object[]{controller.secciones(), controller.periodos()}, d -> {
            @SuppressWarnings("unchecked")
            List<Seccion> s = (List<Seccion>) d[0];
            @SuppressWarnings("unchecked")
            List<Periodo> p = (List<Periodo>) d[1];
            Combos.llenar(cboSeccion, s);
            Combos.llenar(cboPeriodo, p);
            Combos.seleccionar(cboPeriodo, x -> !x.cerrado());
            cargando = false;
            cargar();
        });
    }

    private void cargar() {
        Seccion s = Combos.seleccionado(cboSeccion);
        Periodo p = Combos.seleccionado(cboPeriodo);
        if (cargando || s == null || p == null) {
            return;
        }
        Tarea.ejecutar(this, () -> controller.consolidado(s, p), c -> {
            modelo.setConsolidado(c);
            tabla.getColumnModel().getColumn(0).setMaxWidth(55);
            tabla.getColumnModel().getColumn(1).setPreferredWidth(240);
            tabla.getTableHeader().setPreferredSize(new java.awt.Dimension(0, 56));
            lblVista.setText("Vista previa del consolidado: " + s.etiqueta() + " · " + p.etiqueta());
            Alumno antes = Combos.seleccionado(cboAlumno);
            Combos.llenar(cboAlumno, c.filas().stream().map(FilaConsolidado::alumno).toList());
            if (antes != null) {
                Combos.seleccionar(cboAlumno, a -> a.alumnoId() == antes.alumnoId());
            }
        });
    }

    private void exportar(int tipo) {
        Seccion s = Combos.seleccionado(cboSeccion);
        Periodo p = Combos.seleccionado(cboPeriodo);
        Alumno a = Combos.seleccionado(cboAlumno);
        if (s == null || p == null || (tipo == 2 && a == null)) {
            return;
        }
        String sec = s.etiqueta();
        switch (tipo) {
            case 0 -> Archivos.guardar(this, "Consolidado_" + sec + "_B" + p.bimestre() + ".pdf",
                    f -> controller.exportarConsolidadoPdf(s, p, f));
            case 1 -> Archivos.guardar(this, "Consolidado_" + sec + "_B" + p.bimestre() + ".xlsx",
                    f -> controller.exportarConsolidadoExcel(s, p, f));
            case 2 -> Archivos.guardar(this, "Libreta_" + a.apellido() + "_" + a.nombre() + ".pdf",
                    f -> controller.exportarLibretaPdf(a, f));
            case 3 -> Archivos.guardar(this, "Nomina_" + sec + ".pdf", f -> controller.exportarNominaPdf(s, f));
            default -> Archivos.guardar(this, "Nomina_completa.xlsx", f -> controller.exportarNominaExcel(null, f));
        }
    }

    /** Columnas dinámicas: una por área de la sección. */
    private static class ModeloConsolidado extends AbstractTableModel {
        private Consolidado c;

        void setConsolidado(Consolidado nuevo) {
            c = nuevo;
            fireTableStructureChanged();
        }

        private int areas() {
            return c == null ? 0 : c.cursos().size();
        }

        @Override
        public int getRowCount() {
            return c == null ? 0 : c.filas().size();
        }

        @Override
        public int getColumnCount() {
            return 2 + areas() + 3;
        }

        @Override
        public String getColumnName(int col) {
            if (col == 0) {
                return "N.°";
            }
            if (col == 1) {
                return "Apellidos y nombres";
            }
            if (col < 2 + areas()) {
                return "<html>" + c.cursos().get(col - 2).nombre() + "</html>";
            }
            return new String[]{"Faltas", "Tardanzas", "% asist."}[col - 2 - areas()];
        }

        @Override
        public Class<?> getColumnClass(int col) {
            return col >= 2 && col < 2 + areas() ? Calificativo.class : Object.class;
        }

        @Override
        public Object getValueAt(int f, int col) {
            FilaConsolidado fila = c.filas().get(f);
            if (col == 0) {
                return f + 1;
            }
            if (col == 1) {
                return fila.alumno().nombreCompleto();
            }
            if (col < 2 + areas()) {
                return fila.porCurso().get(c.cursos().get(col - 2).cursoId());
            }
            return switch (col - 2 - areas()) {
                case 0 -> fila.asistencia().faltas() + fila.asistencia().justificadas();
                case 1 -> fila.asistencia().tardanzas();
                default -> String.format(Locale.ROOT, "%.0f%%", fila.asistencia().porcentajeAsistencia());
            };
        }
    }
}
