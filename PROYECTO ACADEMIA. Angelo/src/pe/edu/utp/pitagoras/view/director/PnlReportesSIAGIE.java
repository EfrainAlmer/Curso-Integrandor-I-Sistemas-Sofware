package pe.edu.utp.pitagoras.view.director;

import pe.edu.utp.pitagoras.config.EstilosUI;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class PnlReportesSIAGIE extends JPanel {
    public PnlReportesSIAGIE() {
        setLayout(new BorderLayout(0, 18));
        setBackground(EstilosUI.FONDO_GRIS);
        setBorder(new EmptyBorder(28, 36, 28, 36));

        JLabel lbl = new JLabel("Motor de Reportes PDF y Exportación SIAGIE (RF18, RF19 / CU18, CU19)");
        lbl.setFont(new Font("SansSerif", Font.BOLD, 24));
        add(lbl, BorderLayout.NORTH);

        JPanel grid = new JPanel(new GridLayout(2, 2, 16, 16));
        grid.setBackground(EstilosUI.FONDO_GRIS);

        JButton btnLibreta = EstilosUI.crearBotonPrimario("Generar Libretas Bimestrales (PDF)");
        btnLibreta.addActionListener(e -> JOptionPane.showMessageDialog(this, "Libretas generadas en PDF listas para impresión local (CU18)."));

        JButton btnNomina = EstilosUI.crearBotonPrimario("Generar Nómina de Matrícula (PDF)");
        btnNomina.addActionListener(e -> JOptionPane.showMessageDialog(this, "Nómina oficial de matriculados exportada en PDF (RF18)."));

        JButton btnAsisPDF = EstilosUI.crearBotonPrimario("Consolidado Mensual de Asistencia (PDF)");
        btnAsisPDF.addActionListener(e -> JOptionPane.showMessageDialog(this, "Reporte consolidado de asistencia generado en PDF."));

        JButton btnSIAGIE = EstilosUI.crearBotonSecundario("Exportar Plantilla Excel/CSV para SIAGIE");
        btnSIAGIE.addActionListener(e -> JOptionPane.showMessageDialog(this,
                "Archivo Excel (.xlsx) generado con calificaciones y asistencia del bimestre\n" +
                "para soporte de carga manual en la plataforma SIAGIE del MINEDU (RF19 / CU19)."));

        grid.add(EstilosUI.crearTarjetaKPI("Libretas de Notas", "Formato PDF Oficial", btnLibreta));
        grid.add(EstilosUI.crearTarjetaKPI("Nómina de Alumnos", "Por Grado y Sección", btnNomina));
        grid.add(EstilosUI.crearTarjetaKPI("Reporte de Asistencia", "Consolidado Mensual/Bimestral", btnAsisPDF));
        grid.add(EstilosUI.crearTarjetaKPI("Interoperabilidad SIAGIE", "Hoja de Cálculo Excel", btnSIAGIE));

        add(grid, BorderLayout.CENTER);
    }
}
