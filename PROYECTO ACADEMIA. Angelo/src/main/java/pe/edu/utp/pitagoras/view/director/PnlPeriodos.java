package pe.edu.utp.pitagoras.view.director;

import pe.edu.utp.pitagoras.config.EstilosUI;
import pe.edu.utp.pitagoras.temporal.EstadoSistema;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDate;

/** CU07 / RF07: cierre y reapertura de periodos de evaluación, con registro en la bitácora. */
public class PnlPeriodos extends JPanel {

    private static final int COL_ID = 0, COL_PERIODO = 1, COL_INICIO = 2, COL_FIN = 3,
                             COL_ESTADO = 4, COL_CIERRE = 5, COL_CERRADO_POR = 6;

    private DefaultTableModel modeloPeriodos;
    private JTable tablaPeriodos;
    private JLabel lblPeriodoActivoCard;

    public PnlPeriodos() {
        initComponents();
        // Si otro módulo (p. ej. el botón "Cerrar periodo" del Dashboard) cambia un periodo, esta vista se refresca.
        EstadoSistema.addOyentePeriodos(() -> SwingUtilities.invokeLater(this::recargar));
    }

    private void initComponents() {
        setLayout(null);
        setBackground(EstilosUI.BG_MAIN);
        setPreferredSize(new Dimension(980, 700));

        EstilosUI.encabezado(this, "Gestión de Periodos de Evaluación",
                "Año escolar 2026 · Control de apertura, cierre y bloqueo de registro de notas y asistencias (RF07)");

        // ── Tarjetas de resumen ───────────────────────────────────
        JPanel cardAnio = EstilosUI.card(32, 92, 280, 95);
        cardAnio.add(EstilosUI.label("Año escolar activo", 18, 14, 200, 18, 12, Font.PLAIN, EstilosUI.TEXT_GRAY));
        cardAnio.add(EstilosUI.label("2026 (Mar - Dic)", 18, 38, 244, 32, 22, Font.BOLD, EstilosUI.TEXT_DARK));
        add(cardAnio);

        JPanel cardActual = EstilosUI.card(330, 92, 290, 95);
        cardActual.add(EstilosUI.label("Periodo en curso", 18, 14, 200, 18, 12, Font.PLAIN, EstilosUI.TEXT_GRAY));
        lblPeriodoActivoCard = EstilosUI.label("", 18, 38, 254, 32, 20, Font.BOLD, EstilosUI.SUCCESS_GREEN);
        cardActual.add(lblPeriodoActivoCard);
        add(cardActual);

        JPanel cardRegla = EstilosUI.card(638, 92, 310, 95);
        cardRegla.add(EstilosUI.label("Regla de calificación anual (MINEDU)", 18, 14, 274, 18, 12, Font.PLAIN, EstilosUI.TEXT_GRAY));
        cardRegla.add(EstilosUI.label("Prevalece Bimestre 4", 18, 38, 274, 32, 20, Font.BOLD, EstilosUI.SIDEBAR_BG));
        add(cardRegla);

        // ── Tabla de bimestres ────────────────────────────────────
        JPanel cardTabla = EstilosUI.card(32, 205, 916, 445);
        cardTabla.add(EstilosUI.label("Cronograma de Bimestres — Nivel Secundario", 20, 18, 410, 22, 15, Font.BOLD, EstilosUI.TEXT_DARK));

        JButton btnCerrarPer = EstilosUI.botonPrimario("Cerrar periodo seleccionado", EstilosUI.DANGER_RED, EstilosUI.DANGER_HOVER);
        btnCerrarPer.setBounds(440, 14, 220, 34);
        btnCerrarPer.addActionListener(e -> cerrarPeriodoSeleccionado());
        cardTabla.add(btnCerrarPer);

        JButton btnReabrirPer = EstilosUI.botonSecundario("Reabrir periodo");
        btnReabrirPer.setBounds(672, 14, 224, 34);
        btnReabrirPer.addActionListener(e -> reabrirPeriodoSeleccionado());
        cardTabla.add(btnReabrirPer);

        String[] cols = {"ID", "Periodo", "Fecha Inicio", "Fecha Fin", "Estado", "Fecha de Cierre", "Cerrado Por"};
        modeloPeriodos = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int row, int col) { return false; }
        };
        tablaPeriodos = EstilosUI.tabla(modeloPeriodos);
        EstilosUI.anchos(tablaPeriodos, 40, 110, 110, 110, 100, 150, 130);
        EstilosUI.centrar(tablaPeriodos, COL_ID, COL_INICIO, COL_FIN, COL_CIERRE);
        tablaPeriodos.getColumnModel().getColumn(COL_ESTADO).setCellRenderer(new EstadoRenderer());
        cardTabla.add(EstilosUI.scroll(tablaPeriodos, 20, 62, 876, 260));

        JLabel lblAviso = new JLabel(
            "<html><b>Nota de seguridad (RF07):</b> Al cerrar un periodo se bloquea inmediatamente la edición de calificativos "
            + "(AD, A, B, C) y asistencias para todos los docentes. Cualquier reapertura exige justificación y queda "
            + "registrada automáticamente en la Bitácora de Auditoría.</html>");
        lblAviso.setFont(EstilosUI.font(Font.PLAIN, 12));
        lblAviso.setForeground(EstilosUI.WARNING_AMBER);
        lblAviso.setBounds(20, 340, 876, 48);
        cardTabla.add(lblAviso);

        add(cardTabla);
        recargar();
    }

    // ── Acciones ──────────────────────────────────────────────────
    private void cerrarPeriodoSeleccionado() {
        EstadoSistema.Periodo p = periodoSeleccionado();
        if (p == null) return;

        if (EstadoSistema.CERRADO.equals(p.estado)) {
            EstilosUI.info(this, "Periodo cerrado", "El " + p.nombre + " ya se encuentra cerrado.");
            return;
        }
        if (EstadoSistema.PENDIENTE.equals(p.estado)) {
            EstilosUI.aviso(this, "Periodo no iniciado",
                    "El " + p.nombre + " aún no está abierto, por lo que no puede cerrarse.");
            return;
        }

        String msg = "¿Confirmas el cierre del " + p.nombre + "?\nSe bloqueará la edición de notas y asistencias.";
        if (LocalDate.now().isBefore(p.fin)) {
            msg += "\n\nAtención: el periodo termina el " + p.fin.format(EstadoSistema.FMT_FECHA)
                 + " y todavía no vence.";
        }
        int conf = JOptionPane.showConfirmDialog(this, msg, "Confirmar cierre",
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (conf != JOptionPane.YES_OPTION) return;

        EstadoSistema.EventoBitacora ev = EstadoSistema.cerrarPeriodo(p);   // actualiza estado + bitácora
        EstilosUI.info(this, "Periodo cerrado",
                p.nombre + " cerrado correctamente.\nRegistrado en la Bitácora de Auditoría (evento N.° " + ev.id + ").");
    }

    private void reabrirPeriodoSeleccionado() {
        EstadoSistema.Periodo p = periodoSeleccionado();
        if (p == null) return;

        if (EstadoSistema.ABIERTO.equals(p.estado)) {
            EstilosUI.info(this, "Periodo abierto", "El " + p.nombre + " ya está abierto.");
            return;
        }
        if (EstadoSistema.PENDIENTE.equals(p.estado)) {
            EstilosUI.aviso(this, "Periodo no iniciado",
                    "El " + p.nombre + " todavía no ha iniciado, no corresponde reabrirlo.");
            return;
        }

        String motivo;
        while (true) {
            motivo = JOptionPane.showInputDialog(this,
                    "Ingresa el motivo de reapertura del " + p.nombre + " (obligatorio por auditoría, mín. 10 caracteres):",
                    "Reabrir periodo", JOptionPane.QUESTION_MESSAGE);
            if (motivo == null) return;                       // canceló
            motivo = motivo.trim();
            if (motivo.length() >= 10) break;
            EstilosUI.aviso(this, "Motivo insuficiente", "Describe el motivo con al menos 10 caracteres.");
        }

        EstadoSistema.EventoBitacora ev = EstadoSistema.reabrirPeriodo(p, motivo);
        EstilosUI.info(this, "Periodo reabierto",
                p.nombre + " reabierto exitosamente.\nRegistrado en la Bitácora de Auditoría (evento N.° " + ev.id + ").");
    }

    private EstadoSistema.Periodo periodoSeleccionado() {
        int fila = tablaPeriodos.getSelectedRow();
        if (fila == -1) {
            EstilosUI.aviso(this, "Selección requerida", "Selecciona un bimestre de la tabla primero.");
            return null;
        }
        return EstadoSistema.getPeriodos().get(tablaPeriodos.convertRowIndexToModel(fila));
    }

    // ── Refresco de la vista a partir del estado ──────────────────
    private void recargar() {
        int seleccion = tablaPeriodos.getSelectedRow();
        modeloPeriodos.setRowCount(0);
        for (EstadoSistema.Periodo p : EstadoSistema.getPeriodos()) {
            modeloPeriodos.addRow(new Object[]{
                String.valueOf(p.id), p.nombre,
                p.inicio.format(EstadoSistema.FMT_FECHA), p.fin.format(EstadoSistema.FMT_FECHA),
                p.estado, p.fechaCierre, p.cerradoPor
            });
        }
        if (seleccion >= 0 && seleccion < modeloPeriodos.getRowCount()) {
            tablaPeriodos.setRowSelectionInterval(seleccion, seleccion);
        }

        EstadoSistema.Periodo enCurso = EstadoSistema.periodoEnCurso();
        if (enCurso != null) {
            lblPeriodoActivoCard.setText(enCurso.nombre + " (Abierto)");
            lblPeriodoActivoCard.setForeground(EstilosUI.SUCCESS_GREEN);
        } else {
            lblPeriodoActivoCard.setText("Sin periodo abierto");
            lblPeriodoActivoCard.setForeground(EstilosUI.DANGER_RED);
        }
    }

    /** Colorea el estado: Abierto (verde), Cerrado (rojo), Pendiente (gris). */
    private static class EstadoRenderer extends DefaultTableCellRenderer {
        EstadoRenderer() {
            setBorder(new EmptyBorder(0, 10, 0, 10));
            setHorizontalAlignment(SwingConstants.LEFT);
        }
        @Override
        public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foc, int r, int c) {
            super.getTableCellRendererComponent(t, v, sel, false, r, c);
            String estado = String.valueOf(v);
            setFont(EstilosUI.font(Font.BOLD, 13));
            if (EstadoSistema.ABIERTO.equals(estado))      setForeground(EstilosUI.SUCCESS_GREEN);
            else if (EstadoSistema.CERRADO.equals(estado)) setForeground(EstilosUI.DANGER_RED);
            else                                           setForeground(EstilosUI.TEXT_GRAY);
            return this;
        }
    }
}
