package pe.edu.pitagoras.sga.model.evaluacion;

import java.util.Map;
import java.util.Optional;

/**
 * Calificativo de un área por bimestre (consolidado de sus competencias) y el anual (RF14).
 *
 * @param area        nombre del área.
 * @param porBimestre calificativo del área por bimestre.
 * @param anual       calificativo anual (bimestre 4), vacío si aún no existe.
 */
public record ResumenArea(String area, Map<Integer, Calificativo> porBimestre, Optional<Calificativo> anual) {

    public Calificativo bimestre(int numero) {
        return porBimestre.get(numero);
    }
}
