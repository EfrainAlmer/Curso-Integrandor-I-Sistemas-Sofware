package pe.edu.pitagoras.sga.view.principal;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import pe.edu.pitagoras.sga.view.comun.Tema;

/** Opción de la barra lateral: ocupa todo el ancho; la activa se pinta en azul (prototipos del equipo). */
class ItemMenu extends JPanel {

    private final JLabel etiqueta;
    private boolean activo;

    ItemMenu(String texto, Runnable alPulsar) {
        super(new BorderLayout());
        setBackground(Tema.MARINO);
        setBorder(BorderFactory.createEmptyBorder(0, 22, 0, 12));
        setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));
        setPreferredSize(new Dimension(240, 46));
        setAlignmentX(LEFT_ALIGNMENT);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        etiqueta = new JLabel(texto);
        etiqueta.setForeground(Color.WHITE);
        etiqueta.setFont(etiqueta.getFont().deriveFont(Font.PLAIN, 15f));
        add(etiqueta, BorderLayout.CENTER);
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                alPulsar.run();
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                if (!activo) {
                    setBackground(Tema.MARINO_CLARO);
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                setBackground(activo ? Tema.ACENTO : Tema.MARINO);
            }
        });
    }

    void setActivo(boolean activo) {
        this.activo = activo;
        setBackground(activo ? Tema.ACENTO : Tema.MARINO);
        etiqueta.setFont(etiqueta.getFont().deriveFont(activo ? Font.BOLD : Font.PLAIN));
        // Franja amarilla a la izquierda de la opción activa (colores del escudo).
        setBorder(activo ? BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0, 5, 0, 0, Tema.DORADO),
                BorderFactory.createEmptyBorder(0, 17, 0, 12)) : BorderFactory.createEmptyBorder(0, 22, 0, 12));
    }
}
