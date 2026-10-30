package pe.edu.pitagoras.sga.view.docente;

import java.awt.Component;
import java.awt.Dimension;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import net.miginfocom.swing.MigLayout;
import pe.edu.pitagoras.sga.controller.DocenteController;
import pe.edu.pitagoras.sga.model.academico.CargaAcademica;
import pe.edu.pitagoras.sga.model.academico.Competencia;
import pe.edu.pitagoras.sga.model.academico.Periodo;
import pe.edu.pitagoras.sga.model.evaluacion.Calificativo;
import pe.edu.pitagoras.sga.model.evaluacion.FilaNotas;
import pe.edu.pitagoras.sga.view.comun.Mensajes;
import pe.edu.pitagoras.sga.view.comun.Tarea;
import pe.edu.pitagoras.sga.view.comun.Tema;

/**
 * Conclusiones descriptivas de un estudiante (logros, dificultades y recomendaciones) por
 * competencia, como en el Informe de progreso del MINEDU. Salen en la libreta.
 */
class DialogoConclusiones extends JDialog {

    private static final int MAXIMO = 500;

    DialogoConclusiones(Component padre, DocenteController controller, CargaAcademica carga, Periodo periodo,
                        List<Competencia> competencias, FilaNotas fila, Map<String, String> actuales) {
        super(SwingUtilities.getWindowAncestor(padre), "Conclusiones descriptivas", ModalityType.APPLICATION_MODAL);
        boolean editable = !periodo.cerrado();
        JPanel p = new JPanel(new MigLayout("insets 22, wrap, fillx", "[620!]"));
        p.setBackground(Tema.BLANCO);
        p.add(Tema.seccion("Conclusiones descriptivas · " + fila.getAlumno()));
        p.add(Tema.subtitulo(carga.curso().nombre() + " · " + carga.seccion().etiqueta() + " · " + periodo.etiqueta()
                + (editable ? "" : " (cerrado: solo lectura)")), "gapbottom 10");
        Map<String, JTextArea> areas = new LinkedHashMap<>();
        for (Competencia c : competencias) {
            Calificativo cal = fila.get(c.codigo());
            JLabel titulo = new JLabel("<html><b>" + c.nombre() + "</b> &nbsp;·&nbsp; Nivel: "
                    + (cal == null ? "sin calificativo" : cal.name() + " (" + cal.getDescripcion().toLowerCase() + ")")
                    + "</html>");
            JTextArea texto = new JTextArea(actuales.getOrDefault(c.codigo(), ""), 3, 40);
            texto.setLineWrap(true);
            texto.setWrapStyleWord(true);
            texto.setEditable(editable);
            JLabel contador = Tema.subtitulo("");
            Runnable contar = () -> {
                int n = texto.getText().length();
                contador.setText(n + " / " + MAXIMO);
                contador.setForeground(n > MAXIMO ? Tema.PELIGRO : Tema.TEXTO_SUAVE);
            };
            contar.run();
            texto.getDocument().addDocumentListener(new DocumentListener() {
                public void insertUpdate(DocumentEvent e) { contar.run(); }
                public void removeUpdate(DocumentEvent e) { contar.run(); }
                public void changedUpdate(DocumentEvent e) { contar.run(); }
            });
            JScrollPane sp = new JScrollPane(texto);
            sp.setBorder(BorderFactory.createLineBorder(Tema.BORDE));
            sp.setPreferredSize(new Dimension(620, 70));
            p.add(titulo, "gaptop 6");
            p.add(sp, "growx");
            p.add(contador, "right");
            areas.put(c.codigo(), texto);
        }
        p.add(Tema.subtitulo("<html><p width='600'>Ejemplo: \"Resuelve problemas con números racionales. Aún "
                + "confunde la jerarquía de operaciones; se recomienda practicar ejercicios combinados.\"</p></html>"),
                "gaptop 6");
        JButton cerrar = Tema.botonSecundario(editable ? "Cancelar" : "Cerrar");
        cerrar.addActionListener(e -> dispose());
        if (editable) {
            JButton guardar = Tema.botonPrimario("Guardar conclusiones");
            guardar.addActionListener(e -> {
                Map<String, String> textos = new LinkedHashMap<>();
                areas.forEach((k, v) -> textos.put(k, v.getText()));
                guardar.setEnabled(false);
                Tarea.ejecutar(this, () -> controller.guardarConclusiones(carga, periodo, fila.getAlumnoId(), textos),
                        n -> {
                            Mensajes.info(this, n == 0 ? "No había cambios." : n + " conclusión(es) guardada(s).");
                            dispose();
                        }, () -> guardar.setEnabled(true));
            });
            p.add(cerrar, "split 2, right, gaptop 12");
            p.add(guardar);
        } else {
            p.add(cerrar, "right, gaptop 12");
        }
        JScrollPane raiz = new JScrollPane(p);
        raiz.setBorder(null);
        setContentPane(raiz);
        pack();
        setSize(getWidth() + 20, Math.min(getHeight(), 760));
        setLocationRelativeTo(getOwner());
    }
}
