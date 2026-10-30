package pe.edu.pitagoras.sga.view.comun;

import com.formdev.flatlaf.FlatClientProperties;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;

/** Utilidades para llenar combos sin perder la selección. */
public final class Combos {

    private Combos() {
    }

    public static <T> JComboBox<T> nuevo() {
        JComboBox<T> c = new JComboBox<>();
        c.putClientProperty(FlatClientProperties.STYLE, "arc:6");
        c.setMaximumRowCount(14);
        return c;
    }

    /** Reemplaza los elementos y conserva la selección si sigue existiendo (por igualdad). */
    public static <T> void llenar(JComboBox<T> combo, List<T> elementos) {
        Object antes = combo.getSelectedItem();
        DefaultComboBoxModel<T> m = new DefaultComboBoxModel<>();
        elementos.forEach(m::addElement);
        combo.setModel(m);
        if (antes != null && elementos.contains(antes)) {
            combo.setSelectedItem(antes);
        }
    }

    /** Selecciona el primer elemento que cumple la condición. */
    public static <T> void seleccionar(JComboBox<T> combo, Predicate<T> condicion) {
        for (int i = 0; i < combo.getItemCount(); i++) {
            if (condicion.test(combo.getItemAt(i))) {
                combo.setSelectedIndex(i);
                return;
            }
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> T seleccionado(JComboBox<T> combo) {
        return (T) combo.getSelectedItem();
    }

    /** @return true si ambos objetos son iguales (útil para comparar selecciones). */
    public static boolean iguales(Object a, Object b) {
        return Objects.equals(a, b);
    }
}
