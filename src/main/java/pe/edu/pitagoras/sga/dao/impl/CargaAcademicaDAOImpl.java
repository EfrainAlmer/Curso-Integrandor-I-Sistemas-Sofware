package pe.edu.pitagoras.sga.dao.impl;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import pe.edu.pitagoras.sga.dao.interfaces.ICargaAcademicaDAO;
import pe.edu.pitagoras.sga.dao.support.JdbcSupport;
import pe.edu.pitagoras.sga.dao.support.Mapeos;
import pe.edu.pitagoras.sga.model.academico.CargaAcademica;

/** JDBC de CursoSeccion (carga académica). */
public class CargaAcademicaDAOImpl extends JdbcSupport implements ICargaAcademicaDAO {

    private static final String SQL_BASE = "SELECT cs.CursoSeccionId, c.CursoId, c.Nombre AS Curso, "
            + Mapeos.COLUMNAS_SECCION + ", d.DocenteId, d.Nombre AS DocNombre, d.Apellido AS DocApellido "
            + "FROM CursoSeccion cs "
            + "JOIN Curso c ON c.CursoId = cs.CursoId "
            + "JOIN Seccion s ON s.SeccionId = cs.SeccionId" + Mapeos.JOIN_SECCION
            + "JOIN Docente d ON d.DocenteId = cs.DocenteId ";

    private static final String ORDEN = "ORDER BY s.GradoId, s.Nombre, c.CursoId";

    @Override
    public List<CargaAcademica> listar() {
        return consultar(SQL_BASE + ORDEN, ps -> { }, CargaAcademicaDAOImpl::mapear,
                "No se pudo listar la carga académica");
    }

    @Override
    public List<CargaAcademica> listarPorDocente(int docenteId) {
        return consultar(SQL_BASE + "WHERE cs.DocenteId = ? " + ORDEN, ps -> ps.setInt(1, docenteId),
                CargaAcademicaDAOImpl::mapear, "No se pudo listar la carga del docente");
    }

    @Override
    public Optional<CargaAcademica> buscar(int cursoSeccionId) {
        return consultarUno(SQL_BASE + "WHERE cs.CursoSeccionId = ?", ps -> ps.setInt(1, cursoSeccionId),
                CargaAcademicaDAOImpl::mapear, "No se pudo consultar la carga académica");
    }

    @Override
    public Optional<Integer> buscarDocenteIdPorUsuario(int usuarioId) {
        return consultarUno("SELECT DocenteId FROM Docente WHERE UsuarioId = ?", ps -> ps.setInt(1, usuarioId),
                rs -> rs.getInt("DocenteId"), "No se pudo consultar el docente");
    }

    @Override
    public void asignar(int cursoId, int seccionId, int docenteId, int usuarioId) {
        enTransaccion(cn -> {
            List<Integer> anterior = consultar(cn,
                    "SELECT DocenteId FROM CursoSeccion WHERE CursoId = ? AND SeccionId = ?", ps -> {
                        ps.setInt(1, cursoId);
                        ps.setInt(2, seccionId);
                    }, rs -> rs.getInt("DocenteId"));
            String clave = "CursoId=" + cursoId + ", SeccionId=" + seccionId;
            if (anterior.isEmpty()) {
                int id = insertar(cn, "INSERT INTO CursoSeccion (CursoId, SeccionId, DocenteId) VALUES (?, ?, ?)",
                        ps -> {
                            ps.setInt(1, cursoId);
                            ps.setInt(2, seccionId);
                            ps.setInt(3, docenteId);
                        });
                // Una evaluación por periodo y competencia, para que el docente pueda calificar.
                actualizar(cn, "INSERT INTO Evaluacion (PeriodoId, CursoSeccionId, CodigoCompetencia) "
                        + "SELECT p.PeriodoId, ?, k.Codigo FROM Periodo p "
                        + "JOIN Competencia k ON k.CursoId = ? "
                        + "WHERE p.AnioLectivo = (SELECT MAX(AnioLectivo) FROM Periodo)", ps -> {
                            ps.setInt(1, id);
                            ps.setInt(2, cursoId);
                        });
                auditar(cn, usuarioId, "CursoSeccion", clave, null, "DocenteId=" + docenteId, "ASIGNAR_CARGA");
            } else if (anterior.get(0) != docenteId) {
                actualizar(cn, "UPDATE CursoSeccion SET DocenteId = ? WHERE CursoId = ? AND SeccionId = ?", ps -> {
                    ps.setInt(1, docenteId);
                    ps.setInt(2, cursoId);
                    ps.setInt(3, seccionId);
                });
                auditar(cn, usuarioId, "CursoSeccion", clave, "DocenteId=" + anterior.get(0),
                        "DocenteId=" + docenteId, "ASIGNAR_CARGA");
            }
            return null;
        }, "No se pudo asignar la carga académica");
    }

    private static CargaAcademica mapear(ResultSet rs) throws SQLException {
        return new CargaAcademica(rs.getInt("CursoSeccionId"), Mapeos.curso(rs), Mapeos.seccion(rs),
                rs.getInt("DocenteId"), texto(rs, "DocApellido") + ", " + texto(rs, "DocNombre"));
    }
}
