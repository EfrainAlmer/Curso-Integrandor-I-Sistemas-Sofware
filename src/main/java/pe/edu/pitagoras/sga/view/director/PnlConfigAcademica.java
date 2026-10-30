package pe.edu.pitagoras.sga.view.director;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;
import pe.edu.pitagoras.sga.controller.DirectorController;
import pe.edu.pitagoras.sga.model.academico.Competencia;
import pe.edu.pitagoras.sga.model.academico.Curso;
import pe.edu.pitagoras.sga.model.academico.Grado;
import pe.edu.pitagoras.sga.model.academico.Seccion;
import pe.edu.pitagoras.sga.view.comun.Combos;
import pe.edu.pitagoras.sga.view.comun.DialogoFormulario;
import pe.edu.pitagoras.sga.view.comun.Mensajes;
import pe.edu.pitagoras.sga.view.comun.ModeloTabla;
import pe.edu.pitagoras.sga.view.comun.PanelModulo;
import pe.edu.pitagoras.sga.view.comun.Tarea;
import pe.edu.pitagoras.sga.view.comun.Tarjeta;
import pe.edu.pitagoras.sga.view.comun.Tema;

/** RF05: grados, secciones, turnos, áreas curriculares y sus competencias. */
public class PnlConfigAcademica extends PanelModulo {

    private final transient DirectorController controller;
    private final ModeloTabla<Seccion> modeloSecciones = new ModeloTabla<Seccion>()
            .columna("Grado", Seccion::grado)
            .columna("Sección", Seccion::letra)
            .columna("Turno", Seccion::turno);
    private final ModeloTabla<Curso> modeloCursos = new ModeloTabla<Curso>().columna("Área curricular", Curso::nombre);
    private final ModeloTabla<Competencia> modeloComp = new ModeloTabla<Competencia>()
            .columna("Código", c -> "C" + c.codigo())
            .columna("Competencia", Competencia::nombre);
    private final JTable tablaCursos = new JTable(modeloCursos);
    private List<Grado> grados = List.of();
    private List<String> turnos = List.of();

    public PnlConfigAcademica(DirectorController controller) {
        super("Configuración académica", "Estructura del año escolar: grados de 1.° a 5.°, secciones, turnos y áreas (RF05)");
        this.controller = controller;

        JButton nuevaSeccion = Tema.botonPrimario("+ Nueva sección");
        Tarjeta cardSecciones = new Tarjeta();
        cardSecciones.setLayout(new BorderLayout(0, 12));
        JPanel cabS = new JPanel(new BorderLayout());
        cabS.setOpaque(false);
        cabS.add(Tema.seccion("Grados y secciones"), BorderLayout.WEST);
        cabS.add(nuevaSeccion, BorderLayout.EAST);
        cardSecciones.add(cabS, BorderLayout.NORTH);
        cardSecciones.add(Tema.tabla(new JTable(modeloSecciones)), BorderLayout.CENTER);

        JButton nuevaArea = Tema.botonPrimario("+ Nueva área");
        Tarjeta cardAreas = new Tarjeta();
        cardAreas.setLayout(new BorderLayout(0, 12));
        JPanel cabA = new JPanel(new BorderLayout());
        cabA.setOpaque(false);
        cabA.add(Tema.seccion("Áreas curriculares y competencias"), BorderLayout.WEST);
        cabA.add(nuevaArea, BorderLayout.EAST);
        JTable tablaComp = new JTable(modeloComp);
        tablaComp.getColumnModel().getColumn(0).setMaxWidth(80);
        JPanel tablas = new JPanel(new GridLayout(2, 1, 0, 12));
        tablas.setOpaque(false);
        tablas.add(Tema.tabla(tablaCursos));
        tablas.add(Tema.tabla(tablaComp));
        cardAreas.add(cabA, BorderLayout.NORTH);
        cardAreas.add(tablas, BorderLayout.CENTER);
        cardAreas.add(Tema.subtitulo("Seleccione un área para ver sus competencias. Los periodos se configuran "
                + "en el módulo Periodos."), BorderLayout.SOUTH);

        JPanel cuerpo = new JPanel(new GridLayout(1, 2, 16, 0));
        cuerpo.setOpaque(false);
        cuerpo.add(cardSecciones);
        cuerpo.add(cardAreas);
        contenido.add(cuerpo, BorderLayout.CENTER);

        tablaCursos.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                mostrarCompetencias();
            }
        });
        nuevaSeccion.addActionListener(e -> nuevaSeccion());
        nuevaArea.addActionListener(e -> nuevaArea());
    }

    @Override
    protected void alMostrar(boolean primeraVez) {
        Tarea.ejecutar(this, () -> new Object[]{controller.secciones(), controller.cursos(), controller.grados(),
                controller.turnos()}, d -> {
                    @SuppressWarnings("unchecked")
                    List<Seccion> s = (List<Seccion>) d[0];
                    @SuppressWarnings("unchecked")
                    List<Curso> c = (List<Curso>) d[1];
                    @SuppressWarnings("unchecked")
                    List<Grado> g = (List<Grado>) d[2];
                    @SuppressWarnings("unchecked")
                    List<String> t = (List<String>) d[3];
                    grados = g;
                    turnos = t;
                    modeloSecciones.setFilas(s);
                    modeloCursos.setFilas(c);
                    if (!c.isEmpty()) {
                        tablaCursos.setRowSelectionInterval(0, 0);
                    }
                });
    }

    private void mostrarCompetencias() {
        int f = tablaCursos.getSelectedRow();
        if (f < 0) {
            modeloComp.setFilas(List.of());
            return;
        }
        Curso c = modeloCursos.getFila(tablaCursos.convertRowIndexToModel(f));
        Tarea.ejecutar(this, () -> controller.competencias(c), modeloComp::setFilas);
    }

    private void nuevaSeccion() {
        JComboBox<Grado> grado = Combos.nuevo();
        Combos.llenar(grado, grados);
        JTextField letra = new JTextField();
        JComboBox<String> turno = Combos.nuevo();
        Combos.llenar(turno, turnos);
        new DialogoFormulario(this, "Nueva sección", "Ejemplo: grado 1.°, sección E, turno Tarde.")
                .campo("Grado", grado).campo("Sección (letra)", letra).campo("Turno", turno)
                .mostrar(() -> {
                    controller.crearSeccion(Combos.seleccionado(grado), letra.getText(), Combos.seleccionado(turno));
                    return Boolean.TRUE;
                }, ok -> {
                    Mensajes.info(this, "Sección creada. Asigne sus áreas en Carga académica.");
                    alMostrar(false);
                });
    }

    private void nuevaArea() {
        JTextField nombre = new JTextField();
        JTextField[] comp = {new JTextField(), new JTextField(), new JTextField(), new JTextField()};
        DialogoFormulario d = new DialogoFormulario(this, "Nueva área curricular", "Ingrese de 1 a 4 competencias.")
                .campo("Nombre del área", nombre);
        for (int i = 0; i < comp.length; i++) {
            d.campo("Competencia " + (i + 1), comp[i]);
        }
        d.mostrar(() -> {
            List<String> lista = new ArrayList<>();
            for (JTextField t : comp) {
                lista.add(t.getText());
            }
            controller.crearCurso(nombre.getText(), lista);
            return Boolean.TRUE;
        }, ok -> {
            Mensajes.info(this, "Área creada. Asígnela a las secciones en Carga académica.");
            alMostrar(false);
        });
    }
}
