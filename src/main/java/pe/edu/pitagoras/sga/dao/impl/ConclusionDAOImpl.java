package pe.edu.pitagoras.sga.dao.impl;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import pe.edu.pitagoras.sga.dao.interfaces.IConclusionDAO;
import pe.edu.pitagoras.sga.dao.support.JdbcSupport;
import pe.edu.pitagoras.sga.model.evaluacion.Conclusion;
import pe.edu.pitagoras.sga.model.evaluacion.FiltroNotas;

/** JDBC de ConclusionDescriptiva. */
public class ConclusionDAOImpl extends JdbcSupport implements IConclusionDAO {

    private static final String SQL_LISTAR = "SELECT cd.AlumnoId, e.CursoSeccionId, cs.CursoId, e.CodigoCompetencia, "
            + "p.PeriodoId, p.BimestreNum, cd.Texto "
            + "FROM ConclusionDescriptiva cd "
            + "JOIN Evaluacion e    ON e.EvaluacionId = cd.EvaluacionId "
            + "JOIN CursoSeccion cs ON cs.CursoSeccionId = e.CursoSeccionId "
            + "JOIN Periodo p       ON p.PeriodoId = e.PeriodoId "
            + "WHERE 1 = 1 ";

    @Override
    public List<Conclusion> listar(FiltroNotas f) {
        StringBuilder sql = new StringBuilder(SQL_LISTAR);
        List<Integer> valores = new ArrayList<>();
        if (f.periodoId() != null) {
            sql.append("AND p.PeriodoId = ? ");
            valores.add(f.periodoId());
        }
        if (f.seccionId() != null) {
            sql.append("AND cs.SeccionId = ? ");
            valores.add(f.seccionId());
        }
        if (f.cursoSeccionId() != null) {
            sql.append("AND e.CursoSeccionId = ? ");
            valores.add(f.cursoSeccionId());
        }
        if (f.alumnoId() != null) {
            sql.append("AND cd.AlumnoId = ? ");
            valores.add(f.alumnoId());
        }
        sql.append("ORDER BY p.BimestreNum, cs.CursoId, e.CodigoCompetencia");
        return consultar(sql.toString(), ps -> {
            for (int i = 0; i < valores.size(); i++) {
                ps.setInt(i + 1, valores.get(i));
            }
        }, rs -> new Conclusion(rs.getInt("AlumnoId"), rs.getInt("CursoSeccionId"), rs.getInt("CursoId"),
                texto(rs, "CodigoCompetencia"), rs.getInt("PeriodoId"), rs.getInt("BimestreNum"),
                texto(rs, "Texto")), "No se pudo consultar las conclusiones descriptivas");
    }

    @Override
    public int guardar(int cursoSeccionId, int periodoId, int alumnoId, Map<String, String> textos, int usuarioId) {
        return enTransaccion(cn -> {
            Map<String, Integer> evaluaciones = new HashMap<>();
            for (Object[] f : consultar(cn, "SELECT EvaluacionId, CodigoCompetencia FROM Evaluacion "
                    + "WHERE CursoSeccionId = ? AND PeriodoId = ?", ps -> {
                        ps.setInt(1, cursoSeccionId);
                        ps.setInt(2, periodoId);
                    }, rs -> new Object[]{texto(rs, "CodigoCompetencia"), rs.getInt("EvaluacionId")})) {
                evaluaciones.put((String) f[0], (Integer) f[1]);
            }
            int cambios = 0;
            for (Map.Entry<String, String> t : textos.entrySet()) {
                Integer evalId = evaluaciones.get(t.getKey());
                if (evalId == null) {
                    continue;
                }
                List<String> antes = consultar(cn, "SELECT Texto FROM ConclusionDescriptiva WHERE AlumnoId = ? "
                        + "AND EvaluacionId = ?", ps -> {
                            ps.setInt(1, alumnoId);
                            ps.setInt(2, evalId);
                        }, rs -> texto(rs, "Texto"));
                String anterior = antes.isEmpty() ? null : antes.get(0);
                String nuevo = t.getValue() == null || t.getValue().isBlank() ? null : recortar(t.getValue().trim(), 500);
                if (Objects.equals(anterior, nuevo)) {
                    continue;
                }
                if (nuevo == null) {
                    actualizar(cn, "DELETE FROM ConclusionDescriptiva WHERE AlumnoId = ? AND EvaluacionId = ?", ps -> {
                        ps.setInt(1, alumnoId);
                        ps.setInt(2, evalId);
                    });
                } else if (anterior == null) {
                    actualizar(cn, "INSERT INTO ConclusionDescriptiva (AlumnoId, EvaluacionId, Texto, Fecha) "
                            + "VALUES (?, ?, ?, ?)", ps -> {
                                ps.setInt(1, alumnoId);
                                ps.setInt(2, evalId);
                                ps.setString(3, nuevo);
                                ps.setTimestamp(4, Timestamp.valueOf(LocalDateTime.now()));
                            });
                } else {
                    actualizar(cn, "UPDATE ConclusionDescriptiva SET Texto = ?, Fecha = ? WHERE AlumnoId = ? "
                            + "AND EvaluacionId = ?", ps -> {
                                ps.setString(1, nuevo);
                                ps.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()));
                                ps.setInt(3, alumnoId);
                                ps.setInt(4, evalId);
                            });
                }
                auditar(cn, usuarioId, "ConclusionDescriptiva", "AlumnoId=" + alumnoId + ", EvaluacionId=" + evalId,
                        anterior, nuevo == null ? "(eliminada)" : nuevo, "CONCLUSION");
                cambios++;
            }
            return cambios;
        }, "No se pudo guardar las conclusiones descriptivas");
    }
}
