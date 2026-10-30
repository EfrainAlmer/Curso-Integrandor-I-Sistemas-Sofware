package pe.edu.pitagoras.sga.dao.interfaces;

import java.time.LocalDate;
import java.util.List;
import pe.edu.pitagoras.sga.model.evaluacion.AsistenciaRegistro;
import pe.edu.pitagoras.sga.model.evaluacion.FiltroAsistencia;
import pe.edu.pitagoras.sga.model.evaluacion.MarcaAsistencia;

/** Asistencia diaria (RF12, RF16). */
public interface IAsistenciaDAO {

    List<AsistenciaRegistro> listar(FiltroAsistencia filtro);

    /**
     * Guarda en lote la asistencia de una carga en una fecha. Las modificaciones de marcas
     * ya existentes quedan en la bitácora (RF09). Todo en una transacción.
     *
     * @return cantidad de marcas nuevas o modificadas.
     */
    int guardar(int cursoSeccionId, LocalDate fecha, List<MarcaAsistencia> marcas, int usuarioId);
}
