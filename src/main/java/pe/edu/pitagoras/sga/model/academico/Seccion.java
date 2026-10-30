package pe.edu.pitagoras.sga.model.academico;

/**
 * Sección de un grado, por ejemplo "3.° B" del turno Mañana.
 *
 * @param seccionId identificador.
 * @param gradoId   grado al que pertenece.
 * @param grado     nombre del grado ("3.°").
 * @param letra     letra de la sección ("B").
 * @param turno     "Mañana" o "Tarde".
 */
public record Seccion(int seccionId, int gradoId, String grado, String letra, String turno) {

    /** @return texto corto para combos y reportes, por ejemplo "3.° B". */
    public String etiqueta() {
        return grado + " " + letra;
    }

    @Override
    public String toString() {
        return etiqueta() + " - " + turno;
    }
}
