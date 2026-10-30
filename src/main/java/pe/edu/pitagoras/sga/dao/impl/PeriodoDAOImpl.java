package pe.edu.pitagoras.sga.dao.impl;

import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import pe.edu.pitagoras.sga.dao.interfaces.IPeriodoDAO;
import pe.edu.pitagoras.sga.dao.support.JdbcSupport;
import pe.edu.pitagoras.sga.model.academico.Periodo;
import pe.edu.pitagoras.sga.model.evaluacion.DetalleRendimiento;

/** JDBC de Periodo y DetalleRendimiento. */
public class PeriodoDAOImpl extends JdbcSupport implements IPeriodoDAO {

    private static final String SQL_BASE = "SELECT p.PeriodoId, p.AnioLectivo, p.BimestreNum, p.FechaInicio, "
            + "p.FechaFin, p.Cerrado, p.FechaCierre, u.Username AS CerradoPor "
            + "FROM Periodo p LEFT JOIN Usuario u ON u.UsuarioId = p.CerradoPorUsuarioId ";

    @Override
    public List<Periodo> listar() {
        return consultar(SQL_BASE + "ORDER BY p.AnioLectivo, p.BimestreNum", ps -> { },
                PeriodoDAOImpl::mapear, "No se pudo listar los periodos");
    }

    @Override
    public Optional<Periodo> buscar(int periodoId) {
        return consultarUno(SQL_BASE + "WHERE p.PeriodoId = ?", ps -> ps.setInt(1, periodoId),
                PeriodoDAOImpl::mapear, "No se pudo consultar el periodo");
    }

    @Override
    public void actualizarFechas(int periodoId, LocalDate inicio, LocalDate fin, int usuarioId) {
        enTransaccion(cn -> {
            Periodo antes = consultar(cn, SQL_BASE + "WHERE p.PeriodoId = ?", ps -> ps.setInt(1, periodoId),
                    PeriodoDAOImpl::mapear).get(0);
            actualizar(cn, "UPDATE Periodo SET FechaInicio = ?, FechaFin = ? WHERE PeriodoId = ?", ps -> {
                ps.setDate(1, Date.valueOf(inicio));
                ps.setDate(2, Date.valueOf(fin));
                ps.setInt(3, periodoId);
            });
            auditar(cn, usuarioId, "Periodo", "PeriodoId=" + periodoId,
                    antes.fechaInicio() + " a " + antes.fechaFin(), inicio + " a " + fin, "CONFIGURAR");
            return null;
        }, "No se pudo actualizar las fechas del periodo");
    }

    @Override
    public void cerrar(int periodoId, int usuarioId, List<DetalleRendimiento> consolidado) {
        enTransaccion(cn -> {
            int filas = actualizar(cn, "UPDATE Periodo SET Cerrado = 1, FechaCierre = ?, CerradoPorUsuarioId = ? "
                    + "WHERE PeriodoId = ? AND Cerrado = 0", ps -> {
                        ps.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()));
                        ps.setInt(2, usuarioId);
                        ps.setInt(3, periodoId);
                    });
            if (filas == 0) {
                throw new SQLException("El periodo ya estaba cerrado");
            }
            // El consolidado se recalcula en cada cierre (por si el periodo se reabrió y se corrigió).
            actualizar(cn, "DELETE FROM DetalleRendimiento WHERE PeriodoId = ?", ps -> ps.setInt(1, periodoId));
            try (var ps = cn.prepareStatement("INSERT INTO DetalleRendimiento (AlumnoId, CursoSeccionId, PeriodoId, "
                    + "Calificativo, TotalAsistencias, TotalFaltas, TotalTardanzas, TotalJustificadas, "
                    + "EstadoAprobacion, UltimaActualizacion) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
                Timestamp ahora = Timestamp.valueOf(LocalDateTime.now());
                for (DetalleRendimiento d : consolidado) {
                    ps.setInt(1, d.alumnoId());
                    ps.setInt(2, d.cursoSeccionId());
                    ps.setInt(3, periodoId);
                    ps.setString(4, d.calificativo().name());
                    ps.setInt(5, d.asistencia().asistencias());
                    ps.setInt(6, d.asistencia().faltas());
                    ps.setInt(7, d.asistencia().tardanzas());
                    ps.setInt(8, d.asistencia().justificadas());
                    ps.setString(9, d.estadoAprobacion());
                    ps.setTimestamp(10, ahora);
                    ps.addBatch();
                }
                ps.executeBatch();
            }
            auditar(cn, usuarioId, "Periodo", "PeriodoId=" + periodoId, "ABIERTO",
                    "CERRADO (" + consolidado.size() + " registros consolidados)", "CERRAR_PERIODO");
            return null;
        }, "No se pudo cerrar el periodo");
    }

    @Override
    public void reabrir(int periodoId, int usuarioId, String motivo) {
        enTransaccion(cn -> {
            actualizar(cn, "UPDATE Periodo SET Cerrado = 0, FechaCierre = NULL, CerradoPorUsuarioId = NULL "
                    + "WHERE PeriodoId = ?", ps -> ps.setInt(1, periodoId));
            auditar(cn, usuarioId, "Periodo", "PeriodoId=" + periodoId, "CERRADO",
                    "ABIERTO. Motivo: " + motivo, "REABRIR_PERIODO");
            return null;
        }, "No se pudo reabrir el periodo");
    }

    private static Periodo mapear(ResultSet rs) throws SQLException {
        return new Periodo(rs.getInt("PeriodoId"), rs.getInt("AnioLectivo"), rs.getInt("BimestreNum"),
                fecha(rs, "FechaInicio"), fecha(rs, "FechaFin"), rs.getBoolean("Cerrado"),
                fechaHora(rs, "FechaCierre"), texto(rs, "CerradoPor"));
    }
}
