package pe.edu.utp.pitagoras.dao;

import pe.edu.utp.pitagoras.config.ConexionBD;
import pe.edu.utp.pitagoras.model.BitacoraEvento;
import pe.edu.utp.pitagoras.model.CalificacionCompetencia;
import pe.edu.utp.pitagoras.model.RegistroAsistencia;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

/**
 * Capa de Acceso a Datos (DAO) para el módulo Docente según la Figura 16 (pág. 40)
 * y el Modelo Físico SQL Server de la Figura 19 (pág. 43).
 *
 * Opera en modo híbrido: intenta ejecutar la transacción JDBC en SQL Server (puerto 1433)
 * y, si el motor local aún no está encendido en la estación de pruebas, mantiene la
 * persistencia en memoria sin interrumpir la demostración.
 */
public class DocenteDAO implements IDocenteDAO {

    @Override
    public boolean guardarLoteAsistenciaSQL(List<RegistroAsistencia> registros, List<BitacoraEvento> eventosAuditoria) {
        String sqlAsistencia = "MERGE INTO Asistencia AS target "
                + "USING (SELECT ? AS AlumnoId, ? AS Fecha) AS source "
                + "ON (target.AlumnoId = source.AlumnoId AND target.Fecha = source.Fecha) "
                + "WHEN MATCHED THEN UPDATE SET Estado = ?, Observacion = ? "
                + "WHEN NOT MATCHED THEN INSERT (AlumnoId, Fecha, Estado, Observacion) VALUES (?, ?, ?, ?);";

        try (Connection cn = ConexionBD.obtenerConexion();
             PreparedStatement ps = cn.prepareStatement(sqlAsistencia)) {
            cn.setAutoCommit(false);
            for (RegistroAsistencia r : registros) {
                String codigoEstado = switch (r.getEstado()) {
                    case "Asistió" -> "A";
                    case "Tardanza" -> "T";
                    default -> r.isJustificada() ? "FJ" : "F";
                };
                ps.setInt(1, r.getEstudianteId());
                ps.setString(2, r.getFecha());
                ps.setString(3, codigoEstado);
                ps.setString(4, r.getObservacion());
                ps.setInt(5, r.getEstudianteId());
                ps.setString(6, r.getFecha());
                ps.setString(7, codigoEstado);
                ps.setString(8, r.getObservacion());
                ps.addBatch();
            }
            ps.executeBatch();
            registrarEventosAuditoriaSQL(cn, eventosAuditoria);
            cn.commit();
            return true;
        } catch (SQLException ex) {
            // Si SQL Server no está activo en la PC local, el controlador conserva los datos en memoria
            return false;
        }
    }

    public boolean guardarCalificativosSQL(List<CalificacionCompetencia> calificaciones, List<BitacoraEvento> eventosAuditoria) {
        String sqlNota = "UPDATE Nota SET Calificativo = ?, Fecha = GETDATE() "
                + "WHERE DetalleCursoSeccionId = ? AND EvaluacionId = ?;";

        try (Connection cn = ConexionBD.obtenerConexion();
             PreparedStatement ps = cn.prepareStatement(sqlNota)) {
            cn.setAutoCommit(false);
            for (CalificacionCompetencia c : calificaciones) {
                String[] comps = c.getCalificativos();
                for (int i = 0; i < comps.length; i++) {
                    ps.setString(1, comps[i]);
                    ps.setInt(2, c.getEstudianteId());
                    ps.setInt(3, i + 1);
                    ps.addBatch();
                }
            }
            ps.executeBatch();
            registrarEventosAuditoriaSQL(cn, eventosAuditoria);
            cn.commit();
            return true;
        } catch (SQLException ex) {
            return false;
        }
    }

    private void registrarEventosAuditoriaSQL(Connection cn, List<BitacoraEvento> eventos) throws SQLException {
        if (eventos == null || eventos.isEmpty()) {
            return;
        }
        String sqlAudit = "INSERT INTO Auditoria (Tabla, ClavePrimaria, UsuarioId, FechaCambio, ValorAnterior, ValorNuevo) "
                + "VALUES (?, ?, 2, GETDATE(), ?, ?);";
        try (PreparedStatement ps = cn.prepareStatement(sqlAudit)) {
            for (BitacoraEvento ev : eventos) {
                ps.setString(1, ev.getModulo());
                ps.setString(2, ev.getAccion());
                ps.setString(3, ev.getValorAnterior());
                ps.setString(4, ev.getValorNuevo());
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }
}
