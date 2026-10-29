package pe.edu.utp.pitagoras.view.director;

import pe.edu.utp.pitagoras.config.EstilosUI;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;

public class PnlAuditoriaRespaldo extends JPanel {
    public PnlAuditoriaRespaldo() {
        setLayout(new BorderLayout(0, 16));
        setBackground(EstilosUI.FONDO_GRIS);
        setBorder(new EmptyBorder(28, 36, 28, 36));

        JPanel top = new JPanel(new BorderLayout());
        top.setBackground(EstilosUI.FONDO_GRIS);
        JLabel lbl = new JLabel("Bitácora de Auditoría y Copias de Seguridad (RF09, RF10 / CU09, CU10)");
        lbl.setFont(new Font("SansSerif", Font.BOLD, 22));

        JButton btnBackup = EstilosUI.crearBotonPrimario("Generar Respaldo SQL Server (.bak)");
        btnBackup.addActionListener(e -> JOptionPane.showMessageDialog(this,
                "Copia de seguridad BD_GestionAcademica_Pitagoras.bak generada en la unidad externa (RF10 / RNF07)."));

        top.add(lbl, BorderLayout.WEST);
        top.add(btnBackup, BorderLayout.EAST);
        add(top, BorderLayout.NORTH);

        String[] cols = {"Fecha y Hora", "Usuario", "Módulo", "Acción", "Valor Anterior", "Valor Nuevo"};
        Object[][] data = {
                {"   2026-10-29 08:15", "docente", "Attendance", "MODIFICACION", "Estado=F (Estudiante 02)", "Estado=F (Justificada=Sí)"},
                {"   2026-10-29 09:30", "docente", "Grades", "MODIFICACION", "Calificativo=B (Estudiante 01)", "Calificativo=A"},
                {"   2026-10-28 18:00", "director", "Audit", "BACKUP_DIARIO", "-", "Backup_20261028.bak (Exitoso)"}
        };
        JTable t = new JTable(data, cols);
        EstilosUI.estilizarTabla(t);
        JScrollPane sp = new JScrollPane(t);
        sp.setBorder(new LineBorder(EstilosUI.BORDE_GRIS, 1));
        add(sp, BorderLayout.CENTER);
    }
}
