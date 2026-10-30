package pe.edu.pitagoras.sga.view.apoderado;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import pe.edu.pitagoras.sga.controller.ApoderadoController;
import pe.edu.pitagoras.sga.model.academico.Alumno;
import pe.edu.pitagoras.sga.model.academico.Periodo;
import pe.edu.pitagoras.sga.model.evaluacion.AsistenciaRegistro;
import pe.edu.pitagoras.sga.model.evaluacion.EstadoAsistencia;
import pe.edu.pitagoras.sga.model.evaluacion.ResumenAsistencia;
import pe.edu.pitagoras.sga.view.comun.Combos;
import pe.edu.pitagoras.sga.view.comun.Fechas;
import pe.edu.pitagoras.sga.view.comun.ModeloTabla;
import pe.edu.pitagoras.sga.view.comun.PanelModulo;
import pe.edu.pitagoras.sga.view.comun.RenderInsignia;
import pe.edu.pitagoras.sga.view.comun.Tarea;
import pe.edu.pitagoras.sga.view.comun.Tarjeta;
import pe.edu.pitagoras.sga.view.comun.Tema;

/** RF16: asistencias, faltas y tardanzas del estudiante, con el detalle por fecha y área. */
public class PnlAsistenciaApoderado extends PanelModulo {

    private static final String TODO_EL_ANIO = "Todo el año";

    private final transient ApoderadoController controller;
    private final JComboBox<Object> cboPeriodo = Combos.nuevo();
    private final JLabel kAsistio = new JLabel("-");
    private final JLabel kTardanza = new JLabel("-");
    private final JLabel kFalta = new JLabel("-");
    private final JLabel kJustificada = new JLabel("-");
    private final JLabel kPorcentaje = new JLabel("-");
    private final ModeloTabla<AsistenciaRegistro> modelo = new ModeloTabla<AsistenciaRegistro>()
            .columna("Fecha", r -> r.fecha().format(Fechas.DIA_SEMANA))
            .columna("Área", AsistenciaRegistro::curso)
            .columna("Estado", EstadoAsistencia.class, AsistenciaRegistro::estado)
            .columna("Observación", r -> r.observacion() == null ? "" : r.observacion());
    private boolean cargando;

    public PnlAsistenciaApoderado(ApoderadoController controller) {
        super("Asistencia", "Asistencias, tardanzas y faltas del estudiante (solo lectura)");
        this.controller = controller;
        JPanel kpis = new JPanel(new GridLayout(1, 5, 14, 0));
        kpis.setOpaque(false);
        kpis.add(Tema.kpi("Asistencias", kAsistio, Tema.EXITO));
        kpis.add(Tema.kpi("Tardanzas", kTardanza, Tema.ALERTA));
        kpis.add(Tema.kpi("Faltas", kFalta, Tema.PELIGRO));
        kpis.add(Tema.kpi("Faltas justificadas", kJustificada, Tema.INFO));
        kpis.add(Tema.kpi("% de asistencia", kPorcentaje, Tema.ACENTO));

        cboPeriodo.setPreferredSize(new java.awt.Dimension(200, 36));
        accion(Tema.etiqueta("Periodo:"));
        accion(cboPeriodo);
        JPanel norte = kpis;

        JTable tabla = new JTable(modelo);
        tabla.setDefaultRenderer(EstadoAsistencia.class, new RenderInsignia(true));
        Tarjeta cardTabla = new Tarjeta();
        cardTabla.setLayout(new BorderLayout(0, 8));
        cardTabla.add(Tema.seccion("Detalle por día y área"), BorderLayout.NORTH);
        cardTabla.add(Tema.tabla(tabla), BorderLayout.CENTER);
        contenido.add(norte, BorderLayout.NORTH);
        contenido.add(cardTabla, BorderLayout.CENTER);
        cboPeriodo.addActionListener(e -> cargar());
    }

    @Override
    protected void alMostrar(boolean primeraVez) {
        if (!primeraVez) {
            cargar();
            return;
        }
        cargando = true;
        Tarea.ejecutar(this, () -> new Object[]{controller.miAlumno(), controller.periodos()}, d -> {
            Alumno a = (Alumno) d[0];
            setCabecera("Asistencia · " + a.nombre() + " " + a.apellido(),
                    a.seccion().etiqueta() + " · Asistencias, tardanzas y faltas (solo lectura)");
            @SuppressWarnings("unchecked")
            List<Periodo> periodos = (List<Periodo>) d[1];
            List<Object> opciones = new ArrayList<>();
            opciones.add(TODO_EL_ANIO);
            opciones.addAll(periodos);
            Combos.llenar(cboPeriodo, opciones);
            cargando = false;
            cargar();
        });
    }

    private void cargar() {
        Object sel = cboPeriodo.getSelectedItem();
        if (cargando || sel == null) {
            return;
        }
        Periodo p = sel instanceof Periodo per ? per : null;
        Tarea.ejecutar(this, () -> controller.asistencia(p), registros -> {
            modelo.setFilas(registros);
            ResumenAsistencia r = ResumenAsistencia.de(registros);
            kAsistio.setText(String.valueOf(r.asistencias()));
            kTardanza.setText(String.valueOf(r.tardanzas()));
            kFalta.setText(String.valueOf(r.faltas()));
            kJustificada.setText(String.valueOf(r.justificadas()));
            kPorcentaje.setText(String.format(Locale.ROOT, "%.1f%%", r.porcentajeAsistencia()));
        });
    }
}
