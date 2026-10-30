package pe.edu.pitagoras.sga.view.docente;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.swing.JComboBox;
import pe.edu.pitagoras.sga.model.academico.CargaAcademica;
import pe.edu.pitagoras.sga.model.academico.Curso;
import pe.edu.pitagoras.sga.model.academico.Seccion;
import pe.edu.pitagoras.sga.view.comun.Combos;

/**
 * Par de combos Sección y Área enlazados: al elegir la sección solo aparecen las áreas que el
 * docente dicta en ella. Juntos identifican una carga académica.
 */
class SelectorCarga {

    final JComboBox<Seccion> cboSeccion = Combos.nuevo();
    final JComboBox<Curso> cboArea = Combos.nuevo();
    private List<CargaAcademica> cargas = List.of();
    private Runnable alCambiar = () -> { };
    private boolean llenando;

    SelectorCarga() {
        cboSeccion.setPreferredSize(new java.awt.Dimension(140, 36));
        cboArea.setPreferredSize(new java.awt.Dimension(210, 36));
        cboSeccion.addActionListener(e -> {
            if (!llenando) {
                llenarAreas();
                avisar();
            }
        });
        cboArea.addActionListener(e -> avisar());
    }

    void setAlCambiar(Runnable r) {
        alCambiar = r;
    }

    void setCargas(List<CargaAcademica> nuevas) {
        llenando = true;
        cargas = nuevas;
        Map<Integer, Seccion> secciones = new LinkedHashMap<>();
        nuevas.forEach(c -> secciones.putIfAbsent(c.seccion().seccionId(), c.seccion()));
        Combos.llenar(cboSeccion, List.copyOf(secciones.values()));
        llenarAreas();
        llenando = false;
    }

    private void llenarAreas() {
        boolean antes = llenando;
        llenando = true;
        Seccion s = Combos.seleccionado(cboSeccion);
        Combos.llenar(cboArea, cargas.stream()
                .filter(c -> s != null && c.seccion().seccionId() == s.seccionId())
                .map(CargaAcademica::curso).toList());
        llenando = antes;
    }

    private void avisar() {
        if (!llenando) {
            alCambiar.run();
        }
    }

    /** @return la carga de la sección y área elegidas, o null. */
    CargaAcademica getCarga() {
        Seccion s = Combos.seleccionado(cboSeccion);
        Curso a = Combos.seleccionado(cboArea);
        return cargas.stream().filter(c -> s != null && a != null && c.seccion().seccionId() == s.seccionId()
                && c.curso().cursoId() == a.cursoId()).findFirst().orElse(null);
    }

    /** Vuelve a seleccionar una carga (al cancelar un cambio con datos sin guardar). */
    void seleccionar(CargaAcademica c) {
        if (c == null) {
            return;
        }
        llenando = true;
        Combos.seleccionar(cboSeccion, s -> s.seccionId() == c.seccion().seccionId());
        llenarAreas();
        Combos.seleccionar(cboArea, a -> a.cursoId() == c.curso().cursoId());
        llenando = false;
    }
}
