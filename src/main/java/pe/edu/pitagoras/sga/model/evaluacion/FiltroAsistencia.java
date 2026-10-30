package pe.edu.pitagoras.sga.model.evaluacion;

import java.time.LocalDate;

/**
 * Criterios para consultar asistencia. Los campos en null no filtran.
 *
 * @param seccionId      sección.
 * @param cursoSeccionId carga académica.
 * @param alumnoId       alumno.
 * @param desde          primera fecha (incluida).
 * @param hasta          última fecha (incluida).
 */
public record FiltroAsistencia(Integer seccionId, Integer cursoSeccionId, Integer alumnoId,
                               LocalDate desde, LocalDate hasta) {

    public static FiltroAsistencia deCargaEnFecha(int cursoSeccionId, LocalDate fecha) {
        return new FiltroAsistencia(null, cursoSeccionId, null, fecha, fecha);
    }

    public static FiltroAsistencia deAlumno(int alumnoId, LocalDate desde, LocalDate hasta) {
        return new FiltroAsistencia(null, null, alumnoId, desde, hasta);
    }

    public static FiltroAsistencia entre(LocalDate desde, LocalDate hasta) {
        return new FiltroAsistencia(null, null, null, desde, hasta);
    }

    public static FiltroAsistencia deSeccion(int seccionId, LocalDate desde, LocalDate hasta) {
        return new FiltroAsistencia(seccionId, null, null, desde, hasta);
    }
}
