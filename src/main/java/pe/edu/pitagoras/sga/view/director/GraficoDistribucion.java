package pe.edu.pitagoras.sga.view.director;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.swing.JComponent;
import pe.edu.pitagoras.sga.model.evaluacion.Calificativo;
import pe.edu.pitagoras.sga.view.comun.Tema;

/**
 * Barras horizontales apiladas al 100 %: distribución de calificativos de área (AD, A, B, C)
 * por grado (RF08). Se dibuja con Java2D, sin librerías externas.
 */
class GraficoDistribucion extends JComponent {

    private static final Color[] COLORES = {new Color(0x16A34A), new Color(0x3B82F6), new Color(0xF59E0B),
        new Color(0xDC2626)};
    private Map<String, Map<Calificativo, Integer>> datos = new LinkedHashMap<>();

    GraficoDistribucion() {
        setPreferredSize(new Dimension(400, 250));
    }

    void setDatos(Map<String, Map<Calificativo, Integer>> nuevos) {
        datos = nuevos;
        // Alto según la cantidad de filas, para que con 20 secciones se pueda desplazar.
        setPreferredSize(new Dimension(400, 40 + nuevos.size() * 32));
        revalidate();
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        Font base = getFont().deriveFont(12f);
        g2.setFont(base);
        FontMetrics fm = g2.getFontMetrics();

        // Leyenda
        int x = 0;
        Calificativo[] cal = Calificativo.values();
        for (int i = 0; i < cal.length; i++) {
            g2.setColor(COLORES[i]);
            g2.fillRoundRect(x, 4, 12, 12, 4, 4);
            g2.setColor(Tema.TEXTO_SUAVE);
            String t = cal[i].name() + " " + cal[i].getDescripcion().toLowerCase();
            g2.drawString(t, x + 18, 15);
            x += 18 + fm.stringWidth(t) + 20;
        }

        int etiqueta = 0;
        for (String k : datos.keySet()) {
            etiqueta = Math.max(etiqueta, g2.getFontMetrics(base.deriveFont(Font.BOLD)).stringWidth(k) + 12);
        }
        int total = 70;
        int y = 34;
        int alto = 22;
        int ancho = getWidth() - etiqueta - total;
        int separacion = 32;
        for (Map.Entry<String, Map<Calificativo, Integer>> e : datos.entrySet()) {
            int suma = e.getValue().values().stream().mapToInt(Integer::intValue).sum();
            g2.setColor(Tema.TEXTO);
            g2.setFont(base.deriveFont(Font.BOLD));
            g2.drawString(e.getKey(), 0, y + alto / 2 + fm.getAscent() / 2 - 2);
            g2.setFont(base);
            g2.setColor(new Color(0xF1F2F5));
            g2.fillRoundRect(etiqueta, y, ancho, alto, 8, 8);
            int px = etiqueta;
            for (int i = 0; i < cal.length && suma > 0; i++) {
                int n = e.getValue().getOrDefault(cal[i], 0);
                int w = i == cal.length - 1 ? etiqueta + ancho - px : (int) Math.round((double) n * ancho / suma);
                if (w > 0) {
                    g2.setColor(COLORES[i]);
                    g2.fillRect(px, y, w, alto);
                    String pct = Math.round(n * 100.0 / suma) + "%";
                    if (w > fm.stringWidth(pct) + 8) {
                        g2.setColor(Color.WHITE);
                        g2.drawString(pct, px + (w - fm.stringWidth(pct)) / 2, y + alto / 2 + fm.getAscent() / 2 - 2);
                    }
                }
                px += w;
            }
            g2.setColor(Tema.TEXTO_SUAVE);
            String t = suma == 0 ? "sin notas" : String.valueOf(suma);
            g2.drawString(t, etiqueta + ancho + 10, y + alto / 2 + fm.getAscent() / 2 - 2);
            y += separacion;
        }
        g2.dispose();
    }
}
