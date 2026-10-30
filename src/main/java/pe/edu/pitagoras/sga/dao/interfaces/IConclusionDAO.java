package pe.edu.pitagoras.sga.dao.interfaces;

import java.util.List;
import java.util.Map;
import pe.edu.pitagoras.sga.model.evaluacion.Conclusion;
import pe.edu.pitagoras.sga.model.evaluacion.FiltroNotas;

/** Conclusiones descriptivas por alumno, competencia y periodo. */
public interface IConclusionDAO {

    List<Conclusion> listar(FiltroNotas filtro);

    /**
     * Guarda las conclusiones de un alumno en una carga y periodo. Un texto vacío borra la
     * conclusión. Los cambios quedan en la bitácora.
     *
     * @param textos código de competencia → texto.
     * @return cantidad de conclusiones que cambiaron.
     */
    int guardar(int cursoSeccionId, int periodoId, int alumnoId, Map<String, String> textos, int usuarioId);
}
