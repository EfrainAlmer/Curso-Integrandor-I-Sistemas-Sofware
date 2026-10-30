package pe.edu.pitagoras.sga.dao.impl;

import java.util.List;
import pe.edu.pitagoras.sga.dao.interfaces.IEstructuraDAO;
import pe.edu.pitagoras.sga.dao.support.JdbcSupport;
import pe.edu.pitagoras.sga.dao.support.Mapeos;
import pe.edu.pitagoras.sga.model.academico.Competencia;
import pe.edu.pitagoras.sga.model.academico.Curso;
import pe.edu.pitagoras.sga.model.academico.Grado;
import pe.edu.pitagoras.sga.model.academico.Seccion;

/** JDBC de Grado, Seccion, Turno, Curso y Competencia. */
public class EstructuraDAOImpl extends JdbcSupport implements IEstructuraDAO {

    private static final String SQL_SECCIONES = "SELECT " + Mapeos.COLUMNAS_SECCION
            + " FROM Seccion s" + Mapeos.JOIN_SECCION + "ORDER BY s.GradoId, s.Nombre";

    @Override
    public List<Grado> listarGrados() {
        return consultar("SELECT GradoId, Nombre FROM Grado ORDER BY GradoId", ps -> { },
                rs -> new Grado(rs.getInt("GradoId"), texto(rs, "Nombre")), "No se pudo listar los grados");
    }

    @Override
    public List<Seccion> listarSecciones() {
        return consultar(SQL_SECCIONES, ps -> { }, Mapeos::seccion, "No se pudo listar las secciones");
    }

    @Override
    public List<String> listarTurnos() {
        return consultar("SELECT Nombre FROM Turno ORDER BY TurnoId", ps -> { },
                rs -> texto(rs, "Nombre"), "No se pudo listar los turnos");
    }

    @Override
    public List<Curso> listarCursos() {
        return consultar("SELECT CursoId, Nombre AS Curso FROM Curso ORDER BY CursoId", ps -> { },
                Mapeos::curso, "No se pudo listar las áreas");
    }

    @Override
    public List<Competencia> listarCompetencias(int cursoId) {
        return consultar("SELECT CursoId, Codigo, Nombre FROM Competencia WHERE CursoId = ? ORDER BY Codigo",
                ps -> ps.setInt(1, cursoId),
                rs -> new Competencia(rs.getInt("CursoId"), texto(rs, "Codigo"), texto(rs, "Nombre")),
                "No se pudo listar las competencias");
    }

    @Override
    public int crearSeccion(int gradoId, String letra, String turno, int usuarioId) {
        return enTransaccion(cn -> {
            int id = insertar(cn, "INSERT INTO Seccion (GradoId, TurnoId, Nombre) "
                    + "SELECT ?, TurnoId, ? FROM Turno WHERE Nombre = ?", ps -> {
                        ps.setInt(1, gradoId);
                        ps.setString(2, letra);
                        ps.setString(3, turno);
                    });
            auditar(cn, usuarioId, "Seccion", "SeccionId=" + id, null,
                    "GradoId=" + gradoId + ", Sección " + letra + ", turno " + turno, "CONFIGURAR");
            return id;
        }, "No se pudo crear la sección");
    }

    @Override
    public int crearCurso(String nombre, List<String> competencias, int usuarioId) {
        return enTransaccion(cn -> {
            int id = insertar(cn, "INSERT INTO Curso (Nombre) VALUES (?)", ps -> ps.setString(1, nombre));
            for (int i = 0; i < competencias.size(); i++) {
                String codigo = String.valueOf(i + 1);
                String competencia = competencias.get(i);
                actualizar(cn, "INSERT INTO Competencia (CursoId, Codigo, Nombre) VALUES (?, ?, ?)", ps -> {
                    ps.setInt(1, id);
                    ps.setString(2, codigo);
                    ps.setString(3, competencia);
                });
            }
            auditar(cn, usuarioId, "Curso", "CursoId=" + id, null,
                    nombre + " (" + competencias.size() + " competencias)", "CONFIGURAR");
            return id;
        }, "No se pudo crear el área");
    }
}
