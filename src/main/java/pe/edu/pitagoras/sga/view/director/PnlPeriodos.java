package pe.edu.pitagoras.sga.view.director;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTable;
import pe.edu.pitagoras.sga.controller.DirectorController;
import pe.edu.pitagoras.sga.model.academico.Periodo;
import pe.edu.pitagoras.sga.view.comun.DialogoFormulario;
import pe.edu.pitagoras.sga.view.comun.Fechas;
import pe.edu.pitagoras.sga.view.comun.Mensajes;
import pe.edu.pitagoras.sga.view.comun.ModeloTabla;
import pe.edu.pitagoras.sga.view.comun.PanelModulo;
import pe.edu.pitagoras.sga.view.comun.RenderInsignia;
import pe.edu.pitagoras.sga.view.comun.Tarea;
import pe.edu.pitagoras.sga.view.comun.Tarjeta;
import pe.edu.pitagoras.sga.view.comun.Tema;

/**
 * RF05 y RF07: fechas de los bimestres, cierre (bloquea notas y asistencia y guarda el
 * consolidado) y reapertura con motivo registrado en la bitácora.
 */
public class PnlPeriodos extends PanelModulo {

    private final transient DirectorController controller;
    private final ModeloTabla<Periodo> modelo = new ModeloTabla<Periodo>()
            .columna("Periodo", p -> "Bimestre " + p.bimestre())
            .columna("Año", Integer.class, Periodo::anioLectivo)
            .columna("Inicio", p -> Fechas.texto(p.fechaInicio()))
            .columna("Fin", p -> Fechas.texto(p.fechaFin()))
            .columna("Estado", String.class, p -> p.cerrado() ? "Cerrado" : "Abierto")
            .columna("Cerrado por", p -> p.cerradoPor() == null ? "-" : p.cerradoPor())
            .columna("Fecha de cierre", p -> p.fechaCierre() == null ? "-" : Fechas.texto(p.fechaCierre()));
    private final JTable tabla = new JTable(modelo);

    public PnlPeriodos(DirectorController controller) {
        super("Periodos", "Bimestres del año escolar: fechas, cierre y reapertura (RF05, RF07)");
        this.controller = controller;
        JButton fechas = Tema.botonSecundario("Editar fechas");
        JButton cerrar = Tema.botonPrimario("Cerrar periodo");
        JButton reabrir = Tema.botonPeligro("Reabrir periodo");
        JPanel botones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        botones.setOpaque(false);
        botones.add(fechas);
        botones.add(reabrir);
        botones.add(cerrar);

        tabla.getColumnModel().getColumn(4).setCellRenderer(new RenderInsignia(true));
        Tarjeta card = new Tarjeta();
        card.setLayout(new BorderLayout(0, 12));
        card.add(botones, BorderLayout.NORTH);
        card.add(Tema.tabla(tabla), BorderLayout.CENTER);
        card.add(Tema.subtitulo("<html>Al cerrar un bimestre se bloquea la edición de notas y asistencia y se guarda el "
                + "consolidado de cada alumno por área.<br>Reabrir exige indicar el motivo, que queda en la bitácora "
                + "de auditoría.</html>"), BorderLayout.SOUTH);
        contenido.add(card, BorderLayout.CENTER);

        fechas.addActionListener(e -> editarFechas());
        cerrar.addActionListener(e -> cerrar());
        reabrir.addActionListener(e -> reabrir());
    }

    @Override
    protected void alMostrar(boolean primeraVez) {
        Tarea.ejecutar(this, controller::periodos, modelo::setFilas);
    }

    private Periodo seleccionado() {
        int f = tabla.getSelectedRow();
        if (f < 0) {
            Mensajes.aviso(this, "Seleccione un periodo de la lista.");
            return null;
        }
        return modelo.getFila(tabla.convertRowIndexToModel(f));
    }

    private void editarFechas() {
        Periodo p = seleccionado();
        if (p == null) {
            return;
        }
        JSpinner inicio = Fechas.selector(p.fechaInicio());
        JSpinner fin = Fechas.selector(p.fechaFin());
        new DialogoFormulario(this, "Fechas del " + p.etiqueta(), null)
                .campo("Inicio", inicio).campo("Fin", fin)
                .mostrar(() -> {
                    controller.actualizarFechas(p, Fechas.leer(inicio), Fechas.leer(fin));
                    return Boolean.TRUE;
                }, ok -> alMostrar(false));
    }

    private void cerrar() {
        Periodo p = seleccionado();
        if (p == null) {
            return;
        }
        if (p.cerrado()) {
            Mensajes.aviso(this, "El " + p.etiqueta() + " ya está cerrado.");
            return;
        }
        if (Mensajes.confirmar(this, "¿Cerrar el " + p.etiqueta() + "?\n\nLos docentes ya no podrán modificar notas ni "
                + "asistencia de este bimestre.\nRevise antes el avance del registro en el Panel de indicadores.")) {
            Tarea.ejecutar(this, () -> controller.cerrarPeriodo(p), n -> {
                Mensajes.info(this, "Periodo cerrado. Se consolidaron " + n + " registros (alumno por área).");
                alMostrar(false);
            });
        }
    }

    private void reabrir() {
        Periodo p = seleccionado();
        if (p == null) {
            return;
        }
        if (!p.cerrado()) {
            Mensajes.aviso(this, "El " + p.etiqueta() + " ya está abierto.");
            return;
        }
        String motivo = JOptionPane.showInputDialog(this, "Indique el motivo de la reapertura (queda en la bitácora):",
                "Reabrir " + p.etiqueta(), JOptionPane.QUESTION_MESSAGE);
        if (motivo != null) {
            Tarea.ejecutarAccion(this, () -> controller.reabrirPeriodo(p, motivo), () -> {
                Mensajes.info(this, "Periodo reabierto.");
                alMostrar(false);
            });
        }
    }
}
