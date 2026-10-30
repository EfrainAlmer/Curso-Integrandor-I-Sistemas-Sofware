package pe.edu.pitagoras.sga.view.docente;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.List;
import java.util.Locale;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import pe.edu.pitagoras.sga.controller.DocenteController;
import pe.edu.pitagoras.sga.model.academico.CargaAcademica;
import pe.edu.pitagoras.sga.model.academico.Competencia;
import pe.edu.pitagoras.sga.model.academico.Periodo;
import pe.edu.pitagoras.sga.model.evaluacion.FilaNotas;
import pe.edu.pitagoras.sga.model.evaluacion.ResumenAlumno;
import pe.edu.pitagoras.sga.view.comun.Archivos;
import pe.edu.pitagoras.sga.view.comun.Combos;
import pe.edu.pitagoras.sga.view.comun.ModeloTabla;
import pe.edu.pitagoras.sga.view.comun.PanelModulo;
import pe.edu.pitagoras.sga.view.comun.Tarea;
import pe.edu.pitagoras.sga.view.comun.Tarjeta;
import pe.edu.pitagoras.sga.view.comun.Tema;

/** RF18: vista previa y emisión del acta de calificativos, asistencia y nómina (PDF y Excel). */
public class PnlReportesDocente extends PanelModulo {

    private final transient DocenteController controller;
    private final SelectorCarga selector = new SelectorCarga();
    private final JComboBox<Periodo> cboPeriodo = Combos.nuevo();
    private final NotasTableModel modeloNotas = new NotasTableModel();
    private final ModeloTabla<ResumenAlumno> modeloAsistencia = new ModeloTabla<ResumenAlumno>()
            .numero()
            .columna("Apellidos y nombres", r -> r.alumno().nombreCompleto())
            .columna("Asistió", Integer.class, r -> r.resumen().asistencias())
            .columna("Tardanza", Integer.class, r -> r.resumen().tardanzas())
            .columna("Falta", Integer.class, r -> r.resumen().faltas())
            .columna("F. justificada", Integer.class, r -> r.resumen().justificadas())
            .columna("% asistencia", r -> String.format(Locale.ROOT, "%.0f%%", r.resumen().porcentajeAsistencia()));
    private final JTable tablaNotas = new JTable(modeloNotas);
    private boolean cargando;

    public PnlReportesDocente(DocenteController controller) {
        super("Reportes", "Acta de calificativos, consolidado de asistencia y nómina con vista previa (RF18)");
        this.controller = controller;

        cboPeriodo.setPreferredSize(new java.awt.Dimension(190, 36));
        JPanel filtros = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        filtros.setOpaque(false);
        filtros.add(Tema.campo("Periodo", cboPeriodo));
        filtros.add(javax.swing.Box.createHorizontalStrut(16));
        filtros.add(Tema.campo("Sección", selector.cboSeccion));
        filtros.add(javax.swing.Box.createHorizontalStrut(16));
        filtros.add(Tema.campo("Área", selector.cboArea));
        JButton actaPdf = Tema.botonPrimario("Acta de notas (PDF)");
        JButton actaExcel = Tema.botonSecundario("Acta de notas (Excel)");
        JButton asisPdf = Tema.botonSecundario("Asistencia (PDF)");
        JButton nominaPdf = Tema.botonSecundario("Nómina (PDF)");
        JPanel botones = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        botones.setOpaque(false);
        botones.add(actaPdf);
        botones.add(actaExcel);
        botones.add(asisPdf);
        botones.add(nominaPdf);
        Tarjeta cardFiltros = new Tarjeta();
        cardFiltros.setLayout(new BorderLayout(0, 14));
        cardFiltros.add(filtros, BorderLayout.CENTER);
        cardFiltros.add(botones, BorderLayout.SOUTH);

        JTabbedPane pestanas = new JTabbedPane();
        pestanas.addTab("Vista previa: calificativos", Tema.tabla(tablaNotas));
        pestanas.addTab("Vista previa: asistencia", Tema.tabla(new JTable(modeloAsistencia)));
        Tarjeta cardTabla = new Tarjeta();
        cardTabla.setLayout(new BorderLayout());
        cardTabla.add(pestanas, BorderLayout.CENTER);

        contenido.add(cardFiltros, BorderLayout.NORTH);
        contenido.add(cardTabla, BorderLayout.CENTER);

        selector.setAlCambiar(this::cargar);
        cboPeriodo.addActionListener(e -> cargar());
        actaPdf.addActionListener(e -> exportar("Acta_" , ".pdf", 0));
        actaExcel.addActionListener(e -> exportar("Acta_", ".xlsx", 1));
        asisPdf.addActionListener(e -> exportar("Asistencia_", ".pdf", 2));
        nominaPdf.addActionListener(e -> exportar("Nomina_", ".pdf", 3));
    }

    @Override
    protected void alMostrar(boolean primeraVez) {
        if (!primeraVez) {
            cargar();
            return;
        }
        cargando = true;
        Tarea.ejecutar(this, () -> new Object[]{controller.misCargas(), controller.periodos(), controller.periodoActual()},
                d -> {
                    @SuppressWarnings("unchecked")
                    List<CargaAcademica> cargas = (List<CargaAcademica>) d[0];
                    @SuppressWarnings("unchecked")
                    List<Periodo> periodos = (List<Periodo>) d[1];
                    selector.setCargas(cargas);
                    Combos.llenar(cboPeriodo, periodos);
                    cboPeriodo.setSelectedItem(d[2]);
                    cargando = false;
                    cargar();
                });
    }

    private void cargar() {
        CargaAcademica carga = selector.getCarga();
        Periodo periodo = Combos.seleccionado(cboPeriodo);
        if (cargando || carga == null || periodo == null) {
            return;
        }
        Tarea.ejecutar(this, () -> new Object[]{controller.competencias(carga), controller.notas(carga, periodo),
                controller.resumenAsistencia(carga, periodo)}, d -> {
                    @SuppressWarnings("unchecked")
                    List<Competencia> comps = (List<Competencia>) d[0];
                    @SuppressWarnings("unchecked")
                    List<FilaNotas> filas = (List<FilaNotas>) d[1];
                    @SuppressWarnings("unchecked")
                    List<ResumenAlumno> asistencia = (List<ResumenAlumno>) d[2];
                    modeloNotas.cargar(comps, filas, false);
                    tablaNotas.getColumnModel().getColumn(0).setPreferredWidth(260);
                    tablaNotas.getTableHeader().setPreferredSize(new java.awt.Dimension(0, 96));
                    modeloAsistencia.setFilas(asistencia);
                });
    }

    private void exportar(String prefijo, String extension, int tipo) {
        CargaAcademica c = selector.getCarga();
        Periodo p = Combos.seleccionado(cboPeriodo);
        if (c == null || p == null) {
            return;
        }
        String nombre = prefijo + c.curso().nombre() + "_" + c.seccion().etiqueta()
                + (tipo == 3 ? "" : "_B" + p.bimestre()) + extension;
        Archivos.guardar(this, nombre, destino -> switch (tipo) {
            case 0 -> controller.exportarActaPdf(c, p, destino);
            case 1 -> controller.exportarActaExcel(c, p, destino);
            case 2 -> controller.exportarAsistenciaPdf(c, p, destino);
            default -> controller.exportarNominaPdf(c, destino);
        });
    }
}
