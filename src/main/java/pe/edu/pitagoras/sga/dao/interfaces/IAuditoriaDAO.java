package pe.edu.pitagoras.sga.dao.interfaces;

import java.time.LocalDate;
import java.util.List;
import pe.edu.pitagoras.sga.model.gestion.EventoAuditoria;

/** Bitácora de auditoría (RF09). */
public interface IAuditoriaDAO {

    /**
     * @param desde  primera fecha (incluida).
     * @param hasta  última fecha (incluida).
     * @param accion acción a filtrar, o null para todas.
     * @param limite máximo de filas.
     * @return eventos del más reciente al más antiguo.
     */
    List<EventoAuditoria> listar(LocalDate desde, LocalDate hasta, String accion, int limite);
}
