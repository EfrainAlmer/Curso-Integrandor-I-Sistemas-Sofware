package pe.edu.pitagoras.sga.model.evaluacion;

import java.util.Map;
import java.util.Optional;

/**
 * Una fila de la libreta: una competencia de un área con su nivel en cada bimestre y el nivel
 * al finalizar el año (el del bimestre 4, RF14).
 *
 * @param area        nombre del área.
 * @param competencia nombre de la competencia.
 * @param porBimestre calificativo por bimestre (clave 1 a 4; puede faltar).
 * @param nivelFinal  nivel al finalizar el periodo lectivo, vacío si el bimestre 4 aún no se califica.
 */
public record LineaLibreta(String area, String competencia, Map<Integer, Calificativo> porBimestre,
                           Optional<Calificativo> nivelFinal) {

    /** @return el calificativo del bimestre, o null si aún no tiene. */
    public Calificativo bimestre(int numero) {
        return porBimestre.get(numero);
    }
}
