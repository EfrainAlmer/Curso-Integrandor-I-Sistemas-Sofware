package pe.edu.utp.pitagoras.view.director;

import pe.edu.utp.pitagoras.config.EstilosUI;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;

public class PnlInscripcion extends JPanel {
    public PnlInscripcion() {
        setLayout(new BorderLayout(0, 16));
        setBackground(EstilosUI.FONDO_GRIS);
        setBorder(new EmptyBorder(28, 36, 28, 36));

        JPanel top = new JPanel(new BorderLayout());
        top.setBackground(EstilosUI.FONDO_GRIS);
        JLabel lbl = new JLabel("Inscripción de Alumnos y Matrícula (RF04 / CU04)");
        lbl.setFont(new Font("SansSerif", Font.BOLD, 24));
        JButton btnMatricular = EstilosUI.crearBotonPrimario("+ Registrar Matrícula 2026");
        btnMatricular.addActionListener(e -> JOptionPane.showMessageDialog(this,
                "Estudiante inscrito en el Año Escolar 2026 y vinculado a su Apoderado (RF04)."));
        top.add(lbl, BorderLayout.WEST);
        top.add(btnMatricular, BorderLayout.EAST);
        add(top, BorderLayout.NORTH);

        String[] cols = {"DNI Estudiante", "Nombres y Apellidos", "Grado y Sección", "Apoderado Vinculado", "Estado Matrícula"};
        Object[][] data = {
                {"   74859612", "Estudiante 01", "3.° A - Secundaria", "Apoderado 01 (DNI 41526398)", "Activa"},
                {"   74859613", "Estudiante 02", "3.° A - Secundaria", "Apoderado 02 (DNI 41526399)", "Activa"},
                {"   74859614", "Estudiante 03", "3.° A - Secundaria", "Apoderado 03 (DNI 41526400)", "Activa"}
        };
        JTable t = new JTable(data, cols);
        EstilosUI.estilizarTabla(t);
        JScrollPane sp = new JScrollPane(t);
        sp.setBorder(new LineBorder(EstilosUI.BORDE_GRIS, 1));
        add(sp, BorderLayout.CENTER);
    }
}
