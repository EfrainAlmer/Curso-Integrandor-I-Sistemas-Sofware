package pe.edu.pitagoras.sga.dao.impl;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import pe.edu.pitagoras.sga.dao.interfaces.IAsistenciaDAO;
import pe.edu.pitagoras.sga.dao.support.JdbcSupport;
import pe.edu.pitagoras.sga.model.evaluacion.AsistenciaRegistro;
import pe.edu.pitagoras.sga.model.evaluacion.EstadoAsistencia;
import pe.edu.pitagoras.sga.model.evaluacion.FiltroAsistencia;
import pe.edu.pitagoras.sga.model.evaluacion.MarcaAsistencia;

/** JDBC de Asistencia. */
public class AsistenciaDAOImpl extends JdbcSupport implements IAsistenciaDAO {

    private static final String SQL_LISTAR = "SELECT s.AlumnoId, s.CursoSeccionId, c.Nombre AS Curso, s.Fecha, "
            + "s.Estado, s.Justificada, s.Observacion "
            + "FROM Asistencia s "
            + "JOIN CursoSeccion cs ON cs.CursoSeccionId = s.CursoSeccionId "
            + "JOIN Curso c ON c.CursoId = cs.CursoId "
            + "WHERE 1 = 1 ";

    @Override
    public List<AsistenciaRegistro> listar(FiltroAsistencia f) {
        StringBuilder sql = new StringBuilder(SQL_LISTAR);
        List<Object> valores = new ArrayList<>();
        agregar(sql, valores, "AND cs.SeccionId = ? ", f.seccionId());
        agregar(sql, valores, "AND s.CursoSeccionId = ? ", f.cursoSeccionId());
        agregar(sql, valores, "AND s.AlumnoId = ? ", f.alumnoId());
        agregar(sql, valores, "AND s.Fecha >= ? ", f.desde());
        agregar(sql, valores, "AND s.Fecha <= ? ", f.hasta());
        sql.append("ORDER BY s.Fecha, c.CursoId, s.AlumnoId");
        return consultar(sql.toString(), ps -> {
            for (int i = 0; i < valores.size(); i++) {
                Object v = valores.get(i);
                if (v instanceof LocalDate d) {
                    ps.setDate(i + 1, Date.valueOf(d));
                } else {
                    ps.setInt(i + 1, (Integer) v);
                }
            }
        }, rs -> new AsistenciaRegistro(rs.getInt("AlumnoId"), rs.getInt("CursoSeccionId"), texto(rs, "Curso"),
                fecha(rs, "Fecha"), EstadoAsistencia.desde(rs.getString("Estado"), rs.getBoolean("Justificada")),
                texto(rs, "Observacion")), "No se pudo consultar la asistencia");
    }

    @Override
    public int guardar(int cursoSeccionId, LocalDate fecha, List<MarcaAsistencia> marcas, int usuarioId) {
        return enTransaccion(cn -> {
            Map<Integer, AsistenciaRegistro> anteriores = new HashMap<>();
            for (AsistenciaRegistro r : consultar(cn, SQL_LISTAR + "AND s.CursoSeccionId = ? AND s.Fecha = ?",
                    ps -> {
                        ps.setInt(1, cursoSeccionId);
                        ps.setDate(2, Date.valueOf(fecha));
                    }, rs -> new AsistenciaRegistro(rs.getInt("AlumnoId"), cursoSeccionId, null, fecha,
                            EstadoAsistencia.desde(rs.getString("Estado"), rs.getBoolean("Justificada")),
                            texto(rs, "Observacion")))) {
                anteriores.put(r.alumnoId(), r);
            }

            int nuevas = 0;
            int cambios = 0;
            try (PreparedStatement ins = cn.prepareStatement("INSERT INTO Asistencia (AlumnoId, CursoSeccionId, "
                    + "Fecha, Estado, Justificada, Observacion) VALUES (?, ?, ?, ?, ?, ?)");
                 PreparedStatement upd = cn.prepareStatement("UPDATE Asistencia SET Estado = ?, Justificada = ?, "
                         + "Observacion = ? WHERE AlumnoId = ? AND CursoSeccionId = ? AND Fecha = ?")) {
                for (MarcaAsistencia m : marcas) {
                    String obs = recortar(m.observacion() == null || m.observacion().isBlank()
                            ? null : m.observacion().trim(), 150);
                    AsistenciaRegistro antes = anteriores.get(m.alumnoId());
                    if (antes == null) {
                        ins.setInt(1, m.alumnoId());
                        ins.setInt(2, cursoSeccionId);
                        ins.setDate(3, Date.valueOf(fecha));
                        asignarEstado(ins, 4, m, obs);
                        ins.addBatch();
                        nuevas++;
                    } else if (antes.estado() != m.estado() || !Objects.equals(antes.observacion(), obs)) {
                        asignarEstado(upd, 1, m, obs);
                        upd.setInt(4, m.alumnoId());
                        upd.setInt(5, cursoSeccionId);
                        upd.setDate(6, Date.valueOf(fecha));
                        upd.addBatch();
                        cambios++;
                        if (antes.estado() != m.estado()) {
                            auditar(cn, usuarioId, "Asistencia", "AlumnoId=" + m.alumnoId() + ", CursoSeccionId="
                                    + cursoSeccionId + ", Fecha=" + fecha, antes.estado().toString(),
                                    m.estado().toString(), "MODIFICAR");
                        }
                    }
                }
                ins.executeBatch();
                upd.executeBatch();
            }
            if (nuevas > 0) {
                auditar(cn, usuarioId, "Asistencia", "CursoSeccionId=" + cursoSeccionId + ", Fecha=" + fecha,
                        null, nuevas + " marcas registradas", "REGISTRAR");
            }
            return nuevas + cambios;
        }, "No se pudo guardar la asistencia");
    }

    /** Estado, Justificada y Observacion a partir de la posición indicada. */
    private static void asignarEstado(PreparedStatement ps, int desde, MarcaAsistencia m, String obs)
            throws SQLException {
        ps.setString(desde, m.estado().getCodigo());
        ps.setBoolean(desde + 1, m.estado().isJustificada());
        setTextoONulo(ps, desde + 2, obs);
    }

    private static void agregar(StringBuilder sql, List<Object> valores, String condicion, Object valor) {
        if (valor != null) {
            sql.append(condicion);
            valores.add(valor);
        }
    }
}
