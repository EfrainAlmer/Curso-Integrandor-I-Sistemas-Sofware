package pe.edu.utp.pitagoras.view.director;

import pe.edu.utp.pitagoras.config.EstilosUI;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;

public class PnlConfigAcademica extends JPanel {
    public PnlConfigAcademica() {
        setLayout(new BorderLayout(0, 16));
        setBackground(EstilosUI.FONDO_GRIS);
        setBorder(new EmptyBorder(28, 36, 28, 36));

        JLabel lbl = new JLabel("Configuración Académica: Grados, Secciones y Áreas (RF05 / CU05)");
        lbl.setFont(new Font("SansSerif", Font.BOLD, 24));
        add(lbl, BorderLayout.NORTH);

        String[] cols = {"Año Escolar", "Grado (Nivel Secundario)", "Sección", "Turno", "Áreas Curriculares Configuradas"};
        Object[][] data = {
                {"   2026", "1.° Secundaria", "A", "Mañana", "Matemática, Comunicación, CyT, CC.SS., Inglés"},
                {"   2026", "2.° Secundaria", "A", "Mañana", "Matemática, Comunicación, CyT, CC.SS., Inglés"},
                {"   2026", "3.° Secundaria", "A", "Mañana", "Matemática, Comunicación, CyT, CC.SS., Inglés"},
                {"   2026", "4.° Secundaria", "A", "Mañana", "Matemática, Comunicación, CyT, CC.SS., Inglés"},
                {"   2026", "5.° Secundaria", "A", "Mañana", "Matemática, Comunicación, CyT, CC.SS., Inglés"}
        };
        JTable t = new JTable(data, cols);
        EstilosUI.estilizarTabla(t);
        JScrollPane sp = new JScrollPane(t);
        sp.setBorder(new LineBorder(EstilosUI.BORDE_GRIS, 1));
        add(sp, BorderLayout.CENTER);
    }
}
