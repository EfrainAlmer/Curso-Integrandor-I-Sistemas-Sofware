package pe.edu.pitagoras.sga.view.comun;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import javax.swing.table.AbstractTableModel;

/**
 * Modelo de tabla genérico de solo lectura: cada columna se define con un título y una
 * función que obtiene el valor del objeto de la fila. Evita repetir un TableModel por pantalla.
 *
 * @param <T> tipo de objeto de cada fila.
 */
public class ModeloTabla<T> extends AbstractTableModel {

    private final List<String> titulos = new ArrayList<>();
    private final List<Function<T, Object>> valores = new ArrayList<>();
    private final List<Class<?>> tipos = new ArrayList<>();
    private final List<T> filas = new ArrayList<>();

    /** Agrega la columna "N.°" con el número de fila. */
    public ModeloTabla<T> numero() {
        titulos.add("N.°");
        tipos.add(Integer.class);
        valores.add(null);
        return this;
    }

    /** Agrega una columna de texto. */
    public ModeloTabla<T> columna(String titulo, Function<T, Object> valor) {
        return columna(titulo, Object.class, valor);
    }

    /** Agrega una columna con un tipo (por ejemplo Calificativo.class para que se pinte como insignia). */
    public ModeloTabla<T> columna(String titulo, Class<?> tipo, Function<T, Object> valor) {
        titulos.add(titulo);
        tipos.add(tipo);
        valores.add(valor);
        return this;
    }

    public void setFilas(List<T> nuevas) {
        filas.clear();
        filas.addAll(nuevas);
        fireTableDataChanged();
    }

    public T getFila(int indiceModelo) {
        return filas.get(indiceModelo);
    }

    public List<T> getFilas() {
        return List.copyOf(filas);
    }

    @Override
    public int getRowCount() {
        return filas.size();
    }

    @Override
    public int getColumnCount() {
        return titulos.size();
    }

    @Override
    public String getColumnName(int col) {
        return titulos.get(col);
    }

    @Override
    public Class<?> getColumnClass(int col) {
        return tipos.get(col);
    }

    @Override
    public Object getValueAt(int fila, int col) {
        Function<T, Object> f = valores.get(col);
        return f == null ? fila + 1 : f.apply(filas.get(fila));
    }
}
