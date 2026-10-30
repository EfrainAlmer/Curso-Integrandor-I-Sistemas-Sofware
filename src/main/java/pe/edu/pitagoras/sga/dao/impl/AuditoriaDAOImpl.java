package pe.edu.pitagoras.sga.dao.impl;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.List;
import pe.edu.pitagoras.sga.dao.interfaces.IAuditoriaDAO;
import pe.edu.pitagoras.sga.dao.support.JdbcSupport;
import pe.edu.pitagoras.sga.model.gestion.EventoAuditoria;

/** JDBC de la bitácora (solo lectura: los registros se escriben dentro de cada transacción). */
public class AuditoriaDAOImpl extends JdbcSupport implements IAuditoriaDAO {

    @Override
    public List<EventoAuditoria> listar(LocalDate desde, LocalDate hasta, String accion, int limite) {
        String sql = "SELECT a.FechaCambio, u.Username, a.Tabla, a.ClavePrimaria, a.ValorAnterior, a.ValorNuevo, "
                + "a.Accion FROM Auditoria a JOIN Usuario u ON u.UsuarioId = a.UsuarioId "
                + "WHERE a.FechaCambio >= ? AND a.FechaCambio < ? "
                + (accion == null ? "" : "AND a.Accion = ? ")
                + "ORDER BY a.FechaCambio DESC, a.AuditoriaId DESC "
                + "OFFSET 0 ROWS FETCH NEXT ? ROWS ONLY";
        return consultar(sql, ps -> {
            int i = 1;
            ps.setTimestamp(i++, Timestamp.valueOf(desde.atStartOfDay()));
            ps.setTimestamp(i++, Timestamp.valueOf(hasta.plusDays(1).atStartOfDay()));
            if (accion != null) {
                ps.setString(i++, accion);
            }
            ps.setInt(i, limite);
        }, rs -> new EventoAuditoria(fechaHora(rs, "FechaCambio"), texto(rs, "Username"), texto(rs, "Tabla"),
                texto(rs, "ClavePrimaria"), texto(rs, "ValorAnterior"), texto(rs, "ValorNuevo"),
                texto(rs, "Accion")), "No se pudo consultar la bitácora");
    }
}
