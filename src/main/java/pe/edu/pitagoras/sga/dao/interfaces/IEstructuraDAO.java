package pe.edu.pitagoras.sga.dao.interfaces;

import java.util.List;
import pe.edu.pitagoras.sga.model.academico.Competencia;
import pe.edu.pitagoras.sga.model.academico.Curso;
import pe.edu.pitagoras.sga.model.academico.Grado;
import pe.edu.pitagoras.sga.model.academico.Seccion;

/** Estructura académica: grados, secciones, turnos, áreas y competencias (RF05). */
public interface IEstructuraDAO {

    List<Grado> listarGrados();

    /** @return secciones ordenadas por grado y letra. */
    List<Seccion> listarSecciones();

    List<String> listarTurnos();

    List<Curso> listarCursos();

    /** @return las competencias del área ordenadas por código. */
    List<Competencia> listarCompetencias(int cursoId);

    /** @return id de la nueva sección. */
    int crearSeccion(int gradoId, String letra, String turno, int usuarioId);

    /** Crea el área con sus competencias (códigos 1, 2, ...). @return id del área. */
    int crearCurso(String nombre, List<String> competencias, int usuarioId);
}
