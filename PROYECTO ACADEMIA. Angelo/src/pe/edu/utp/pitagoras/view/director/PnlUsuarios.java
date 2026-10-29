package pe.edu.utp.pitagoras.view.director;

import pe.edu.utp.pitagoras.config.EstilosUI;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;

public class PnlUsuarios extends JPanel {
    public PnlUsuarios() {
        setLayout(new BorderLayout(0, 16));
        setBackground(EstilosUI.FONDO_GRIS);
        setBorder(new EmptyBorder(28, 36, 28, 36));

        JPanel top = new JPanel(new BorderLayout());
        top.setBackground(EstilosUI.FONDO_GRIS);
        JLabel lbl = new JLabel("Gestión de Usuarios y Roles (RF03 / CU03)");
        lbl.setFont(new Font("SansSerif", Font.BOLD, 24));
        JButton btnNuevo = EstilosUI.crearBotonPrimario("+ Nuevo Usuario (BCrypt)");
        btnNuevo.addActionListener(e -> JOptionPane.showMessageDialog(this,
                "Se creó el usuario y se generó automáticamente la cuenta vinculada del Apoderado (RF03)."));
        top.add(lbl, BorderLayout.WEST);
        top.add(btnNuevo, BorderLayout.EAST);
        add(top, BorderLayout.NORTH);

        String[] cols = {"ID", "Usuario", "Nombre Completo", "Rol", "Estado", "Hash Criptográfico"};
        Object[][] data = {
                {"   1", "director", "Director General", "Director / Coordinador", "Activo", "$2a$10$EixZaYVK1fsbw1ZfbX3OXe..."},
                {"   2", "docente", "Prof. Carlos Mendoza", "Docente", "Activo", "$2a$10$92IXUNpkjO0rOQ5byMi.Ye..."},
                {"   3", "apoderado", "Apoderado Estudiante A", "Alumno / Apoderado", "Activo", "$2a$10$vI8aWBnW3fID.ZQ4/zo1G..."}
        };
        JTable t = new JTable(data, cols);
        EstilosUI.estilizarTabla(t);
        JScrollPane sp = new JScrollPane(t);
        sp.setBorder(new LineBorder(EstilosUI.BORDE_GRIS, 1));
        add(sp, BorderLayout.CENTER);
    }
}
