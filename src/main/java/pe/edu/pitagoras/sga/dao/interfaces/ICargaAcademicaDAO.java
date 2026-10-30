package pe.edu.pitagoras.sga.dao.interfaces;

import java.util.List;
import java.util.Optional;
import pe.edu.pitagoras.sga.model.academico.CargaAcademica;

/** Asignación de áreas y secciones a los docentes (RF06, RF11). */
public interface ICargaAcademicaDAO {

    List<CargaAcademica> listar();

    List<CargaAcademica> listarPorDocente(int docenteId);

    Optional<CargaAcademica> buscar(int cursoSeccionId);

    /** @return el docente vinculado a la cuenta, o vacío si la cuenta no es de un docente. */
    Optional<Integer> buscarDocenteIdPorUsuario(int usuarioId);

    /**
     * Asigna (o reasigna) el área de una sección a un docente. Si la carga es nueva,
     * crea sus evaluaciones en todos los periodos del año.
     */
    void asignar(int cursoId, int seccionId, int docenteId, int usuarioId);
}
