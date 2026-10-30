package pe.edu.pitagoras.sga.view.comun;

import java.awt.Component;
import java.awt.FlowLayout;
import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;

/**
 * Muestra la celda como una lista desplegable (aunque no se esté editando), para que el usuario
 * vea de inmediato que puede cambiar el valor, igual que en los prototipos del equipo.
 */
public class RenderCombo implements TableCellRenderer {

    private final JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
    private final JComboBox<Object> combo = new JComboBox<>();
    private final String vacio;

    /** @param vacio texto cuando la celda no tiene valor (por ejemplo "—"). */
    public RenderCombo(int ancho, String vacio) {
        this.vacio = vacio;
        combo.setPreferredSize(new java.awt.Dimension(ancho, 30));
        panel.setBorder(BorderFactory.createEmptyBorder(5, 0, 0, 0));
        panel.add(combo);
    }

    @Override
    public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foco, int f, int c) {
        combo.removeAllItems();
        combo.addItem(v == null ? vacio : v);
        panel.setBackground(sel ? t.getSelectionBackground() : t.getBackground());
        return panel;
    }

    /** @return el panel, para cambiar el fondo (resaltar cambios sin guardar). */
    public JPanel getPanel() {
        return panel;
    }
}
