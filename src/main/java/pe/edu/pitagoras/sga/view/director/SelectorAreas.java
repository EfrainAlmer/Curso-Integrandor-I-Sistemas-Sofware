package pe.edu.pitagoras.sga.view.director;

import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import pe.edu.pitagoras.sga.model.academico.Curso;

/** Casillas para marcar las áreas que dicta un docente (de 1 a 3). */
class SelectorAreas extends JPanel {

    private static final int MAXIMO = 3;
    private final List<JCheckBox> casillas = new ArrayList<>();
    private final List<Curso> cursos;

    SelectorAreas(List<Curso> cursos, List<Curso> marcadas) {
        super(new GridLayout(0, 2, 6, 2));
        setOpaque(false);
        this.cursos = cursos;
        for (Curso c : cursos) {
            JCheckBox cb = new JCheckBox(c.nombre());
            cb.setOpaque(false);
            cb.setSelected(marcadas.stream().anyMatch(m -> m.cursoId() == c.cursoId()));
            cb.addActionListener(e -> limitar());
            casillas.add(cb);
            add(cb);
        }
        limitar();
    }

    /** Al llegar a 3 áreas se desactivan las demás casillas. */
    private void limitar() {
        long marcadas = casillas.stream().filter(JCheckBox::isSelected).count();
        casillas.forEach(cb -> cb.setEnabled(cb.isSelected() || marcadas < MAXIMO));
    }

    List<Curso> seleccionadas() {
        List<Curso> r = new ArrayList<>();
        for (int i = 0; i < casillas.size(); i++) {
            if (casillas.get(i).isSelected()) {
                r.add(cursos.get(i));
            }
        }
        return r;
    }
}
