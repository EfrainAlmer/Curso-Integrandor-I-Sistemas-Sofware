package pe.edu.utp.pitagoras.dao;

import pe.edu.utp.pitagoras.model.BitacoraEvento;
import pe.edu.utp.pitagoras.model.CalificacionCompetencia;
import pe.edu.utp.pitagoras.model.RegistroAsistencia;

import java.util.List;

/**
 * Interfaz DAO para las operaciones de persistencia del módulo Docente.
 * <p>
 * Aplica los principios <b>SOLID</b> exigidos en la consigna del Avance de Proyecto Final 3:
 * <ul>
 *   <li><b>S (Single Responsibility):</b> Define exclusivamente el contrato de persistencia académica del docente.</li>
 *   <li><b>I (Interface Segregation):</b> Separa las operaciones del docente de las operaciones administrativas del director.</li>
 *   <li><b>D (Dependency Inversion):</b> {@code DocenteController} depende de esta abstracción y no de detalles concretos de SQL.</li>
 * </ul>
 *
 * @author Angelo Giuseppe Paredes Quispe
 * @version 1.0
 */
public interface IDocenteDAO {

    /**
     * Persiste en lote los registros de asistencia diaria de una sección y sus eventos de auditoría asociados.
     *
     * @param registros        lista de asistencias capturadas por el docente (RF12 / CU12)
     * @param eventosAuditoria lista de eventos generados para la bitácora (RF09 / CU09)
     * @return {@code true} si la transacción en base de datos se completó; {@code false} si opera en modo local en memoria
     */
    boolean guardarLoteAsistenciaSQL(List<RegistroAsistencia> registros, List<BitacoraEvento> eventosAuditoria);

    /**
     * Persiste las calificaciones por competencia en escala literal MINEDU (AD, A, B, C) y registra la auditoría.
     *
     * @param calificaciones   lista de calificativos por estudiante y competencia (RF13 / CU13)
     * @param eventosAuditoria lista de cambios detectados (valor anterior vs. valor nuevo) para la bitácora (RF09)
     * @return {@code true} si la transacción SQL se ejecutó; {@code false} si opera en modo local en memoria
     */
    boolean guardarCalificativosSQL(List<CalificacionCompetencia> calificaciones, List<BitacoraEvento> eventosAuditoria);
}
