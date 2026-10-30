package pe.edu.pitagoras.sga.view.comun;

import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.extras.FlatSVGIcon;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import pe.edu.pitagoras.sga.model.evaluacion.Calificativo;

/**
 * Sistema visual único de la aplicación (colores, fuentes y componentes), tomado de los
 * prototipos UI/UX del informe: barra lateral coral, tarjetas blancas redondeadas y fondo gris claro.
 * Todas las pantallas usan estas piezas para que se vean uniformes.
 */
public final class Tema {

    /** Azul marino de la barra lateral y de la cabecera de las tablas. */
    public static final Color MARINO = new Color(0x1F3A5F);
    public static final Color MARINO_CLARO = new Color(0x2A4A73);
    /** Azul de botones principales y de la opción activa del menú. */
    public static final Color ACENTO = new Color(0x2B5F94);
    public static final Color ACENTO_CLARO = new Color(0x4677A8);
    public static final Color ACENTO_SUAVE = new Color(0xE4ECF5);
    /** Amarillo del escudo, atenuado para que se vea sobrio. */
    public static final Color DORADO = new Color(0xC9A227);
    public static final Color DORADO_SUAVE = new Color(0xFBF4DF);
    /** Fondo de las filas de resumen (calificativo del área). */
    public static final Color FONDO_RESUMEN = new Color(0xF7F3E6);
    public static final Color FONDO = new Color(0xF3F5F8);
    public static final Color BLANCO = Color.WHITE;
    public static final Color TEXTO = new Color(0x1E2A3A);
    public static final Color TEXTO_SUAVE = new Color(0x6B7280);
    public static final Color BORDE = new Color(0xE3E5EA);
    public static final Color EXITO = new Color(0x15803D);
    public static final Color EXITO_SUAVE = new Color(0xE6F6EC);
    public static final Color ALERTA = new Color(0xB45309);
    public static final Color ALERTA_SUAVE = new Color(0xFEF3E2);
    public static final Color PELIGRO = new Color(0xB91C1C);
    public static final Color PELIGRO_SUAVE = new Color(0xFDECEC);
    public static final Color INFO = new Color(0x2563EB);
    public static final Color INFO_SUAVE = new Color(0xE8F0FE);

    private Tema() {
    }

    // ---------- textos ----------

    public static JLabel titulo(String texto) {
        JLabel l = new JLabel(texto);
        l.setFont(l.getFont().deriveFont(Font.BOLD, 24f));
        l.setForeground(TEXTO);
        return l;
    }

    public static JLabel subtitulo(String texto) {
        JLabel l = new JLabel(texto);
        l.setFont(l.getFont().deriveFont(13f));
        l.setForeground(TEXTO_SUAVE);
        return l;
    }

    public static JLabel seccion(String texto) {
        JLabel l = new JLabel(texto);
        l.setFont(l.getFont().deriveFont(Font.BOLD, 16f));
        l.setForeground(TEXTO);
        return l;
    }

    public static JLabel etiqueta(String texto) {
        JLabel l = new JLabel(texto);
        l.setFont(l.getFont().deriveFont(13f));
        l.setForeground(TEXTO);
        return l;
    }

    // ---------- botones ----------

    public static JButton botonPrimario(String texto) {
        JButton b = new JButton(texto);
        b.putClientProperty(FlatClientProperties.STYLE, "font:bold;arc:6;margin:7,18,7,18");
        return b;
    }

    public static JButton botonSecundario(String texto) {
        JButton b = new JButton(texto);
        b.putClientProperty(FlatClientProperties.STYLE,
                "background:#FFFFFF;foreground:#1F3A5F;borderColor:#1F3A5F;borderWidth:1;arc:6;font:bold;"
                        + "margin:7,18,7,18;hoverBackground:#EEF3F9;pressedBackground:#DCE6F2;focusedBackground:#FFFFFF;"
                        + "focusedForeground:#1F3A5F;pressedForeground:#1F3A5F");
        return b;
    }

    public static JButton botonPeligro(String texto) {
        JButton b = new JButton(texto);
        b.putClientProperty(FlatClientProperties.STYLE,
                "background:#FFFFFF;foreground:#B91C1C;borderColor:#D98C8C;borderWidth:1;arc:6;font:bold;margin:7,18,7,18;"
                        + "hoverBackground:#FDECEC;focusedBackground:#FFFFFF;focusedForeground:#B91C1C;"
                        + "pressedForeground:#B91C1C;pressedBackground:#F9D5D5");
        return b;
    }

    // ---------- contenedores ----------

    /** @return panel blanco con esquinas redondeadas y relleno interno. */
    public static Tarjeta tarjeta() {
        return new Tarjeta();
    }

    /** Tarjeta de indicador: título pequeño y valor grande. */
    public static Tarjeta kpi(String titulo, JLabel valor, Color colorValor) {
        Tarjeta t = new Tarjeta();
        t.setLayout(new BorderLayout(0, 4));
        JLabel tit = subtitulo(titulo);
        valor.setFont(valor.getFont().deriveFont(Font.BOLD, 26f));
        valor.setForeground(colorValor == null ? MARINO : colorValor);
        t.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0, 4, 0, 0, DORADO),
                BorderFactory.createEmptyBorder(14, 16, 14, 16)));
        t.add(tit, BorderLayout.NORTH);
        t.add(valor, BorderLayout.CENTER);
        return t;
    }

    /** Etiqueta encima de un campo, como en los prototipos. */
    public static JPanel campo(String etiqueta, JComponent componente) {
        JPanel p = new JPanel(new BorderLayout(0, 6));
        p.setOpaque(false);
        p.add(etiqueta(etiqueta), BorderLayout.NORTH);
        p.add(componente, BorderLayout.CENTER);
        return p;
    }

    /** Barra de aviso de color (información, éxito, alerta o peligro). */
    public static JLabel aviso(String texto, Color fondo, Color frente) {
        JLabel l = new JLabel(texto);
        l.setOpaque(true);
        l.setBackground(fondo);
        l.setForeground(frente);
        l.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
        l.putClientProperty(FlatClientProperties.STYLE, "arc:6");
        return l;
    }

    // ---------- tablas ----------

    /** Aplica el estilo de los prototipos: filas altas, sin líneas verticales y cabecera clara. */
    public static JScrollPane tabla(JTable tabla) {
        tabla.setRowHeight(40);
        tabla.setShowVerticalLines(false);
        tabla.setShowHorizontalLines(true);
        tabla.setGridColor(new Color(0xEEF0F3));
        tabla.setIntercellSpacing(new Dimension(0, 1));
        tabla.setSelectionBackground(ACENTO_SUAVE);
        tabla.setSelectionForeground(TEXTO);
        tabla.setFillsViewportHeight(true);
        tabla.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tabla.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
        JTableHeader h = tabla.getTableHeader();
        h.setReorderingAllowed(false);
        h.setPreferredSize(new Dimension(0, 42));
        // Cabecera azul marino con texto blanco, como en los prototipos del equipo.
        DefaultTableCellRenderer cabecera = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foco, int f, int c) {
                super.getTableCellRendererComponent(t, v, false, false, f, c);
                setBackground(MARINO);
                setForeground(Color.WHITE);
                // Los títulos largos (competencias) usan HTML para partirse en varias líneas.
                boolean largo = v != null && v.toString().startsWith("<html>");
                setFont(getFont().deriveFont(Font.BOLD, largo ? 12f : 13f));
                setVerticalAlignment(largo ? TOP : CENTER);
                setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0, 0, 3, 0, DORADO),
                        largo ? BorderFactory.createEmptyBorder(8, 10, 4, 8) : BorderFactory.createEmptyBorder(0, 12, 0, 12)));
                setHorizontalAlignment(LEFT);
                return this;
            }
        };
        h.setDefaultRenderer(cabecera);
        DefaultTableCellRenderer celdas = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foco, int f, int c) {
                super.getTableCellRendererComponent(t, v, sel, false, f, c);
                setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 12));
                return this;
            }
        };
        tabla.setDefaultRenderer(Object.class, celdas);
        tabla.setDefaultRenderer(String.class, celdas);
        tabla.setDefaultRenderer(Integer.class, celdas);
        tabla.setDefaultRenderer(Calificativo.class, new RenderInsignia());
        JScrollPane sp = new JScrollPane(tabla);
        sp.setBorder(BorderFactory.createLineBorder(BORDE));
        sp.getViewport().setBackground(BLANCO);
        return sp;
    }

    // ---------- colores por calificativo ----------

    /** Color sólido de la insignia del calificativo (texto blanco encima). */
    public static Color colorSolido(Calificativo c) {
        return switch (c) {
            case AD -> new Color(0x1E7B4F);
            case A -> new Color(0x2B5F94);
            case B -> new Color(0x8A5A00);
            case C -> new Color(0xB42318);
        };
    }

    public static Color colorTexto(Calificativo c) {
        return switch (c) {
            case AD -> EXITO;
            case A -> INFO;
            case B -> ALERTA;
            case C -> PELIGRO;
        };
    }

    public static Color colorFondo(Calificativo c) {
        return switch (c) {
            case AD -> EXITO_SUAVE;
            case A -> INFO_SUAVE;
            case B -> ALERTA_SUAVE;
            case C -> PELIGRO_SUAVE;
        };
    }

    // ---------- íconos ----------

    /** @return el escudo del colegio escalado al alto indicado, o null si no se encuentra. */
    public static javax.swing.ImageIcon escudo(int alto) {
        java.net.URL url = Tema.class.getResource("/app/img/escudo.png");
        if (url == null) {
            return null;
        }
        java.awt.Image img = new javax.swing.ImageIcon(url).getImage();
        int ancho = Math.round(alto * img.getWidth(null) / (float) img.getHeight(null));
        return new javax.swing.ImageIcon(img.getScaledInstance(ancho, alto, java.awt.Image.SCALE_SMOOTH));
    }

    /** @return ícono SVG de /app/icon/ pintado con el color indicado. */
    public static FlatSVGIcon icono(String nombre, int tamano, Color color) {
        FlatSVGIcon icono = new FlatSVGIcon("app/icon/" + nombre + ".svg", tamano, tamano);
        icono.setColorFilter(new FlatSVGIcon.ColorFilter(c -> color));
        return icono;
    }
}
