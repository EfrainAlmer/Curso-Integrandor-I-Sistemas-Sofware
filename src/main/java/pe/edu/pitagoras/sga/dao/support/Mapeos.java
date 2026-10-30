package pe.edu.pitagoras.sga.dao.support;

import java.sql.ResultSet;
import java.sql.SQLException;
import pe.edu.pitagoras.sga.model.academico.Curso;
import pe.edu.pitagoras.sga.model.academico.Seccion;

/**
 * Lecturas que se repiten en varios DAO. Las consultas deben traer las columnas
 * con estos alias.
 */
public final class Mapeos {

    /** Columnas esperadas: SeccionId, GradoId, Grado, Letra, Turno. */
    public static final String COLUMNAS_SECCION =
            "s.SeccionId, s.GradoId, g.Nombre AS Grado, s.Nombre AS Letra, t.Nombre AS Turno";

    /** JOIN que acompaña a {@link #COLUMNAS_SECCION}; la tabla Seccion debe tener el alias s. */
    public static final String JOIN_SECCION =
            " JOIN Grado g ON g.GradoId = s.GradoId JOIN Turno t ON t.TurnoId = s.TurnoId ";

    private Mapeos() {
    }

    public static Seccion seccion(ResultSet rs) throws SQLException {
        return new Seccion(rs.getInt("SeccionId"), rs.getInt("GradoId"), rs.getString("Grado").trim(),
                rs.getString("Letra").trim(), rs.getString("Turno").trim());
    }

    public static Curso curso(ResultSet rs) throws SQLException {
        return new Curso(rs.getInt("CursoId"), rs.getString("Curso").trim());
    }
}
