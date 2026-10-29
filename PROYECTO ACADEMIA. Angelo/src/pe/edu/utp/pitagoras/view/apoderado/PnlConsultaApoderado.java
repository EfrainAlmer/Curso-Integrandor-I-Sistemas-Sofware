package pe.edu.utp.pitagoras.view.apoderado;

import pe.edu.utp.pitagoras.config.EstilosUI;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.table.TableCellRenderer;
import java.awt.*;

public class PnlConsultaApoderado extends JPanel {

    public PnlConsultaApoderado() {
        setLayout(new BorderLayout(0, 18));
        setBackground(EstilosUI.FONDO_GRIS);
        setBorder(new EmptyBorder(28, 36, 28, 36));
        inicializarUI();
    }

    private void inicializarUI() {
        JPanel norte = new JPanel();
        norte.setBackground(EstilosUI.FONDO_GRIS);
        norte.setLayout(new BoxLayout(norte, BoxLayout.Y_AXIS));

        JLabel lblTitulo = new JLabel("Estudiante A · 3.° A");
        lblTitulo.setFont(new Font("SansSerif", Font.BOLD, 24));
        lblTitulo.setForeground(EstilosUI.TEXTO_OSCURO);

        JLabel lblSub = new JLabel("Vista de solo lectura para el alumno y su apoderado · Bimestre 2");
        lblSub.setFont(new Font("SansSerif", Font.PLAIN, 13));
        lblSub.setForeground(EstilosUI.TEXTO_SECUNDARIO);

        norte.add(lblTitulo);
        norte.add(Box.createVerticalStrut(4));
        norte.add(lblSub);
        norte.add(Box.createVerticalStrut(16));

        // 4 Tarjetas de Resumen de Asistencia (Figura 14)
        JPanel gridAsistencia = new JPanel(new GridLayout(1, 4, 14, 0));
        gridAsistencia.setBackground(EstilosUI.FONDO_GRIS);
        gridAsistencia.setAlignmentX(Component.LEFT_ALIGNMENT);

        gridAsistencia.add(EstilosUI.crearTarjetaKPI("Asistencias", "38", null));
        gridAsistencia.add(EstilosUI.crearTarjetaKPI("Faltas", "2", null));
        gridAsistencia.add(EstilosUI.crearTarjetaKPI("Tardanzas", "1", null));
        gridAsistencia.add(EstilosUI.crearTarjetaKPI("Faltas justificadas", "1", null));

        norte.add(gridAsistencia);
        add(norte, BorderLayout.NORTH);

        // Tabla de solo lectura con insignias de Calificativo (Figura 14)
        String[] cols = {"Área", "Competencia", "Calificativo"};
        Object[][] datos = {
                {"   Matemática", "Resuelve problemas de cantidad", "A"},
                {"   ", "Regularidad, equivalencia y cambio", "B"},
                {"   ", "Forma, movimiento y localización", "A"},
                {"   ", "Gestión de datos e incertidumbre", "A"}
        };

        JTable tabla = new JTable(datos, cols) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // Estricto solo lectura (RF15 / RNF03)
            }
        };
        EstilosUI.estilizarTabla(tabla);
        tabla.setRowHeight(48);
        tabla.getColumnModel().getColumn(0).setPreferredWidth(180);
        tabla.getColumnModel().getColumn(1).setPreferredWidth(520);
        tabla.getColumnModel().getColumn(2).setPreferredWidth(120);
        tabla.getColumnModel().getColumn(2).setCellRenderer(new BadgeCalificativoRenderer());

        JScrollPane scroll = new JScrollPane(tabla);
        scroll.setBorder(new LineBorder(EstilosUI.BORDE_GRIS, 1));
        scroll.getViewport().setBackground(Color.WHITE);
        add(scroll, BorderLayout.CENTER);

        // Pie de página (Leyenda y botón Ver libreta PDF)
        JPanel sur = new JPanel(new BorderLayout());
        sur.setBackground(EstilosUI.FONDO_GRIS);

        JLabel lblLeyenda = new JLabel("Los calificativos finales del año corresponden al último bimestre.");
        lblLeyenda.setFont(new Font("SansSerif", Font.PLAIN, 13));
        lblLeyenda.setForeground(EstilosUI.TEXTO_SECUNDARIO);

        JButton btnVerLibreta = EstilosUI.crearBotonPrimario("Ver libreta (PDF)");
        btnVerLibreta.addActionListener(e -> JOptionPane.showMessageDialog(this,
                "Generando vista previa de la Libreta de Notas en PDF para Estudiante A · 3.° A (RF17 / CU17).",
                "Visor de Libreta PDF", JOptionPane.INFORMATION_MESSAGE));

        sur.add(lblLeyenda, BorderLayout.WEST);
        sur.add(btnVerLibreta, BorderLayout.EAST);
        add(sur, BorderLayout.SOUTH);
    }

    // Renderiza la pastilla de color (Badge) para el calificativo AD, A, B, C igual a la Figura 14
    private static class BadgeCalificativoRenderer implements TableCellRenderer {
        private final JPanel contenedor = new JPanel(new FlowLayout(FlowLayout.LEFT, 14, 11));
        private final JLabel badge = new JLabel("", SwingConstants.CENTER);

        public BadgeCalificativoRenderer() {
            contenedor.setBackground(Color.WHITE);
            badge.setPreferredSize(new Dimension(42, 24));
            badge.setOpaque(true);
            badge.setForeground(Color.WHITE);
            badge.setFont(new Font("SansSerif", Font.BOLD, 12));
            contenedor.add(badge);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            String cal = value != null ? value.toString() : "A";
            badge.setText(cal);
            badge.setBackground(switch (cal) {
                case "AD" -> EstilosUI.COLOR_AD;
                case "A"  -> EstilosUI.COLOR_A;
                case "B"  -> EstilosUI.COLOR_B;
                default   -> EstilosUI.COLOR_C;
            });
            return contenedor;
        }
    }
}
