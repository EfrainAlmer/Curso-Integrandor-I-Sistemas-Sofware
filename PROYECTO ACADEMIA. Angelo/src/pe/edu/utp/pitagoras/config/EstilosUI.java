package pe.edu.utp.pitagoras.config;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import java.awt.*;

public class EstilosUI {
    public static final Color SIDEBAR_BG = new Color(23, 50, 86);
    public static final Color SIDEBAR_ACTIVO = new Color(41, 118, 182);
    public static final Color FONDO_GRIS = new Color(247, 249, 251);
    public static final Color AZUL_PRIMARIO = new Color(36, 112, 176);
    public static final Color TEXTO_OSCURO = new Color(20, 28, 38);
    public static final Color TEXTO_SECUNDARIO = new Color(108, 117, 125);
    public static final Color BORDE_GRIS = new Color(218, 224, 230);
    public static final Color ALERTA_DORADA = new Color(150, 108, 22);

    // Colores de escala literal MINEDU (Figura 13 y 14)
    public static final Color COLOR_AD = new Color(21, 96, 64);
    public static final Color COLOR_A  = new Color(36, 112, 176);
    public static final Color COLOR_B  = new Color(153, 102, 18);
    public static final Color COLOR_C  = new Color(166, 28, 28);

    public static JButton crearBotonPrimario(String texto) {
        JButton btn = new JButton(texto);
        btn.setFont(new Font("SansSerif", Font.BOLD, 13));
        btn.setForeground(Color.WHITE);
        btn.setBackground(AZUL_PRIMARIO);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setOpaque(true);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setBorder(new EmptyBorder(9, 18, 9, 18));
        return btn;
    }

    public static JButton crearBotonSecundario(String texto) {
        JButton btn = new JButton(texto);
        btn.setFont(new Font("SansSerif", Font.BOLD, 13));
        btn.setForeground(SIDEBAR_BG);
        btn.setBackground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(SIDEBAR_BG, 1, true),
                new EmptyBorder(8, 16, 8, 16)
        ));
        return btn;
    }

    public static void estilizarTabla(JTable tabla) {
        tabla.setRowHeight(48);
        tabla.setShowHorizontalLines(true);
        tabla.setShowVerticalLines(false);
        tabla.setGridColor(new Color(233, 236, 239));
        tabla.setBackground(Color.WHITE);
        tabla.setSelectionBackground(new Color(242, 246, 250));
        tabla.setSelectionForeground(Color.BLACK);
        tabla.setFont(new Font("SansSerif", Font.PLAIN, 13));

        JTableHeader header = tabla.getTableHeader();
        header.setPreferredSize(new Dimension(0, 40));
        header.setReorderingAllowed(false);
        header.setDefaultRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                JLabel lbl = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                lbl.setBackground(SIDEBAR_BG);
                lbl.setForeground(Color.WHITE);
                lbl.setFont(new Font("SansSerif", Font.BOLD, 13));
                lbl.setBorder(new EmptyBorder(0, 12, 0, 12));
                return lbl;
            }
        });
    }

    public static JPanel crearTarjetaKPI(String titulo, String valor, JComponent extra) {
        JPanel card = new JPanel(new BorderLayout(0, 6));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(BORDE_GRIS, 1, true),
                new EmptyBorder(16, 18, 16, 18)
        ));
        JLabel lblTit = new JLabel(titulo);
        lblTit.setFont(new Font("SansSerif", Font.PLAIN, 12));
        lblTit.setForeground(TEXTO_SECUNDARIO);

        JLabel lblVal = new JLabel(valor);
        lblVal.setFont(new Font("SansSerif", Font.BOLD, 26));
        lblVal.setForeground(TEXTO_OSCURO);

        card.add(lblTit, BorderLayout.NORTH);
        card.add(lblVal, BorderLayout.CENTER);
        if (extra != null) {
            card.add(extra, BorderLayout.SOUTH);
        }
        return card;
    }
}
