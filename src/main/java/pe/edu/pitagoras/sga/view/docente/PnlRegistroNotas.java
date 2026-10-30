package pe.edu.pitagoras.sga.view.docente;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.util.List;
import javax.swing.DefaultCellEditor;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;
import pe.edu.pitagoras.sga.controller.DocenteController;
import pe.edu.pitagoras.sga.model.academico.CargaAcademica;
import pe.edu.pitagoras.sga.model.academico.Competencia;
import pe.edu.pitagoras.sga.model.academico.Periodo;
import pe.edu.pitagoras.sga.model.evaluacion.Calificativo;
import pe.edu.pitagoras.sga.model.evaluacion.FilaNotas;
import pe.edu.pitagoras.sga.view.comun.Combos;
import pe.edu.pitagoras.sga.view.comun.Mensajes;
import pe.edu.pitagoras.sga.view.comun.PanelModulo;
import pe.edu.pitagoras.sga.view.comun.RenderCombo;
import pe.edu.pitagoras.sga.view.comun.RenderInsignia;
import pe.edu.pitagoras.sga.view.comun.Tarea;
import pe.edu.pitagoras.sga.view.comun.Tema;

/**
 * RF13: registro de calificativos por estudiante, competencia y bimestre en la escala AD, A, B, C.
 * Cada celda es una lista desplegable (solo acepta valores de la escala), el calificativo del área
 * se calcula al momento y se avisa de los cambios sin guardar. Si el bimestre está cerrado (RF07)
 * la grilla queda en solo lectura.
 */
public class PnlRegistroNotas extends PanelModulo {

    private final transient DocenteController controller;
    private final JComboBox<Periodo> cboPeriodo = Combos.nuevo();
    private final SelectorCarga selector = new SelectorCarga();
    private final NotasTableModel modelo = new NotasTableModel();
    private final JTable tabla = new JTable(modelo);
    private final JLabel lblEstado = new JLabel(" ");
    private final JLabel lblAvance = Tema.subtitulo(" ");
    private final JLabel lblCambios = new JLabel(" ");
    private final JButton btnGuardar = Tema.botonPrimario("Guardar calificativos");
    private final JButton btnDescartar = Tema.botonSecundario("Descartar");
    private final JButton btnConclusiones = Tema.botonSecundario("Conclusiones descriptivas");
    private boolean cargando;
    private CargaAcademica cargaMostrada;
    private Periodo periodoMostrado;

    public PnlRegistroNotas(DocenteController controller) {
        super("Registro de calificaciones", "Un calificativo (AD, A, B, C) por estudiante, competencia y periodo");
        this.controller = controller;

        cboPeriodo.setPreferredSize(new Dimension(190, 36));
        JPanel filtros = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        filtros.setOpaque(false);
        filtros.add(Tema.campo("Periodo", cboPeriodo));
        filtros.add(espacio());
        filtros.add(Tema.campo("Sección", selector.cboSeccion));
        filtros.add(espacio());
        filtros.add(Tema.campo("Área", selector.cboArea));
        filtros.add(espacio());
        JPanel estado = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        estado.setOpaque(false);
        estado.add(lblEstado);
        estado.add(lblAvance);
        JPanel norte = new JPanel(new BorderLayout(0, 12));
        norte.setOpaque(false);
        norte.add(filtros, BorderLayout.NORTH);
        norte.add(estado, BorderLayout.SOUTH);

        lblCambios.setForeground(Tema.ALERTA);
        JPanel botones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        botones.setOpaque(false);
        botones.add(btnConclusiones);
        botones.add(btnDescartar);
        botones.add(btnGuardar);
        JPanel sur = new JPanel(new BorderLayout());
        sur.setOpaque(false);
        sur.add(lblCambios, BorderLayout.WEST);
        sur.add(botones, BorderLayout.EAST);

        tabla.setRowHeight(46);
        tabla.setSurrendersFocusOnKeystroke(true);
        var scroll = Tema.tabla(tabla);
        tabla.getTableHeader().setPreferredSize(new Dimension(0, 80));
        contenido.add(norte, BorderLayout.NORTH);
        contenido.add(scroll, BorderLayout.CENTER);
        contenido.add(sur, BorderLayout.SOUTH);

        modelo.setAlCambiar(this::actualizarEstado);
        cboPeriodo.addActionListener(e -> cambiarFiltro());
        selector.setAlCambiar(this::cambiarFiltro);
        btnGuardar.addActionListener(e -> guardar());
        btnConclusiones.addActionListener(e -> abrirConclusiones());
        tabla.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2 && tabla.columnAtPoint(e.getPoint()) == 0) {
                    abrirConclusiones();
                }
            }
        });
        btnDescartar.addActionListener(e -> {
            modelo.descartar();
            actualizarEstado();
        });
    }

    private static Component espacio() {
        return javax.swing.Box.createHorizontalStrut(16);
    }

    @Override
    protected void alMostrar(boolean primeraVez) {
        if (!primeraVez) {
            return;
        }
        cargando = true;
        Tarea.ejecutar(this, () -> new Object[]{controller.misCargas(), controller.periodos(), controller.periodoActual()},
                datos -> {
                    @SuppressWarnings("unchecked")
                    List<CargaAcademica> cargas = (List<CargaAcademica>) datos[0];
                    @SuppressWarnings("unchecked")
                    List<Periodo> periodos = (List<Periodo>) datos[1];
                    Combos.llenar(cboPeriodo, periodos);
                    cboPeriodo.setSelectedItem(datos[2]);
                    selector.setCargas(cargas);
                    cargando = false;
                    cargar();
                });
    }

    @Override
    public boolean tieneCambiosSinGuardar() {
        return modelo.hayCambios();
    }

    /** Alerta de cambios sin guardar al cambiar de periodo, sección o área (RF13). */
    private void cambiarFiltro() {
        if (cargando) {
            return;
        }
        if (modelo.hayCambios() && !Mensajes.confirmar(this, "Hay calificativos sin guardar. ¿Desea descartarlos?")) {
            cargando = true;
            cboPeriodo.setSelectedItem(periodoMostrado);
            selector.seleccionar(cargaMostrada);
            cargando = false;
            return;
        }
        cargar();
    }

    private void cargar() {
        CargaAcademica carga = selector.getCarga();
        Periodo periodo = Combos.seleccionado(cboPeriodo);
        if (carga == null || periodo == null) {
            return;
        }
        Tarea.ejecutar(this, () -> new Object[]{controller.competencias(carga), controller.notas(carga, periodo)},
                datos -> {
                    @SuppressWarnings("unchecked")
                    List<Competencia> comps = (List<Competencia>) datos[0];
                    @SuppressWarnings("unchecked")
                    List<FilaNotas> filas = (List<FilaNotas>) datos[1];
                    cargaMostrada = carga;
                    periodoMostrado = periodo;
                    modelo.cargar(comps, filas, !periodo.cerrado());
                    configurarColumnas(!periodo.cerrado());
                    actualizarEstado();
                });
    }

    private void configurarColumnas(boolean editable) {
        tabla.getColumnModel().getColumn(0).setPreferredWidth(220);
        JComboBox<Calificativo> editor = new JComboBox<>(new Calificativo[]{null, Calificativo.AD, Calificativo.A,
                Calificativo.B, Calificativo.C});
        TableCellRenderer render = editable ? new RenderCeldaNota() : new RenderInsignia(true);
        int ultima = modelo.getColumnCount() - 1;
        for (int c = NotasTableModel.COLUMNAS_FIJAS; c <= ultima; c++) {
            var col = tabla.getColumnModel().getColumn(c);
            col.setPreferredWidth(200);
            if (c < ultima) {
                col.setCellEditor(new DefaultCellEditor(editor));
                col.setCellRenderer(render);
            } else {
                col.setCellRenderer(new RenderInsignia(true));
                col.setPreferredWidth(80);
            }
        }
    }

    /** Abre las conclusiones descriptivas del estudiante seleccionado. */
    private void abrirConclusiones() {
        int f = tabla.getSelectedRow();
        if (f < 0 || cargaMostrada == null) {
            Mensajes.aviso(this, "Seleccione un estudiante de la lista (o haga doble clic en su nombre).");
            return;
        }
        if (modelo.hayCambios()) {
            Mensajes.aviso(this, "Guarde o descarte primero los calificativos modificados.");
            return;
        }
        FilaNotas fila = modelo.getFilas().get(tabla.convertRowIndexToModel(f));
        CargaAcademica carga = cargaMostrada;
        Periodo periodo = periodoMostrado;
        List<Competencia> comps = List.copyOf(modelo.getCompetencias());
        Tarea.ejecutar(this, () -> controller.conclusiones(carga, periodo, fila.getAlumnoId()),
                textos -> new DialogoConclusiones(this, controller, carga, periodo, comps, fila, textos).setVisible(true));
    }

    private void actualizarEstado() {
        Periodo p = periodoMostrado;
        if (p == null) {
            return;
        }
        lblEstado.setOpaque(true);
        lblEstado.setText(p.cerrado() ? "  Bimestre cerrado: solo lectura  " : "  Bimestre abierto  ");
        lblEstado.setBackground(p.cerrado() ? Tema.ALERTA_SUAVE : Tema.EXITO_SUAVE);
        lblEstado.setForeground(p.cerrado() ? Tema.ALERTA : Tema.EXITO);
        long faltan = modelo.incompletos();
        lblAvance.setText((faltan == 0 ? "Todos los estudiantes tienen sus calificativos completos."
                : faltan + " de " + modelo.getRowCount() + " estudiantes con calificativos pendientes.")
                + "  Doble clic en un estudiante para escribir sus conclusiones descriptivas.");
        boolean cambios = modelo.hayCambios();
        lblCambios.setText(cambios ? "Hay cambios sin guardar" : " ");
        btnGuardar.setEnabled(cambios && !p.cerrado());
        btnDescartar.setEnabled(cambios);
        tabla.repaint();
    }

    private void guardar() {
        if (tabla.isEditing()) {
            tabla.getCellEditor().stopCellEditing();
        }
        CargaAcademica carga = cargaMostrada;
        Periodo periodo = periodoMostrado;
        List<FilaNotas> filas = modelo.getFilas();
        btnGuardar.setEnabled(false);
        Tarea.ejecutar(this, () -> controller.guardarNotas(carga, periodo, filas), n -> {
            Mensajes.info(this, n + " calificativo(s) guardado(s). Las modificaciones quedaron en la bitácora.");
            cargar();
        }, this::actualizarEstado);
    }

    /** Celda con aspecto de lista desplegable; si el valor cambió y no se guardó, fondo ámbar. */
    private class RenderCeldaNota implements TableCellRenderer {
        private final RenderCombo combo = new RenderCombo(70, "—");

        @Override
        public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foco, int f, int c) {
            Component comp = combo.getTableCellRendererComponent(t, v, sel, foco, f, c);
            if (modelo.modificada(t.convertRowIndexToModel(f), t.convertColumnIndexToModel(c))) {
                combo.getPanel().setBackground(new Color(0xFFF4D6));
            }
            return comp;
        }
    }
}
