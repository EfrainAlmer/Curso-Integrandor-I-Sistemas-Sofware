package pe.edu.pitagoras.sga.dao.impl;

import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import pe.edu.pitagoras.sga.dao.interfaces.INotaDAO;
import pe.edu.pitagoras.sga.dao.support.JdbcSupport;
import pe.edu.pitagoras.sga.model.evaluacion.CambioNota;
import pe.edu.pitagoras.sga.model.evaluacion.Calificativo;
import pe.edu.pitagoras.sga.model.evaluacion.FiltroNotas;
import pe.edu.pitagoras.sga.model.evaluacion.NotaRegistro;

/** JDBC de Nota y Evaluacion. */
public class NotaDAOImpl extends JdbcSupport implements INotaDAO {

    private static final String SQL_LISTAR = "SELECT n.AlumnoId, e.CursoSeccionId, c.CursoId, c.Nombre AS Curso, "
            + "e.CodigoCompetencia, p.PeriodoId, p.BimestreNum, n.Calificativo "
            + "FROM Nota n "
            + "JOIN Evaluacion e    ON e.EvaluacionId = n.EvaluacionId "
            + "JOIN CursoSeccion cs ON cs.CursoSeccionId = e.CursoSeccionId "
            + "JOIN Curso c         ON c.CursoId = cs.CursoId "
            + "JOIN Periodo p       ON p.PeriodoId = e.PeriodoId "
            + "WHERE 1 = 1 ";

    @Override
    public List<NotaRegistro> listar(FiltroNotas f) {
        StringBuilder sql = new StringBuilder(SQL_LISTAR);
        List<Integer> valores = new ArrayList<>();
        agregar(sql, valores, "AND p.PeriodoId = ? ", f.periodoId());
        agregar(sql, valores, "AND cs.SeccionId = ? ", f.seccionId());
        agregar(sql, valores, "AND e.CursoSeccionId = ? ", f.cursoSeccionId());
        agregar(sql, valores, "AND n.AlumnoId = ? ", f.alumnoId());
        sql.append("ORDER BY p.BimestreNum, c.CursoId, n.AlumnoId, e.CodigoCompetencia");
        return consultar(sql.toString(), ps -> {
            for (int i = 0; i < valores.size(); i++) {
                ps.setInt(i + 1, valores.get(i));
            }
        }, rs -> new NotaRegistro(rs.getInt("AlumnoId"), rs.getInt("CursoSeccionId"), rs.getInt("CursoId"),
                texto(rs, "Curso"), texto(rs, "CodigoCompetencia"), rs.getInt("PeriodoId"),
                rs.getInt("BimestreNum"), Calificativo.desdeTexto(rs.getString("Calificativo")).orElseThrow()),
                "No se pudo consultar los calificativos");
    }

    @Override
    public int guardar(int cursoSeccionId, int periodoId, List<CambioNota> cambios, int usuarioId) {
        return enTransaccion(cn -> {
            Map<String, Integer> evaluaciones = new HashMap<>();
            for (Object[] fila : consultar(cn, "SELECT EvaluacionId, CodigoCompetencia FROM Evaluacion "
                    + "WHERE CursoSeccionId = ? AND PeriodoId = ?", ps -> {
                        ps.setInt(1, cursoSeccionId);
                        ps.setInt(2, periodoId);
                    }, rs -> new Object[]{texto(rs, "CodigoCompetencia"), rs.getInt("EvaluacionId")})) {
                evaluaciones.put((String) fila[0], (Integer) fila[1]);
            }
            Map<String, String> anteriores = new HashMap<>();
            for (String[] fila : consultar(cn, "SELECT n.AlumnoId, n.EvaluacionId, n.Calificativo FROM Nota n "
                    + "JOIN Evaluacion e ON e.EvaluacionId = n.EvaluacionId "
                    + "WHERE e.CursoSeccionId = ? AND e.PeriodoId = ?", ps -> {
                        ps.setInt(1, cursoSeccionId);
                        ps.setInt(2, periodoId);
                    }, rs -> new String[]{rs.getInt("AlumnoId") + "|" + rs.getInt("EvaluacionId"),
                        texto(rs, "Calificativo")})) {
                anteriores.put(fila[0], fila[1]);
            }

            Timestamp ahora = Timestamp.valueOf(LocalDateTime.now());
            int nuevas = 0;
            int modificadas = 0;
            try (PreparedStatement ins = cn.prepareStatement(
                         "INSERT INTO Nota (AlumnoId, EvaluacionId, Calificativo, Fecha) VALUES (?, ?, ?, ?)");
                 PreparedStatement upd = cn.prepareStatement(
                         "UPDATE Nota SET Calificativo = ?, Fecha = ? WHERE AlumnoId = ? AND EvaluacionId = ?")) {
                for (CambioNota c : cambios) {
                    Integer evaluacionId = evaluaciones.get(c.codigoCompetencia());
                    if (evaluacionId == null) {
                        throw new IllegalStateException("La competencia " + c.codigoCompetencia()
                                + " no tiene evaluación en este periodo");
                    }
                    String clave = c.alumnoId() + "|" + evaluacionId;
                    String anterior = anteriores.get(clave);
                    String nuevo = c.calificativo().name();
                    if (nuevo.equals(anterior)) {
                        continue;
                    }
                    if (anterior == null) {
                        ins.setInt(1, c.alumnoId());
                        ins.setInt(2, evaluacionId);
                        ins.setString(3, nuevo);
                        ins.setTimestamp(4, ahora);
                        ins.addBatch();
                        nuevas++;
                    } else {
                        upd.setString(1, nuevo);
                        upd.setTimestamp(2, ahora);
                        upd.setInt(3, c.alumnoId());
                        upd.setInt(4, evaluacionId);
                        upd.addBatch();
                        modificadas++;
                        // RF09: usuario, fecha, valor anterior y valor nuevo
                        auditar(cn, usuarioId, "Nota", "AlumnoId=" + c.alumnoId() + ", EvaluacionId=" + evaluacionId,
                                anterior, nuevo, "MODIFICAR");
                    }
                }
                ins.executeBatch();
                upd.executeBatch();
            }
            if (nuevas > 0) {
                auditar(cn, usuarioId, "Nota", "CursoSeccionId=" + cursoSeccionId + ", PeriodoId=" + periodoId,
                        null, nuevas + " calificativos registrados", "REGISTRAR");
            }
            return nuevas + modificadas;
        }, "No se pudo guardar los calificativos");
    }

    private static void agregar(StringBuilder sql, List<Integer> valores, String condicion, Integer valor) {
        if (valor != null) {
            sql.append(condicion);
            valores.add(valor);
        }
    }
}
