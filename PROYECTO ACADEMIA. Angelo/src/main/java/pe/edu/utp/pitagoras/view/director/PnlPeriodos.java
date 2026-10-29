package pe.edu.utp.pitagoras.view.director;

import pe.edu.utp.pitagoras.config.EstilosUI;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;

public class PnlPeriodos extends JPanel {
    public PnlPeriodos() {
        setLayout(new BorderLayout(0, 16));
        setBackground(EstilosUI.FONDO_GRIS);
        setBorder(new EmptyBorder(28, 36, 28, 36));

        JLabel lbl = new JLabel("Control de Apertura y Cierre de Periodos Bimestrales (RF07 / CU07)");
        lbl.setFont(new Font("SansSerif", Font.BOLD, 24));
        add(lbl, BorderLayout.NORTH);

        String[] cols = {"Periodo", "Fecha Inicio", "Fecha Fin", "Estado Actual", "Auditoría de Cierre"};
        Object[][] data = {
                {"   Bimestre 1", "09/03/2026", "15/05/2026", "Cerrado (Bloqueado)", "Cerrado por Director General"},
                {"   Bimestre 2", "18/05/2026", "24/07/2026", "Abierto (En curso)", "Edición habilitada"},
                {"   Bimestre 3", "10/08/2026", "16/10/2026", "Pendiente", "-"},
                {"   Bimestre 4", "19/10/2026", "18/12/2026", "Pendiente (Define Nota Anual RF14)", "-"}
        };
        JTable t = new JTable(data, cols);
        EstilosUI.estilizarTabla(t);
        JScrollPane sp = new JScrollPane(t);
        sp.setBorder(new LineBorder(EstilosUI.BORDE_GRIS, 1));
        add(sp, BorderLayout.CENTER);
    }
}
