package pe.edu.pitagoras.sga.view.docente;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import pe.edu.pitagoras.sga.controller.DocenteController;
import pe.edu.pitagoras.sga.model.academico.Alumno;
import pe.edu.pitagoras.sga.model.academico.CargaAcademica;
import pe.edu.pitagoras.sga.view.comun.ModeloTabla;
import pe.edu.pitagoras.sga.view.comun.PanelModulo;
import pe.edu.pitagoras.sga.view.comun.Tarea;
import pe.edu.pitagoras.sga.view.comun.Tarjeta;
import pe.edu.pitagoras.sga.view.comun.Tema;

/** RF11: áreas y secciones asignadas al docente y la lista de alumnos de cada una. */
public class PnlMiCarga extends PanelModulo {

    private final transient DocenteController controller;
    private final JLabel kpiSecciones = new JLabel("-");
    private final JLabel kpiAreas = new JLabel("-");
    private final JLabel kpiAlumnos = new JLabel("-");
    private final ModeloTabla<CargaAcademica> modeloCargas = new ModeloTabla<CargaAcademica>()
            .columna("Área curricular", c -> c.curso().nombre())
            .columna("Grado y sección", c -> c.seccion().etiqueta())
            .columna("Turno", c -> c.seccion().turno());
    private final ModeloTabla<Alumno> modeloAlumnos = new ModeloTabla<>();
    private final JTable tablaCargas = new JTable(modeloCargas);
    private final JLabel lblAlumnos = Tema.seccion("Alumnos");
    private final Map<Integer, List<Alumno>> alumnosPorSeccion = new LinkedHashMap<>();

    public PnlMiCarga(DocenteController controller) {
        super("Mi carga académica", "Áreas y secciones asignadas para el año escolar 2026 (RF11)");
        this.controller = controller;

        modeloAlumnos.numero()
                .columna("DNI", Alumno::dni)
                .columna("Apellidos y nombres", Alumno::nombreCompleto)
                .columna("Apoderado", Alumno::nombreApoderado)
                .columna("Teléfono", a -> a.telefonoApoderado() == null ? "-" : a.telefonoApoderado());

        JPanel kpis = new JPanel(new GridLayout(1, 3, 16, 0));
        kpis.setOpaque(false);
        kpis.add(Tema.kpi("Secciones a cargo", kpiSecciones, null));
        kpis.add(Tema.kpi("Áreas que dicta", kpiAreas, null));
        kpis.add(Tema.kpi("Alumnos en total", kpiAlumnos, Tema.ACENTO));

        Tarjeta cardCargas = new Tarjeta();
        cardCargas.setLayout(new BorderLayout(0, 10));
        cardCargas.add(Tema.seccion("Secciones y áreas asignadas"), BorderLayout.NORTH);
        cardCargas.add(Tema.tabla(tablaCargas), BorderLayout.CENTER);

        JTable tablaAlumnos = new JTable(modeloAlumnos);
        Tarjeta cardAlumnos = new Tarjeta();
        cardAlumnos.setLayout(new BorderLayout(0, 10));
        cardAlumnos.add(lblAlumnos, BorderLayout.NORTH);
        cardAlumnos.add(Tema.tabla(tablaAlumnos), BorderLayout.CENTER);
        tablaAlumnos.getColumnModel().getColumn(0).setMaxWidth(60);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, cardCargas, cardAlumnos);
        split.setResizeWeight(0.34);
        split.setBorder(null);
        split.setOpaque(false);
        split.setDividerSize(14);

        contenido.add(kpis, BorderLayout.NORTH);
        contenido.add(split, BorderLayout.CENTER);

        tablaCargas.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                mostrarAlumnos();
            }
        });
    }

    @Override
    protected void alMostrar(boolean primeraVez) {
        Tarea.ejecutar(this, () -> {
            List<CargaAcademica> cargas = controller.misCargas();
            Map<Integer, List<Alumno>> alumnos = new LinkedHashMap<>();
            for (CargaAcademica c : cargas) {
                alumnos.computeIfAbsent(c.seccion().seccionId(), id -> controller.alumnos(c));
            }
            return Map.entry(cargas, alumnos);
        }, datos -> {
            alumnosPorSeccion.clear();
            alumnosPorSeccion.putAll(datos.getValue());
            List<CargaAcademica> cargas = datos.getKey();
            modeloCargas.setFilas(cargas);
            kpiSecciones.setText(String.valueOf(alumnosPorSeccion.size()));
            kpiAreas.setText(String.valueOf(cargas.stream().map(c -> c.curso().cursoId()).distinct().count()));
            kpiAlumnos.setText(String.valueOf(alumnosPorSeccion.values().stream().mapToInt(List::size).sum()));
            if (!cargas.isEmpty()) {
                tablaCargas.setRowSelectionInterval(0, 0);
            }
        });
    }

    private void mostrarAlumnos() {
        int fila = tablaCargas.getSelectedRow();
        if (fila < 0) {
            modeloAlumnos.setFilas(List.of());
            return;
        }
        CargaAcademica c = modeloCargas.getFila(tablaCargas.convertRowIndexToModel(fila));
        List<Alumno> alumnos = alumnosPorSeccion.getOrDefault(c.seccion().seccionId(), List.of());
        lblAlumnos.setText("Alumnos de " + c.seccion().etiqueta() + " (" + alumnos.size() + ")");
        modeloAlumnos.setFilas(alumnos);
    }
}
