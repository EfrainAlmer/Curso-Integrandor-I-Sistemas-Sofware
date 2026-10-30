package pe.edu.pitagoras.sga.view.comun;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;
import pe.edu.pitagoras.sga.model.evaluacion.Calificativo;

/**
 * Dibuja el valor de la celda como una "píldora" de color: calificativos (AD verde, A azul,
 * B ámbar, C rojo) y estados de texto (Activo, Cerrado, ...).
 */
public class RenderInsignia extends JComponent implements TableCellRenderer {

    private String texto;
    private Color fondo;
    private Color frente;
    private Color fondoFila;
    private boolean alIzquierda;

    /** @param alIzquierda true para alinear la píldora a la izquierda; false la centra. */
    public RenderInsignia(boolean alIzquierda) {
        this.alIzquierda = alIzquierda;
    }

    public RenderInsignia() {
        this(false);
    }

    @Override
    public Component getTableCellRendererComponent(JTable t, Object valor, boolean sel, boolean foco, int fila, int col) {
        fondoFila = sel ? t.getSelectionBackground() : t.getBackground();
        texto = null;
        if (valor instanceof Calificativo c) {
            texto = c.name();
            fondo = Tema.colorSolido(c);
            frente = java.awt.Color.WHITE;
        } else if (valor != null && !valor.toString().isBlank()) {
            texto = valor.toString();
            Color[] colores = coloresEstado(texto);
            fondo = colores[0];
            frente = colores[1];
        }
        setToolTipText(valor instanceof Calificativo c ? c.getDescripcion() : null);
        return this;
    }

    /** Cambia el fondo de la celda (por ejemplo, para resaltar un valor sin guardar). */
    public void resaltar(Color fondoCelda) {
        this.fondoFila = fondoCelda;
    }

    /** @return fondo y texto para estados conocidos. */
    static Color[] coloresEstado(String texto) {
        String t = texto.toLowerCase(java.util.Locale.ROOT);
        if (t.startsWith("activo") || t.startsWith("abierto") || t.startsWith("exitoso") || t.startsWith("asisti")
                || t.startsWith("logrado") || t.startsWith("registrado")) {
            return new Color[]{Tema.EXITO_SUAVE, Tema.EXITO};
        }
        if (t.startsWith("inactivo") || t.startsWith("retirado") || t.startsWith("fallido") || t.equals("falta")
                || t.startsWith("bloqueado") || t.startsWith("en inicio")) {
            return new Color[]{Tema.PELIGRO_SUAVE, Tema.PELIGRO};
        }
        if (t.startsWith("cerrado") || t.startsWith("tardanza") || t.startsWith("debe cambiar")
                || t.startsWith("pendiente") || t.startsWith("en proceso")) {
            return new Color[]{Tema.ALERTA_SUAVE, Tema.ALERTA};
        }
        return new Color[]{Tema.INFO_SUAVE, Tema.INFO};
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setColor(fondoFila);
        g2.fillRect(0, 0, getWidth(), getHeight());
        if (texto != null) {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            Font f = getFont() == null ? g2.getFont() : getFont();
            g2.setFont(f.deriveFont(Font.BOLD, 12f));
            FontMetrics fm = g2.getFontMetrics();
            boolean corto = texto.length() <= 2;
            int ancho = corto ? 52 : fm.stringWidth(texto) + 22;
            int alto = 24;
            int x = alIzquierda ? 12 : (getWidth() - ancho) / 2;
            int y = (getHeight() - alto) / 2;
            g2.setColor(fondo);
            g2.fillRoundRect(x, y, ancho, alto, 6, 6);
            g2.setColor(frente);
            g2.drawString(texto, x + (ancho - fm.stringWidth(texto)) / 2, y + (alto + fm.getAscent() - fm.getDescent()) / 2);
        }
        g2.dispose();
    }
}
