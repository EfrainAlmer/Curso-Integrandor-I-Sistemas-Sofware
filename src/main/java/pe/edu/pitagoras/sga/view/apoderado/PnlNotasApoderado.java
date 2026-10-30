package pe.edu.pitagoras.sga.view.apoderado;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import pe.edu.pitagoras.sga.controller.ApoderadoController;
import pe.edu.pitagoras.sga.model.academico.Alumno;
import pe.edu.pitagoras.sga.model.academico.Periodo;
import pe.edu.pitagoras.sga.model.evaluacion.AsistenciaRegistro;
import pe.edu.pitagoras.sga.model.evaluacion.Calificativo;
import pe.edu.pitagoras.sga.model.evaluacion.FilaConsultaNota;
import pe.edu.pitagoras.sga.model.evaluacion.ResumenAsistencia;
import pe.edu.pitagoras.sga.view.comun.Archivos;
import pe.edu.pitagoras.sga.view.comun.Combos;
import pe.edu.pitagoras.sga.view.comun.ModeloTabla;
import pe.edu.pitagoras.sga.view.comun.PanelModulo;
import pe.edu.pitagoras.sga.view.comun.RenderInsignia;
import pe.edu.pitagoras.sga.view.comun.Tarea;
import pe.edu.pitagoras.sga.view.comun.Tarjeta;
import pe.edu.pitagoras.sga.view.comun.Tema;

/**
 * RF15: calificativos del estudiante por área, competencia y bimestre, con el resumen de asistencia
 * del bimestre (RF16). Solo lectura; el alumno siempre es el de la sesión.
 */
public class PnlNotasApoderado extends PanelModulo {

    private final transient ApoderadoController controller;
    private final JComboBox<Periodo> cboPeriodo = Combos.nuevo();
    private final JLabel kAsistencias = new JLabel("-");
    private final JLabel kFaltas = new JLabel("-");
    private final JLabel kTardanzas = new JLabel("-");
    private final JLabel kJustificadas = new JLabel("-");
    private final ModeloTabla<FilaConsultaNota> modelo = new ModeloTabla<FilaConsultaNota>()
            .columna("Área", f -> f.esResumen() ? f.area() : "")
            .columna("Competencia", FilaConsultaNota::competencia)
            .columna("Calificativo", Calificativo.class, FilaConsultaNota::calificativo)
            .columna("Conclusión descriptiva del docente", f -> f.conclusion() == null ? "" : f.conclusion());
    private Alumno alumno;
    private boolean cargando;

    public PnlNotasApoderado(ApoderadoController controller) {
        super("Calificaciones", "Vista de solo lectura para el alumno y su apoderado");
        this.controller = controller;
        cboPeriodo.setPreferredSize(new java.awt.Dimension(200, 36));
        accion(Tema.etiqueta("Periodo:"));
        accion(cboPeriodo);

        JPanel kpis = new JPanel(new GridLayout(1, 4, 16, 0));
        kpis.setOpaque(false);
        kpis.add(Tema.kpi("Asistencias", kAsistencias, null));
        kpis.add(Tema.kpi("Faltas", kFaltas, null));
        kpis.add(Tema.kpi("Tardanzas", kTardanzas, null));
        kpis.add(Tema.kpi("Faltas justificadas", kJustificadas, null));

        JTable tabla = new JTable(modelo);
        tabla.setRowSelectionAllowed(false);
        DefaultTableCellRenderer texto = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object v, boolean s, boolean f, int r, int c) {
                super.getTableCellRendererComponent(t, v, false, false, r, c);
                boolean resumen = modelo.getFila(r).esResumen();
                setFont(getFont().deriveFont(resumen ? Font.BOLD : Font.PLAIN));
                setBackground(resumen ? Tema.FONDO_RESUMEN : t.getBackground());
                setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 12));
                return this;
            }
        };
        tabla.getColumnModel().getColumn(0).setCellRenderer(texto);
        tabla.getColumnModel().getColumn(1).setCellRenderer(texto);
        tabla.getColumnModel().getColumn(2).setCellRenderer(new RenderInsignia(true));
        tabla.getColumnModel().getColumn(0).setPreferredWidth(200);
        tabla.getColumnModel().getColumn(1).setPreferredWidth(380);
        tabla.getColumnModel().getColumn(2).setPreferredWidth(110);
        tabla.getColumnModel().getColumn(3).setPreferredWidth(420);
        tabla.getColumnModel().getColumn(3).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object v, boolean s, boolean f, int r, int c) {
                super.getTableCellRendererComponent(t, v, false, false, r, c);
                setToolTipText(v == null || v.toString().isEmpty() ? null : "<html><p width='380'>" + v + "</p></html>");
                setForeground(Tema.TEXTO_SUAVE);
                setBackground(modelo.getFila(r).esResumen() ? Tema.FONDO_RESUMEN : t.getBackground());
                setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 12));
                return this;
            }
        });
        Tarjeta cardTabla = new Tarjeta();
        cardTabla.setLayout(new BorderLayout());
        cardTabla.add(Tema.tabla(tabla), BorderLayout.CENTER);

        JButton libreta = Tema.botonPrimario("Ver libreta (PDF)");
        JPanel sur = new JPanel(new BorderLayout());
        sur.setOpaque(false);
        sur.add(Tema.subtitulo("Los calificativos finales del año corresponden al último bimestre. "
                + "En negrita, el calificativo de cada área."), BorderLayout.WEST);
        sur.add(libreta, BorderLayout.EAST);

        contenido.add(kpis, BorderLayout.NORTH);
        contenido.add(cardTabla, BorderLayout.CENTER);
        contenido.add(sur, BorderLayout.SOUTH);
        cboPeriodo.addActionListener(e -> cargar());
        libreta.addActionListener(e -> {
            if (alumno != null) {
                Archivos.guardar(this, "Libreta_" + alumno.apellido() + ".pdf", controller::exportarLibreta);
            }
        });
    }

    @Override
    protected void alMostrar(boolean primeraVez) {
        if (!primeraVez) {
            cargar();
            return;
        }
        cargando = true;
        Tarea.ejecutar(this, () -> new Object[]{controller.miAlumno(), controller.periodos()}, d -> {
            alumno = (Alumno) d[0];
            @SuppressWarnings("unchecked")
            List<Periodo> periodos = (List<Periodo>) d[1];
            Combos.llenar(cboPeriodo, periodos);
            Combos.seleccionar(cboPeriodo, p -> !p.cerrado());
            cargando = false;
            cargar();
        });
    }

    private void cargar() {
        Periodo p = Combos.seleccionado(cboPeriodo);
        if (cargando || p == null || alumno == null) {
            return;
        }
        setCabecera(alumno.nombre() + " " + alumno.apellido() + " · " + alumno.seccion().etiqueta(),
                "Vista de solo lectura para el alumno y su apoderado · Bimestre " + p.bimestre());
        Tarea.ejecutar(this, () -> new Object[]{controller.notas(p), controller.asistencia(p)}, d -> {
            @SuppressWarnings("unchecked")
            List<FilaConsultaNota> notas = (List<FilaConsultaNota>) d[0];
            @SuppressWarnings("unchecked")
            List<AsistenciaRegistro> asistencia = (List<AsistenciaRegistro>) d[1];
            modelo.setFilas(notas);
            ResumenAsistencia r = ResumenAsistencia.de(asistencia);
            kAsistencias.setText(String.valueOf(r.asistencias()));
            kFaltas.setText(String.valueOf(r.faltas()));
            kTardanzas.setText(String.valueOf(r.tardanzas()));
            kJustificadas.setText(String.valueOf(r.justificadas()));
        });
    }
}
