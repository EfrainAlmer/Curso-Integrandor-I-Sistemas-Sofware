package pe.edu.pitagoras.sga.model.evaluacion;

/**
 * Criterios para consultar calificativos. Los campos en null no filtran.
 *
 * @param periodoId      periodo.
 * @param seccionId      sección.
 * @param cursoSeccionId carga académica.
 * @param alumnoId       alumno.
 */
public record FiltroNotas(Integer periodoId, Integer seccionId, Integer cursoSeccionId, Integer alumnoId) {

    public static FiltroNotas dePeriodo(int periodoId) {
        return new FiltroNotas(periodoId, null, null, null);
    }

    public static FiltroNotas deCarga(int cursoSeccionId, int periodoId) {
        return new FiltroNotas(periodoId, null, cursoSeccionId, null);
    }

    public static FiltroNotas deSeccion(int seccionId, Integer periodoId) {
        return new FiltroNotas(periodoId, seccionId, null, null);
    }

    public static FiltroNotas deAlumno(int alumnoId) {
        return new FiltroNotas(null, null, null, alumnoId);
    }
}
