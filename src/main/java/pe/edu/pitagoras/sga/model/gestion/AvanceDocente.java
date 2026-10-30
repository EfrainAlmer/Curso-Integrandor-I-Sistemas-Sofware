package pe.edu.pitagoras.sga.model.gestion;

/**
 * Avance del registro de notas de un docente en el periodo (RF08).
 *
 * @param docente     nombre del docente.
 * @param registradas calificativos ya registrados.
 * @param esperados   calificativos que debería registrar (alumnos x competencias).
 */
public record AvanceDocente(String docente, int registradas, int esperados) {

    public double porcentaje() {
        return esperados == 0 ? 100.0 : registradas * 100.0 / esperados;
    }
}
