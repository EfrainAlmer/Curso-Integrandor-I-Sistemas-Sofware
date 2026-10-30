package pe.edu.pitagoras.sga.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import pe.edu.pitagoras.sga.model.academico.Alumno;
import pe.edu.pitagoras.sga.model.evaluacion.AsistenciaRegistro;
import pe.edu.pitagoras.sga.model.evaluacion.Calificativo;
import pe.edu.pitagoras.sga.model.evaluacion.FilaNotas;
import pe.edu.pitagoras.sga.model.evaluacion.NotaRegistro;
import pe.edu.pitagoras.sga.model.evaluacion.ReglaCalificativo;
import pe.edu.pitagoras.sga.model.evaluacion.ResumenAsistencia;

/** Cálculos compartidos por los services (sin acceso a datos, fáciles de probar). */
final class CalculoAcademico {

    /** Alumno en una carga académica. */
    record Clave(int alumnoId, int cursoSeccionId) {
    }

    private CalculoAcademico() {
    }

    /** @return calificativo del área por alumno y carga, a partir de las notas de un periodo. */
    static Map<Clave, Calificativo> areas(List<NotaRegistro> notas) {
        Map<Clave, List<Calificativo>> agrupadas = new LinkedHashMap<>();
        for (NotaRegistro n : notas) {
            agrupadas.computeIfAbsent(new Clave(n.alumnoId(), n.cursoSeccionId()), k -> new ArrayList<>())
                    .add(n.calificativo());
        }
        Map<Clave, Calificativo> resultado = new LinkedHashMap<>();
        agrupadas.forEach((k, v) -> ReglaCalificativo.calificativoArea(v).ifPresent(c -> resultado.put(k, c)));
        return resultado;
    }

    /** @return totales de asistencia por alumno y carga. */
    static Map<Clave, ResumenAsistencia> asistencia(List<AsistenciaRegistro> registros) {
        Map<Clave, List<AsistenciaRegistro>> agrupadas = new HashMap<>();
        for (AsistenciaRegistro r : registros) {
            agrupadas.computeIfAbsent(new Clave(r.alumnoId(), r.cursoSeccionId()), k -> new ArrayList<>()).add(r);
        }
        Map<Clave, ResumenAsistencia> resultado = new HashMap<>();
        agrupadas.forEach((k, v) -> resultado.put(k, ResumenAsistencia.de(v)));
        return resultado;
    }

    /** @return una fila por alumno con los calificativos ya registrados de una carga. */
    static List<FilaNotas> filasNotas(List<Alumno> alumnos, List<NotaRegistro> notas) {
        Map<Integer, FilaNotas> filas = new LinkedHashMap<>();
        for (Alumno a : alumnos) {
            filas.put(a.alumnoId(), new FilaNotas(a.alumnoId(), a.dni(), a.nombreCompleto()));
        }
        for (NotaRegistro n : notas) {
            FilaNotas f = filas.get(n.alumnoId());
            if (f != null) {
                f.cargar(n.codigoCompetencia(), n.calificativo());
            }
        }
        return new ArrayList<>(filas.values());
    }
}
