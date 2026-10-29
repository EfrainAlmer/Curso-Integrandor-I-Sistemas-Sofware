package pe.edu.utp.pitagoras.config;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.geom.RoundRectangle2D;
import javax.swing.table.TableModel;

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

    // ══════════ Componentes migrados de UI.java (fase 1) ══════════
    // SIDEBAR_BG se conserva con el valor original de EstilosUI para no alterar el menú lateral.


    // ── Paleta ────────────────────────────────────────────────────
    public static final Color BG_MAIN       = new Color(244, 246, 249);
    public static final Color BG_CARD       = Color.WHITE;
    public static final Color BG_FIELD      = new Color(248, 249, 251);
    public static final Color HEADER_TABLE  = new Color(28, 53, 94);
    public static final Color PRIMARY_BLUE  = new Color(37, 99, 235);
    public static final Color PRIMARY_HOVER = new Color(29, 78, 216);
    public static final Color DANGER_RED    = new Color(200, 30, 50);
    public static final Color DANGER_HOVER  = new Color(170, 20, 40);
    public static final Color SUCCESS_GREEN = new Color(22, 135, 84);
    public static final Color SUCCESS_HOVER = new Color(16, 110, 68);
    public static final Color WARNING_AMBER = new Color(180, 110, 0);
    public static final Color TEXT_DARK     = new Color(25, 30, 40);
    public static final Color TEXT_GRAY     = new Color(115, 125, 140);
    public static final Color BORDER_CLR    = new Color(220, 224, 230);

    public static final String FUENTE = "Segoe UI";

    public static Font font(int style, int size) {
        return new Font(FUENTE, style, size);
    }

    // ── Encabezado estándar de cada panel ─────────────────────────
    public static void encabezado(JPanel panel, String titulo, String subtitulo) {
        panel.add(label(titulo, 32, 24, 880, 32, 24, Font.BOLD, TEXT_DARK));
        panel.add(label(subtitulo, 32, 58, 900, 20, 13, Font.PLAIN, TEXT_GRAY));
    }

    // ── Tarjeta con esquinas redondeadas ──────────────────────────
    public static JPanel card(int x, int y, int w, int h) {
        JPanel card = new JPanel(null) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(BG_CARD);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 12, 12));
                g2.setColor(BORDER_CLR);
                g2.draw(new RoundRectangle2D.Float(0, 0, getWidth() - 1, getHeight() - 1, 12, 12));
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setBounds(x, y, w, h);
        return card;
    }

    public static JLabel label(String text, int x, int y, int w, int h, int size, int style, Color color) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(font(style, size));
        lbl.setForeground(color);
        lbl.setBounds(x, y, w, h);
        return lbl;
    }

    /** Etiqueta pequeña en mayúsculas que va encima de cada campo. */
    public static JLabel etiquetaCampo(String text, int x, int y, int w) {
        return label(text, x, y, w, 16, 10, Font.BOLD, TEXT_GRAY);
    }

    // ── Botones ───────────────────────────────────────────────────
    public static JButton botonPrimario(String texto, Color normal, Color hover) {
        return crearBoton(texto, normal, hover, null, Color.WHITE, 13);
    }

    public static JButton botonPrimario(String texto) {
        return botonPrimario(texto, PRIMARY_BLUE, PRIMARY_HOVER);
    }

    /** Botón con borde y texto de color; el fondo hover es una versión muy clara del mismo color. */
    public static JButton botonSecundario(String texto, Color color) {
        return crearBoton(texto, Color.WHITE, mezclar(color, 0.92f), color, color, 12);
    }

    public static JButton botonSecundario(String texto) {
        return botonSecundario(texto, SIDEBAR_BG);
    }

    private static JButton crearBoton(String texto, Color fondo, Color fondoHover,
                                      Color borde, Color colorTexto, int tamFuente) {
        JButton btn = new JButton(texto) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                boolean activo = isEnabled();
                boolean resaltado = activo && (getModel().isPressed() || getModel().isRollover());
                Color bg = !activo ? new Color(232, 235, 240) : (resaltado ? fondoHover : fondo);
                g2.setColor(bg);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 8, 8));
                if (borde != null) {
                    g2.setColor(activo ? borde : BORDER_CLR);
                    g2.draw(new RoundRectangle2D.Float(0, 0, getWidth() - 1, getHeight() - 1, 8, 8));
                }
                g2.setColor(activo ? colorTexto : TEXT_GRAY);
                g2.setFont(getFont());
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(getText(), (getWidth() - fm.stringWidth(getText())) / 2,
                        (getHeight() + fm.getAscent() - fm.getDescent()) / 2);
                g2.dispose();
            }
        };
        btn.setFont(font(Font.BOLD, tamFuente));
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }

    public static Color mezclar(Color c, float tBlanco) {
        int r = Math.round(c.getRed()   + (255 - c.getRed())   * tBlanco);
        int g = Math.round(c.getGreen() + (255 - c.getGreen()) * tBlanco);
        int b = Math.round(c.getBlue()  + (255 - c.getBlue())  * tBlanco);
        return new Color(r, g, b);
    }

    // ── Campos de texto ───────────────────────────────────────────
    /** Campo con texto de sugerencia (hint) que NO forma parte del contenido: getText() queda vacío. */
    public static JTextField campoTexto(String hint) {
        JTextField field = new JTextField() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (hint != null && getText().isEmpty() && !isFocusOwner()) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                    g2.setColor(TEXT_GRAY);
                    g2.setFont(getFont());
                    FontMetrics fm = g2.getFontMetrics();
                    Insets in = getInsets();
                    int y = in.top + (getHeight() - in.top - in.bottom - fm.getHeight()) / 2 + fm.getAscent();
                    g2.drawString(hint, in.left, y);
                    g2.dispose();
                }
            }
        };
        estilizarCampo(field);
        return field;
    }

    public static JPasswordField campoPassword() {
        JPasswordField field = new JPasswordField();
        estilizarCampo(field);
        field.setEchoChar('\u25CF');
        return field;
    }

    private static void estilizarCampo(JTextField field) {
        field.setFont(font(Font.PLAIN, 13));
        field.setBackground(BG_FIELD);
        field.setForeground(TEXT_DARK);
        field.setCaretColor(TEXT_DARK);
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_CLR, 1),
                new EmptyBorder(6, 12, 6, 12)));
        field.addFocusListener(new FocusAdapter() {
            @Override public void focusGained(FocusEvent e) { field.repaint(); }
            @Override public void focusLost(FocusEvent e)   { field.repaint(); }
        });
    }

    public static <T> JComboBox<T> combo(T[] items) {
        JComboBox<T> cbo = new JComboBox<>(items);
        cbo.setFont(font(Font.PLAIN, 13));
        return cbo;
    }

    // ── Tablas ────────────────────────────────────────────────────
    public static JTable tabla(TableModel modelo) {
        JTable tabla = new JTable(modelo);
        tabla.setFont(font(Font.PLAIN, 13));
        tabla.setRowHeight(36);
        tabla.setGridColor(new Color(238, 240, 245));
        tabla.setSelectionBackground(new Color(224, 238, 255));
        tabla.setSelectionForeground(TEXT_DARK);
        tabla.setShowVerticalLines(false);
        tabla.setFillsViewportHeight(true);
        tabla.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JTableHeader header = tabla.getTableHeader();
        header.setPreferredSize(new Dimension(0, 38));
        header.setReorderingAllowed(false);

        // Se sobrescribe getTableCellRendererComponent porque DefaultTableCellRenderer
        // vuelve a aplicar la fuente de la tabla y descartaría la fuente del encabezado.
        DefaultTableCellRenderer hr = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object v, boolean sel,
                                                           boolean foc, int r, int c) {
                super.getTableCellRendererComponent(t, v, false, false, r, c);
                setBackground(HEADER_TABLE);
                setForeground(Color.WHITE);
                setFont(font(Font.BOLD, 12));
                setBorder(new EmptyBorder(0, 10, 0, 10));
                setHorizontalAlignment(SwingConstants.LEFT);
                return this;
            }
        };
        // Como renderer por defecto del encabezado (no por columna) para que sobreviva
        // a un setDataVector()/cambio de columnas del modelo.
        header.setDefaultRenderer(hr);

        DefaultTableCellRenderer cr = new DefaultTableCellRenderer();
        cr.setBorder(new EmptyBorder(0, 10, 0, 10));
        tabla.setDefaultRenderer(Object.class, cr);
        return tabla;
    }

    public static JScrollPane scroll(JTable tabla, int x, int y, int w, int h) {
        JScrollPane sp = new JScrollPane(tabla);
        sp.setBounds(x, y, w, h);
        sp.setBorder(BorderFactory.createLineBorder(BORDER_CLR));
        sp.getViewport().setBackground(Color.WHITE);
        return sp;
    }

    public static void anchos(JTable tabla, int... anchos) {
        for (int i = 0; i < anchos.length && i < tabla.getColumnCount(); i++) {
            tabla.getColumnModel().getColumn(i).setPreferredWidth(anchos[i]);
        }
    }

    public static void centrar(JTable tabla, int... columnas) {
        for (int col : columnas) {
            DefaultTableCellRenderer r = new DefaultTableCellRenderer();
            r.setHorizontalAlignment(SwingConstants.CENTER);
            tabla.getColumnModel().getColumn(col).setCellRenderer(r);
        }
    }

    // ── Mensajes ──────────────────────────────────────────────────
    public static void info(Component padre, String titulo, String msg) {
        JOptionPane.showMessageDialog(padre, msg, titulo, JOptionPane.INFORMATION_MESSAGE);
    }

    public static void aviso(Component padre, String titulo, String msg) {
        JOptionPane.showMessageDialog(padre, msg, titulo, JOptionPane.WARNING_MESSAGE);
    }

    public static void error(Component padre, String titulo, String msg) {
        JOptionPane.showMessageDialog(padre, msg, titulo, JOptionPane.ERROR_MESSAGE);
    }

    /** Envuelve un panel de tamaño fijo en un JScrollPane para que no se recorte si la ventana es pequeña. */
    public static JScrollPane envolver(JPanel panel) {
        JScrollPane sp = new JScrollPane(panel);
        sp.setBorder(null);
        sp.getViewport().setBackground(BG_MAIN);
        sp.getVerticalScrollBar().setUnitIncrement(16);
        sp.getHorizontalScrollBar().setUnitIncrement(16);
        return sp;
    }
}
