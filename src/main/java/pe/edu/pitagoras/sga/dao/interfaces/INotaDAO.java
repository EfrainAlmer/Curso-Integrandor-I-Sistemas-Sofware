package pe.edu.pitagoras.sga.dao.interfaces;

import java.util.List;
import pe.edu.pitagoras.sga.model.evaluacion.CambioNota;
import pe.edu.pitagoras.sga.model.evaluacion.FiltroNotas;
import pe.edu.pitagoras.sga.model.evaluacion.NotaRegistro;

/** Calificativos (RF13). */
public interface INotaDAO {

    List<NotaRegistro> listar(FiltroNotas filtro);

    /**
     * Guarda en lote los calificativos de una carga y periodo. Cada valor que cambia
     * queda en la bitácora con su valor anterior (RF09). Todo en una transacción.
     *
     * @return cantidad de calificativos que realmente cambiaron.
     */
    int guardar(int cursoSeccionId, int periodoId, List<CambioNota> cambios, int usuarioId);
}
