package pe.edu.pitagoras.sga.model.evaluacion;

/**
 * Consolidado de un alumno en un área al cerrar el bimestre (RF07, RF19).
 *
 * @param alumnoId       alumno.
 * @param cursoSeccionId carga académica.
 * @param calificativo   calificativo del área en el periodo.
 * @param asistencia     totales de asistencia del periodo en esa área.
 */
public record DetalleRendimiento(int alumnoId, int cursoSeccionId, Calificativo calificativo,
                                 ResumenAsistencia asistencia) {

    public String estadoAprobacion() {
        return ReglaCalificativo.estadoAprobacion(calificativo);
    }
}
