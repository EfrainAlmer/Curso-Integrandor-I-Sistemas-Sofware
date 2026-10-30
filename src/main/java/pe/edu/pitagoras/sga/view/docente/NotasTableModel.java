package pe.edu.pitagoras.sga.view.docente;

import java.util.ArrayList;
import java.util.List;
import javax.swing.table.AbstractTableModel;
import pe.edu.pitagoras.sga.model.academico.Competencia;
import pe.edu.pitagoras.sga.model.evaluacion.Calificativo;
import pe.edu.pitagoras.sga.model.evaluacion.FilaNotas;

/**
 * Grilla de notas: estudiante, una columna por competencia (editable) y el calificativo
 * del área calculado al momento. Solo acepta valores de la escala AD, A, B, C (RF13).
 */
class NotasTableModel extends AbstractTableModel {

    static final int COLUMNAS_FIJAS = 1;

    private final List<FilaNotas> filas = new ArrayList<>();
    private final List<Competencia> competencias = new ArrayList<>();
    private boolean editable;
    private Runnable alCambiar = () -> { };

    void cargar(List<Competencia> comps, List<FilaNotas> nuevas, boolean sePuedeEditar) {
        competencias.clear();
        competencias.addAll(comps);
        filas.clear();
        filas.addAll(nuevas);
        editable = sePuedeEditar;
        fireTableStructureChanged();
    }

    void setAlCambiar(Runnable r) {
        alCambiar = r;
    }

    List<FilaNotas> getFilas() {
        return filas;
    }

    List<Competencia> getCompetencias() {
        return competencias;
    }

    boolean hayCambios() {
        return filas.stream().anyMatch(FilaNotas::tieneCambios);
    }

    void descartar() {
        filas.forEach(FilaNotas::descartar);
        fireTableDataChanged();
    }

    /** @return alumnos con alguna competencia sin calificativo. */
    long incompletos() {
        return filas.stream().filter(f -> f.getValores().size() < competencias.size()).count();
    }

    @Override
    public int getRowCount() {
        return filas.size();
    }

    @Override
    public int getColumnCount() {
        return COLUMNAS_FIJAS + competencias.size() + 1;
    }

    @Override
    public String getColumnName(int c) {
        if (c == 0) {
            return "Estudiante";
        }
        if (c == getColumnCount() - 1) {
            return "Área";
        }
        // Nombre de la competencia en dos líneas si es largo.
        return "<html>" + competencias.get(c - COLUMNAS_FIJAS).nombre() + "</html>";
    }

    /** @return descripción de la competencia de la columna (para el tooltip de la cabecera). */
    String descripcion(int c) {
        if (c >= COLUMNAS_FIJAS && c < getColumnCount() - 1) {
            return competencias.get(c - COLUMNAS_FIJAS).nombre();
        }
        return c == getColumnCount() - 1 ? "Calificativo del área en el periodo (se calcula)" : null;
    }

    @Override
    public Class<?> getColumnClass(int c) {
        return c >= COLUMNAS_FIJAS ? Calificativo.class : String.class;
    }

    @Override
    public boolean isCellEditable(int f, int c) {
        return editable && c >= COLUMNAS_FIJAS && c < getColumnCount() - 1;
    }

    @Override
    public Object getValueAt(int f, int c) {
        FilaNotas fila = filas.get(f);
        if (c == 0) {
            return fila.getAlumno();
        }
        if (c == getColumnCount() - 1) {
            return fila.calificativoArea().orElse(null);
        }
        return fila.get(competencias.get(c - COLUMNAS_FIJAS).codigo());
    }

    @Override
    public void setValueAt(Object valor, int f, int c) {
        // Validación en tiempo real: cualquier texto se convierte a la escala o se rechaza.
        Calificativo cal = valor instanceof Calificativo k ? k
                : Calificativo.desdeTexto(valor == null ? null : valor.toString()).orElse(null);
        filas.get(f).poner(competencias.get(c - COLUMNAS_FIJAS).codigo(), cal);
        fireTableRowsUpdated(f, f);
        alCambiar.run();
    }

    /** @return true si la celda tiene un valor distinto del guardado (se resalta). */
    boolean modificada(int f, int c) {
        if (c < COLUMNAS_FIJAS || c >= getColumnCount() - 1) {
            return false;
        }
        FilaNotas fila = filas.get(f);
        String codigo = competencias.get(c - COLUMNAS_FIJAS).codigo();
        return !java.util.Objects.equals(fila.get(codigo), fila.getOriginal(codigo));
    }
}
