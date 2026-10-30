package pe.edu.pitagoras.sga.view.apoderado;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import pe.edu.pitagoras.sga.controller.ApoderadoController;
import pe.edu.pitagoras.sga.model.evaluacion.Calificativo;
import pe.edu.pitagoras.sga.model.evaluacion.Libreta;
import pe.edu.pitagoras.sga.model.evaluacion.LineaLibreta;
import pe.edu.pitagoras.sga.model.evaluacion.ResumenArea;
import pe.edu.pitagoras.sga.model.evaluacion.ResumenAsistencia;
import pe.edu.pitagoras.sga.view.comun.Archivos;
import pe.edu.pitagoras.sga.view.comun.ModeloTabla;
import pe.edu.pitagoras.sga.view.comun.PanelModulo;
import pe.edu.pitagoras.sga.view.comun.RenderInsignia;
import pe.edu.pitagoras.sga.view.comun.Tarea;
import pe.edu.pitagoras.sga.view.comun.Tarjeta;
import pe.edu.pitagoras.sga.view.comun.Tema;

/**
 * RF17: libreta del año con el formato del Informe de progreso del MINEDU: competencias por bimestre,
 * nivel al finalizar el año (bimestre 4, RF14), asistencia por bimestre y situación final.
 */
public class PnlLibreta extends PanelModulo {

    /** Fila de la tabla: resumen de un área o una de sus competencias. */
    private record Fila(String area, String competencia, Map<Integer, Calificativo> bimestres,
                        Optional<Calificativo> fin, boolean resumen) {
    }

    private final transient ApoderadoController controller;
    private final ModeloTabla<Fila> modelo = new ModeloTabla<Fila>()
            .columna("Área curricular", f -> f.resumen() ? f.area() : "")
            .columna("Competencia", Fila::competencia)
            .columna("1.er bim.", Calificativo.class, f -> f.bimestres().get(1))
            .columna("2.° bim.", Calificativo.class, f -> f.bimestres().get(2))
            .columna("3.er bim.", Calificativo.class, f -> f.bimestres().get(3))
            .columna("4.° bim.", Calificativo.class, f -> f.bimestres().get(4))
            .columna("Nivel final", Calificativo.class, f -> f.fin().orElse(null));
    private final ModeloTabla<Object[]> modeloAsistencia = new ModeloTabla<Object[]>()
            .columna("", f -> f[0]).columna("1.er bim.", f -> f[1]).columna("2.° bim.", f -> f[2])
            .columna("3.er bim.", f -> f[3]).columna("4.° bim.", f -> f[4]).columna("Total", f -> f[5]);
    private final JLabel lblSituacion = Tema.seccion(" ");
    private Libreta libreta;

    public PnlLibreta(ApoderadoController controller) {
        super("Libreta de notas", "Informe de progreso del aprendizaje del estudiante");
        this.controller = controller;
        JButton pdf = Tema.botonPrimario("Descargar / imprimir PDF");
        accion(pdf);

        JTable tabla = new JTable(modelo);
        tabla.setRowSelectionAllowed(false);
        TableCellRenderer texto = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object v, boolean s, boolean f, int r, int c) {
                super.getTableCellRendererComponent(t, v, false, false, r, c);
                boolean resumen = modelo.getFila(r).resumen();
                setFont(getFont().deriveFont(resumen ? Font.BOLD : Font.PLAIN));
                setBackground(resumen ? Tema.FONDO_RESUMEN : t.getBackground());
                setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 12));
                return this;
            }
        };
        tabla.getColumnModel().getColumn(0).setCellRenderer(texto);
        tabla.getColumnModel().getColumn(1).setCellRenderer(texto);
        for (int c = 2; c < 7; c++) {
            tabla.getColumnModel().getColumn(c).setCellRenderer(new RenderInsignia());
            tabla.getColumnModel().getColumn(c).setPreferredWidth(90);
        }
        tabla.getColumnModel().getColumn(0).setPreferredWidth(170);
        tabla.getColumnModel().getColumn(1).setPreferredWidth(430);
        Tarjeta cardTabla = new Tarjeta();
        cardTabla.setLayout(new BorderLayout(0, 8));
        cardTabla.add(Tema.tabla(tabla), BorderLayout.CENTER);
        cardTabla.add(Tema.subtitulo("Nivel final = nivel alcanzado en el 4.° bimestre (RF14). En negrita, el "
                + "calificativo de cada área. Las conclusiones descriptivas del docente están en el PDF."),
                BorderLayout.SOUTH);

        JTable tablaAsis = new JTable(modeloAsistencia);
        tablaAsis.setRowSelectionAllowed(false);
        tablaAsis.getColumnModel().getColumn(0).setPreferredWidth(260);
        Tarjeta cardAsis = new Tarjeta();
        cardAsis.setLayout(new BorderLayout(0, 8));
        cardAsis.add(Tema.seccion("Asistencia por bimestre"), BorderLayout.NORTH);
        var spAsis = Tema.tabla(tablaAsis);
        spAsis.setPreferredSize(new java.awt.Dimension(0, 210));
        cardAsis.add(spAsis, BorderLayout.CENTER);
        JPanel situacion = new JPanel(new GridLayout(2, 1, 0, 4));
        situacion.setOpaque(false);
        situacion.add(Tema.subtitulo("Situación final"));
        situacion.add(lblSituacion);
        cardAsis.add(situacion, BorderLayout.SOUTH);

        contenido.add(cardTabla, BorderLayout.CENTER);
        contenido.add(cardAsis, BorderLayout.SOUTH);

        pdf.addActionListener(e -> {
            if (libreta != null) {
                Archivos.guardar(this, "Libreta_" + libreta.alumno().apellido() + "_" + libreta.anio() + ".pdf",
                        controller::exportarLibreta);
            }
        });
    }

    @Override
    protected void alMostrar(boolean primeraVez) {
        Tarea.ejecutar(this, controller::libreta, this::mostrar);
    }

    private void mostrar(Libreta l) {
        libreta = l;
        setCabecera("Libreta de notas " + l.anio() + " · " + l.alumno().nombre() + " " + l.alumno().apellido(),
                l.alumno().seccion() + " · DNI " + l.alumno().dni() + " · " + l.colegio().nombre());
        List<Fila> filas = new ArrayList<>();
        for (ResumenArea r : l.areas()) {
            filas.add(new Fila(r.area(), "Calificativo del área", r.porBimestre(), r.anual(), true));
            for (LineaLibreta ln : l.lineas()) {
                if (ln.area().equals(r.area())) {
                    filas.add(new Fila(ln.area(), ln.competencia(), ln.porBimestre(), ln.nivelFinal(), false));
                }
            }
        }
        modelo.setFilas(filas);
        List<Object[]> asis = new ArrayList<>();
        asis.add(filaAsistencia("Inasistencias justificadas", l, ResumenAsistencia::justificadas));
        asis.add(filaAsistencia("Inasistencias injustificadas", l, ResumenAsistencia::faltas));
        asis.add(filaAsistencia("Tardanzas", l, ResumenAsistencia::tardanzas));
        asis.add(filaAsistencia("Asistencias", l, ResumenAsistencia::asistencias));
        modeloAsistencia.setFilas(asis);
        lblSituacion.setText(l.situacionFinal());
        lblSituacion.setForeground(l.situacionFinal().startsWith("Promovido") ? Tema.EXITO
                : l.situacionFinal().startsWith("En proceso") ? Tema.MARINO : Tema.PELIGRO);
    }

    private static Object[] filaAsistencia(String titulo, Libreta l, java.util.function.ToIntFunction<ResumenAsistencia> f) {
        Object[] fila = new Object[6];
        fila[0] = titulo;
        for (int b = 1; b <= 4; b++) {
            ResumenAsistencia r = l.asistenciaBimestre().get(b);
            fila[b] = r == null ? "-" : f.applyAsInt(r);
        }
        fila[5] = f.applyAsInt(l.asistencia());
        return fila;
    }

}
