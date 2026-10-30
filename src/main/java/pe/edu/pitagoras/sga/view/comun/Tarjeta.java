package pe.edu.pitagoras.sga.view.comun;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.swing.BorderFactory;
import javax.swing.JPanel;

/** Panel blanco con esquinas redondeadas y borde suave (las "cards" de los prototipos). */
public class Tarjeta extends JPanel {

    private static final int ARCO = 8;

    public Tarjeta() {
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(16, 18, 16, 18));
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(Tema.BLANCO);
        g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, ARCO, ARCO);
        g2.setColor(Tema.BORDE);
        g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, ARCO, ARCO);
        g2.dispose();
        super.paintComponent(g);
    }
}
