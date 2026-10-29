package pe.edu.utp.pitagoras.view.director;

import pe.edu.utp.pitagoras.config.EstilosUI;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.table.TableCellRenderer;
import java.awt.*;

public class PnlDashboardDirector extends JPanel {

    public PnlDashboardDirector() {
        setLayout(new BorderLayout(0, 18));
        setBackground(EstilosUI.FONDO_GRIS);
        setBorder(new EmptyBorder(26, 34, 26, 34));
        inicializarUI();
    }

    private void inicializarUI() {
        // Cabecera
        JPanel norte = new JPanel();
        norte.setBackground(EstilosUI.FONDO_GRIS);
        norte.setLayout(new BoxLayout(norte, BoxLayout.Y_AXIS));

        JLabel lblTitulo = new JLabel("Panel de indicadores");
        lblTitulo.setFont(new Font("SansSerif", Font.BOLD, 24));
        lblTitulo.setForeground(EstilosUI.TEXTO_OSCURO);

        JLabel lblSub = new JLabel("Año escolar 2026 · Bimestre 2 (abierto)");
        lblSub.setFont(new Font("SansSerif", Font.PLAIN, 13));
        lblSub.setForeground(EstilosUI.TEXTO_SECUNDARIO);

        norte.add(lblTitulo);
        norte.add(Box.createVerticalStrut(4));
        norte.add(lblSub);
        norte.add(Box.createVerticalStrut(16));

        // 4 Tarjetas KPI (Figura 13)
        JPanel gridKPI = new JPanel(new GridLayout(1, 4, 14, 0));
        gridKPI.setBackground(EstilosUI.FONDO_GRIS);
        gridKPI.setAlignmentX(Component.LEFT_ALIGNMENT);

        gridKPI.add(EstilosUI.crearTarjetaKPI("Asistencia general", "94 %", null));
        gridKPI.add(EstilosUI.crearTarjetaKPI("Avance del registro de notas", "68 %", null));
        gridKPI.add(EstilosUI.crearTarjetaKPI("Estudiantes en riesgo (C)", "12", null));

        JButton btnCerrarPeriodo = EstilosUI.crearBotonSecundario("Cerrar periodo");
        btnCerrarPeriodo.addActionListener(e -> JOptionPane.showMessageDialog(this,
                "Periodo Bimestre 2 cerrado correctamente (RF07 / CU07).\n" +
                "Se ha bloqueado la edición de notas y asistencia y se registró en Auditoría.",
                "Cierre de Periodo", JOptionPane.INFORMATION_MESSAGE));
        gridKPI.add(EstilosUI.crearTarjetaKPI("Periodo actual", "Bimestre 2", btnCerrarPeriodo));

        norte.add(gridKPI);
        add(norte, BorderLayout.NORTH);

        // Panel central dividido en 2 bloques (Figura 13)
        JPanel centro = new JPanel(new GridLayout(1, 2, 18, 0));
        centro.setBackground(EstilosUI.FONDO_GRIS);

        // Bloque Izquierdo: Distribución de calificativos por grado
        JPanel cardIzq = crearContenedorSeccion("Distribución de calificativos por grado");
        String[] colsDist = {"Grado", "AD · A · B · C"};
        Object[][] datosDist = {
                {"   1.°", new int[]{18, 52, 22, 8}},
                {"   2.°", new int[]{14, 48, 28, 10}},
                {"   3.°", new int[]{20, 44, 24, 12}},
                {"   4.°", new int[]{15, 42, 31, 12}},
                {"   5.°", new int[]{22, 46, 22, 10}}
        };
        JTable tablaDist = new JTable(datosDist, colsDist);
        EstilosUI.estilizarTabla(tablaDist);
        tablaDist.setRowHeight(42);
        tablaDist.getColumnModel().getColumn(0).setMaxWidth(70);
        tablaDist.getColumnModel().getColumn(1).setCellRenderer(new BarraApiladaRenderer());
        cardIzq.add(new JScrollPane(tablaDist), BorderLayout.CENTER);

        // Bloque Derecho: Alertas: estudiantes en riesgo
        JPanel cardDer = crearContenedorSeccion("Alertas: estudiantes en riesgo");
        String[] colsRiesgo = {"Estudiante", "Sección", "Áreas con C"};
        Object[][] datosRiesgo = {
                {"   Estudiante A", "2.° A", "2"},
                {"   Estudiante B", "3.° A", "1"},
                {"   Estudiante C", "4.° A", "3"}
        };
        JTable tablaRiesgo = new JTable(datosRiesgo, colsRiesgo);
        EstilosUI.estilizarTabla(tablaRiesgo);
        tablaRiesgo.setRowHeight(42);
        cardDer.add(new JScrollPane(tablaRiesgo), BorderLayout.CENTER);

        centro.add(cardIzq);
        centro.add(cardDer);
        add(centro, BorderLayout.CENTER);
    }

    private JPanel crearContenedorSeccion(String subtitulo) {
        JPanel p = new JPanel(new BorderLayout(0, 10));
        p.setBackground(Color.WHITE);
        p.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(EstilosUI.BORDE_GRIS, 1, true),
                new EmptyBorder(16, 16, 16, 16)
        ));
        JLabel lbl = new JLabel(subtitulo);
        lbl.setFont(new Font("SansSerif", Font.BOLD, 15));
        lbl.setForeground(EstilosUI.TEXTO_OSCURO);
        p.add(lbl, BorderLayout.NORTH);
        return p;
    }

    // Dibuja la barra horizontal multicolor AD (Verde), A (Azul), B (Ámbar), C (Rojo) de la Figura 13
    private static class BarraApiladaRenderer extends JPanel implements TableCellRenderer {
        private int[] porcentajes = {25, 25, 25, 25};

        public BarraApiladaRenderer() {
            setBackground(Color.WHITE);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            if (value instanceof int[] arr) {
                this.porcentajes = arr;
            }
            return this;
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int padX = 12;
            int barHeight = 14;
            int y = (getHeight() - barHeight) / 2;
            int totalWidth = getWidth() - (padX * 2);

            Color[] colores = {EstilosUI.COLOR_AD, EstilosUI.COLOR_A, EstilosUI.COLOR_B, EstilosUI.COLOR_C};
            int currentX = padX;

            for (int i = 0; i < porcentajes.length; i++) {
                int w = (int) Math.round((porcentajes[i] / 100.0) * totalWidth);
                if (i == porcentajes.length - 1) {
                    w = (padX + totalWidth) - currentX;
                }
                g2.setColor(colores[i]);
                g2.fillRect(currentX, y, Math.max(0, w), barHeight);
                currentX += w;
            }
            g2.dispose();
        }
    }
}
