package pe.edu.utp.pitagoras.view.director;

import pe.edu.utp.pitagoras.config.EstilosUI;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

/** CU06 / RF06: asignación de carga académica (grados, secciones y áreas) a cada docente. */
public class PnlCargaAcademica extends JPanel {

    /** Docente disponible para asignar. toString() es lo que se ve en el combo. */
    private record Docente(String dni, String nombre, String especialidad) {
        @Override public String toString() { return nombre + "  ·  " + dni; }
    }

    private static final Docente[] DOCENTES = {
        new Docente("71234567", "Mendoza Ríos, Carlos",    "Matemática"),
        new Docente("72345678", "Vargas Soto, Elena",      "Comunicación"),
        new Docente("73456789", "Quispe Huamán, Jorge",    "Ciencia y Tecnología"),
        new Docente("74567890", "Rojas Poma, Lucía",       "Ciencias Sociales")
    };

    private static final String[] AREAS = {
        "Matemática", "Comunicación", "Ciencia y Tecnología", "Ciencias Sociales",
        "Desarrollo Personal, Ciudadanía y Cívica", "Educación para el Trabajo"
    };

    private static final int COL_ID = 0, COL_DNI = 1, COL_DOCENTE = 2, COL_GRADO_SEC = 3, COL_TURNO = 4, COL_AREA = 5;

    private JComboBox<Docente> cboDocente;
    private JComboBox<String> cboGrado, cboSeccion, cboTurno, cboArea;
    private JLabel lblTotal;
    private DefaultTableModel modeloCarga;
    private JTable tablaCarga;

    public PnlCargaAcademica() {
        initComponents();
    }

    private void initComponents() {
        setLayout(null);
        setBackground(EstilosUI.BG_MAIN);
        setPreferredSize(new Dimension(980, 700));

        EstilosUI.encabezado(this, "Asignación de Carga Académica",
                "Año escolar 2026 · Vinculación de docentes con grados, secciones y áreas curriculares (RF06)");

        // ── Formulario ────────────────────────────────────────────
        JPanel cardForm = EstilosUI.card(32, 92, 916, 155);

        cardForm.add(EstilosUI.etiquetaCampo("DOCENTE TITULAR", 20, 16, 200));
        cboDocente = EstilosUI.combo(DOCENTES);
        cboDocente.setBounds(20, 36, 310, 36);
        cardForm.add(cboDocente);

        cardForm.add(EstilosUI.etiquetaCampo("GRADO SECUNDARIA", 346, 16, 140));
        cboGrado = EstilosUI.combo(new String[]{"1.°", "2.°", "3.°", "4.°", "5.°"});
        cboGrado.setBounds(346, 36, 110, 36);
        cardForm.add(cboGrado);

        cardForm.add(EstilosUI.etiquetaCampo("SECCIÓN", 472, 16, 90));
        cboSeccion = EstilosUI.combo(new String[]{"A", "B"});
        cboSeccion.setBounds(472, 36, 90, 36);
        cardForm.add(cboSeccion);

        cardForm.add(EstilosUI.etiquetaCampo("TURNO", 578, 16, 110));
        cboTurno = EstilosUI.combo(new String[]{"Mañana", "Tarde"});
        cboTurno.setBounds(578, 36, 115, 36);
        cardForm.add(cboTurno);

        cardForm.add(EstilosUI.etiquetaCampo("ÁREA CURRICULAR (MINEDU)", 20, 82, 220));
        cboArea = EstilosUI.combo(AREAS);
        cboArea.setBounds(20, 102, 310, 36);
        cardForm.add(cboArea);

        JButton btnAsignar = EstilosUI.botonPrimario("Asignar Carga Docente");
        btnAsignar.setBounds(346, 102, 216, 36);
        btnAsignar.addActionListener(e -> accionAsignarCarga());
        cardForm.add(btnAsignar);

        JButton btnEliminar = EstilosUI.botonSecundario("Quitar asignación seleccionada", EstilosUI.DANGER_RED);
        btnEliminar.setBounds(578, 102, 250, 36);
        btnEliminar.addActionListener(e -> accionEliminarCarga());
        cardForm.add(btnEliminar);

        add(cardForm);

        // ── Tabla ─────────────────────────────────────────────────
        JPanel cardTabla = EstilosUI.card(32, 265, 916, 390);
        cardTabla.add(EstilosUI.label("Distribución de Carga Académica Registrada — Año 2026",
                20, 16, 560, 22, 15, Font.BOLD, EstilosUI.TEXT_DARK));

        lblTotal = EstilosUI.label("", 616, 18, 280, 18, 12, Font.PLAIN, EstilosUI.TEXT_GRAY);
        lblTotal.setHorizontalAlignment(SwingConstants.RIGHT);
        cardTabla.add(lblTotal);

        String[] cols = {"ID Carga", "DNI Docente", "Docente Asignado", "Grado y Sección", "Turno", "Área Curricular"};
        Object[][] datos = {
            {"CA-01", "71234567", "Mendoza Ríos, Carlos",  "3.° A", "Mañana", "Matemática"},
            {"CA-02", "71234567", "Mendoza Ríos, Carlos",  "4.° A", "Mañana", "Matemática"},
            {"CA-03", "72345678", "Vargas Soto, Elena",    "3.° A", "Mañana", "Comunicación"},
            {"CA-04", "73456789", "Quispe Huamán, Jorge",  "5.° B", "Tarde",  "Ciencia y Tecnología"}
        };
        modeloCarga = new DefaultTableModel(datos, cols) {
            @Override public boolean isCellEditable(int row, int col) { return false; }
        };
        tablaCarga = EstilosUI.tabla(modeloCarga);
        EstilosUI.anchos(tablaCarga, 70, 90, 210, 110, 90, 230);
        EstilosUI.centrar(tablaCarga, COL_ID, COL_DNI, COL_GRADO_SEC, COL_TURNO);
        cardTabla.add(EstilosUI.scroll(tablaCarga, 20, 50, 876, 318));
        add(cardTabla);

        // ── Comportamiento ────────────────────────────────────────
        // Al elegir docente se propone su área; al elegir sección se propone el turno que ya tiene.
        cboDocente.addActionListener(e -> sugerirAreaDelDocente());
        cboGrado.addActionListener(e -> sugerirTurnoDeLaSeccion());
        cboSeccion.addActionListener(e -> sugerirTurnoDeLaSeccion());
        sugerirAreaDelDocente();
        sugerirTurnoDeLaSeccion();
        actualizarTotal();
    }

    // ── Acciones ──────────────────────────────────────────────────
    private void accionAsignarCarga() {
        Docente doc = (Docente) cboDocente.getSelectedItem();
        String gradoSec = cboGrado.getSelectedItem() + " " + cboSeccion.getSelectedItem();
        String turno = (String) cboTurno.getSelectedItem();
        String area = (String) cboArea.getSelectedItem();

        // 1) Un área en una misma sección no puede tener dos docentes.
        for (int i = 0; i < modeloCarga.getRowCount(); i++) {
            if (valor(i, COL_GRADO_SEC).equals(gradoSec) && valor(i, COL_AREA).equals(area)) {
                EstilosUI.aviso(this, "Carga duplicada",
                        "El área de " + area + " en la sección " + gradoSec
                        + " ya tiene asignado a " + valor(i, COL_DOCENTE) + ".");
                return;
            }
        }

        // 2) Una sección solo funciona en un turno.
        String turnoExistente = turnoDeSeccion(gradoSec);
        if (turnoExistente != null && !turnoExistente.equals(turno)) {
            EstilosUI.aviso(this, "Turno incoherente",
                    "La sección " + gradoSec + " ya funciona en el turno " + turnoExistente
                    + ".\nUsa el mismo turno para asignar nuevas áreas a esa sección.");
            return;
        }

        // 3) Área distinta a la especialidad del docente: se permite, pero con confirmación.
        if (!area.equals(doc.especialidad())) {
            int r = JOptionPane.showConfirmDialog(this,
                    doc.nombre() + " figura con especialidad en " + doc.especialidad()
                    + ".\n¿Deseas asignarle igualmente el área de " + area + "?",
                    "Área distinta a la especialidad", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
            if (r != JOptionPane.YES_OPTION) return;
        }

        modeloCarga.addRow(new Object[]{ siguienteId(), doc.dni(), doc.nombre(), gradoSec, turno, area });
        int ultima = modeloCarga.getRowCount() - 1;
        tablaCarga.setRowSelectionInterval(ultima, ultima);
        tablaCarga.scrollRectToVisible(tablaCarga.getCellRect(ultima, 0, true));
        actualizarTotal();

        EstilosUI.info(this, "Registro exitoso", "Carga académica asignada correctamente a " + doc.nombre() + ".");
    }

    private void accionEliminarCarga() {
        int fila = tablaCarga.getSelectedRow();
        if (fila == -1) {
            EstilosUI.aviso(this, "Selección requerida", "Selecciona una fila de la tabla para quitar la asignación.");
            return;
        }
        String idCarga = valor(fila, COL_ID);
        String detalle = valor(fila, COL_AREA) + " · " + valor(fila, COL_GRADO_SEC) + " · " + valor(fila, COL_DOCENTE);
        int conf = JOptionPane.showConfirmDialog(this,
                "¿Deseas eliminar la asignación " + idCarga + "?\n" + detalle,
                "Confirmar eliminación", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (conf == JOptionPane.YES_OPTION) {
            modeloCarga.removeRow(fila);
            actualizarTotal();
        }
    }

    // ── Utilidades ────────────────────────────────────────────────
    private String valor(int fila, int col) {
        return modeloCarga.getValueAt(fila, col).toString();
    }

    /** ID basado en el mayor existente (no en el nº de filas) para no repetir IDs tras eliminar. */
    private String siguienteId() {
        int max = 0;
        for (int i = 0; i < modeloCarga.getRowCount(); i++) {
            try { max = Math.max(max, Integer.parseInt(valor(i, COL_ID).substring(3))); }
            catch (NumberFormatException ignorada) { }
        }
        return String.format("CA-%02d", max + 1);
    }

    private String turnoDeSeccion(String gradoSec) {
        for (int i = 0; i < modeloCarga.getRowCount(); i++) {
            if (valor(i, COL_GRADO_SEC).equals(gradoSec)) return valor(i, COL_TURNO);
        }
        return null;
    }

    private void sugerirAreaDelDocente() {
        Docente d = (Docente) cboDocente.getSelectedItem();
        if (d != null) cboArea.setSelectedItem(d.especialidad());
    }

    private void sugerirTurnoDeLaSeccion() {
        if (cboGrado.getSelectedItem() == null || cboSeccion.getSelectedItem() == null) return;
        String turno = turnoDeSeccion(cboGrado.getSelectedItem() + " " + cboSeccion.getSelectedItem());
        if (turno != null) cboTurno.setSelectedItem(turno);
    }

    private void actualizarTotal() {
        int n = modeloCarga.getRowCount();
        lblTotal.setText(n + (n == 1 ? " asignación registrada" : " asignaciones registradas"));
    }
}
