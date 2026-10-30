package pe.edu.pitagoras.sga.dao.interfaces;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import pe.edu.pitagoras.sga.model.academico.Periodo;
import pe.edu.pitagoras.sga.model.evaluacion.DetalleRendimiento;

/** Bimestres del año escolar (RF05, RF07). */
public interface IPeriodoDAO {

    /** @return periodos ordenados por año y bimestre. */
    List<Periodo> listar();

    Optional<Periodo> buscar(int periodoId);

    void actualizarFechas(int periodoId, LocalDate inicio, LocalDate fin, int usuarioId);

    /** Cierra el periodo, guarda el consolidado y lo registra en la bitácora, en una sola transacción. */
    void cerrar(int periodoId, int usuarioId, List<DetalleRendimiento> consolidado);

    /** Reabre el periodo y lo registra en la bitácora. */
    void reabrir(int periodoId, int usuarioId, String motivo);
}
