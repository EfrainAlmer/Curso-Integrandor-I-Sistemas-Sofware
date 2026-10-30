package pe.edu.pitagoras.sga.model.evaluacion;

/**
 * Un calificativo guardado: alumno, competencia de un área en una sección y periodo.
 *
 * @param alumnoId          alumno.
 * @param cursoSeccionId    carga académica.
 * @param cursoId           área.
 * @param curso             nombre del área.
 * @param codigoCompetencia '1' a '4'.
 * @param periodoId         periodo.
 * @param bimestre          1 a 4.
 * @param calificativo      AD, A, B o C.
 */
public record NotaRegistro(int alumnoId, int cursoSeccionId, int cursoId, String curso,
                           String codigoCompetencia, int periodoId, int bimestre, Calificativo calificativo) {
}
