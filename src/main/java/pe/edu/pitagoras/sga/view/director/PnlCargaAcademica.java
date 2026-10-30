package pe.edu.pitagoras.sga.view.director;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.RowFilter;
import javax.swing.table.TableRowSorter;
import pe.edu.pitagoras.sga.controller.DirectorController;
import pe.edu.pitagoras.sga.model.academico.CargaAcademica;
import pe.edu.pitagoras.sga.model.academico.Curso;
import pe.edu.pitagoras.sga.model.academico.Docente;
import pe.edu.pitagoras.sga.model.academico.Seccion;
import pe.edu.pitagoras.sga.view.comun.Combos;
import pe.edu.pitagoras.sga.view.comun.Mensajes;
import pe.edu.pitagoras.sga.view.comun.ModeloTabla;
import pe.edu.pitagoras.sga.view.comun.PanelModulo;
import pe.edu.pitagoras.sga.view.comun.Tarea;
import pe.edu.pitagoras.sga.view.comun.Tarjeta;
import pe.edu.pitagoras.sga.view.comun.Tema;

/** RF06: asignación de áreas y secciones a cada docente. */
public class PnlCargaAcademica extends PanelModulo {

    private static final String TODOS = "Todos los docentes";

    private final transient DirectorController controller;
    private final JComboBox<Curso> cboCurso = Combos.nuevo();
    private final JComboBox<Seccion> cboSeccion = Combos.nuevo();
    private final JComboBox<Docente> cboDocente = Combos.nuevo();
    private final JComboBox<Object> cboFiltro = Combos.nuevo();
    private final ModeloTabla<CargaAcademica> modelo = new ModeloTabla<CargaAcademica>()
            .columna("Grado y sección", c -> c.seccion().etiqueta())
            .columna("Turno", c -> c.seccion().turno())
            .columna("Área curricular", c -> c.curso().nombre())
            .columna("Docente", CargaAcademica::docenteNombre);
    private final JTable tabla = new JTable(modelo);
    private final TableRowSorter<ModeloTabla<CargaAcademica>> orden = new TableRowSorter<>(modelo);
    private final javax.swing.JLabel lblDocentes = Tema.subtitulo(" ");
    private List<Docente> docentes = List.of();
    private java.util.Map<Integer, List<Curso>> areas = java.util.Map.of();

    public PnlCargaAcademica(DirectorController controller) {
        super("Carga académica", "Áreas y secciones asignadas a cada docente (RF06)");
        this.controller = controller;

        JPanel form = new JPanel(new GridLayout(1, 3, 14, 0));
        form.setOpaque(false);
        form.add(Tema.campo("Área curricular", cboCurso));
        form.add(Tema.campo("Grado y sección", cboSeccion));
        form.add(Tema.campo("Docente", cboDocente));
        JButton asignar = Tema.botonPrimario("Asignar");
        JPanel filaForm = new JPanel(new BorderLayout(14, 0));
        filaForm.setOpaque(false);
        filaForm.add(form, BorderLayout.CENTER);
        JPanel boton = new JPanel(new BorderLayout());
        boton.setOpaque(false);
        boton.add(asignar, BorderLayout.SOUTH);
        filaForm.add(boton, BorderLayout.EAST);
        Tarjeta cardForm = new Tarjeta();
        cardForm.setLayout(new BorderLayout(0, 10));
        cardForm.add(Tema.seccion("Asignar o reasignar"), BorderLayout.NORTH);
        cardForm.add(filaForm, BorderLayout.CENTER);
        cardForm.add(lblDocentes, BorderLayout.SOUTH);

        cboFiltro.setPreferredSize(new java.awt.Dimension(280, 36));
        JPanel barra = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        barra.setOpaque(false);
        barra.add(Tema.seccion("Carga vigente"));
        barra.add(cboFiltro);
        tabla.setRowSorter(orden);
        Tarjeta cardTabla = new Tarjeta();
        cardTabla.setLayout(new BorderLayout(0, 12));
        cardTabla.add(barra, BorderLayout.NORTH);
        cardTabla.add(Tema.tabla(tabla), BorderLayout.CENTER);

        contenido.add(cardForm, BorderLayout.NORTH);
        contenido.add(cardTabla, BorderLayout.CENTER);

        cboFiltro.addActionListener(e -> filtrar());
        cboCurso.addActionListener(e -> filtrarDocentes());
        asignar.addActionListener(e -> asignar());
        tabla.getSelectionModel().addListSelectionListener(e -> {
            int f = tabla.getSelectedRow();
            if (!e.getValueIsAdjusting() && f >= 0) {
                CargaAcademica c = modelo.getFila(tabla.convertRowIndexToModel(f));
                Combos.seleccionar(cboCurso, x -> x.cursoId() == c.curso().cursoId());
                Combos.seleccionar(cboSeccion, x -> x.seccionId() == c.seccion().seccionId());
                Combos.seleccionar(cboDocente, x -> x.docenteId() == c.docenteId());
            }
        });
    }

    @Override
    protected void alMostrar(boolean primeraVez) {
        Tarea.ejecutar(this, () -> new Object[]{controller.cursos(), controller.secciones(), controller.docentes(),
                controller.cargas(), controller.areasDocentes()}, d -> {
                    @SuppressWarnings("unchecked")
                    List<Curso> c = (List<Curso>) d[0];
                    @SuppressWarnings("unchecked")
                    List<Seccion> s = (List<Seccion>) d[1];
                    @SuppressWarnings("unchecked")
                    List<Docente> doc = (List<Docente>) d[2];
                    @SuppressWarnings("unchecked")
                    List<CargaAcademica> cargas = (List<CargaAcademica>) d[3];
                    @SuppressWarnings("unchecked")
                    java.util.Map<Integer, List<Curso>> a = (java.util.Map<Integer, List<Curso>>) d[4];
                    docentes = doc;
                    areas = a;
                    Combos.llenar(cboCurso, c);
                    Combos.llenar(cboSeccion, s);
                    filtrarDocentes();
                    List<Object> filtro = new ArrayList<>();
                    filtro.add(TODOS);
                    filtro.addAll(doc);
                    Combos.llenar(cboFiltro, filtro);
                    modelo.setFilas(cargas);
                    filtrar();
                });
    }

    /** Solo muestra los docentes cuya especialidad incluye el área elegida. */
    private void filtrarDocentes() {
        Curso c = Combos.seleccionado(cboCurso);
        if (c == null) {
            return;
        }
        List<Docente> aptos = docentes.stream()
                .filter(d -> areas.getOrDefault(d.usuarioId(), List.of()).stream().anyMatch(x -> x.cursoId() == c.cursoId()))
                .toList();
        Combos.llenar(cboDocente, aptos);
        lblDocentes.setText(aptos.isEmpty()
                ? "Ningún docente tiene registrada el área " + c.nombre() + ". Agréguela en Usuarios > Editar datos."
                : aptos.size() + " docente(s) dictan " + c.nombre() + ". Si el área ya tiene docente en esa sección, "
                        + "se reemplaza; las notas registradas se conservan.");
    }

    private void filtrar() {
        Object sel = cboFiltro.getSelectedItem();
        orden.setRowFilter(sel instanceof Docente d
                ? RowFilter.regexFilter("^" + java.util.regex.Pattern.quote(d.nombreCompleto()) + "$", 3) : null);
    }

    private void asignar() {
        Curso c = Combos.seleccionado(cboCurso);
        Seccion s = Combos.seleccionado(cboSeccion);
        Docente d = Combos.seleccionado(cboDocente);
        if (c == null || s == null || d == null) {
            Mensajes.aviso(this, "Seleccione el área, la sección y el docente.");
            return;
        }
        if (Mensajes.confirmar(this, "¿Asignar " + c.nombre() + " de " + s.etiqueta() + " a " + d.nombreCompleto() + "?")) {
            Tarea.ejecutarAccion(this, () -> controller.asignarCarga(c, s, d), () -> {
                Mensajes.info(this, "Carga académica actualizada.");
                alMostrar(false);
            });
        }
    }
}
