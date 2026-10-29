package pe.edu.utp.pitagoras.view.director;

import pe.edu.utp.pitagoras.config.EstilosUI;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;

public class PnlCargaAcademica extends JPanel {
    public PnlCargaAcademica() {
        setLayout(new BorderLayout(0, 16));
        setBackground(EstilosUI.FONDO_GRIS);
        setBorder(new EmptyBorder(28, 36, 28, 36));

        JLabel lbl = new JLabel("Asignación de Carga Académica a Docentes (RF06 / CU06)");
        lbl.setFont(new Font("SansSerif", Font.BOLD, 24));
        add(lbl, BorderLayout.NORTH);

        String[] cols = {"Código Carga", "Docente Asignado", "Grado y Sección", "Área Curricular", "Año Escolar"};
        Object[][] data = {
                {"   CA-001", "Prof. Carlos Mendoza", "3.° A", "Matemática", "2026"},
                {"   CA-002", "Prof. María Quispe", "3.° A", "Comunicación", "2026"},
                {"   CA-003", "Prof. Jorge Rojas", "3.° A", "Ciencia y Tecnología", "2026"}
        };
        JTable t = new JTable(data, cols);
        EstilosUI.estilizarTabla(t);
        JScrollPane sp = new JScrollPane(t);
        sp.setBorder(new LineBorder(EstilosUI.BORDE_GRIS, 1));
        add(sp, BorderLayout.CENTER);
    }
}
