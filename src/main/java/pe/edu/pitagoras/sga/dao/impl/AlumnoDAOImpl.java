package pe.edu.pitagoras.sga.dao.impl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import pe.edu.pitagoras.sga.dao.interfaces.IAlumnoDAO;
import pe.edu.pitagoras.sga.dao.support.JdbcSupport;
import pe.edu.pitagoras.sga.dao.support.Mapeos;
import pe.edu.pitagoras.sga.model.academico.Alumno;

/** JDBC de Alumno y de la cuenta de su apoderado. */
public class AlumnoDAOImpl extends JdbcSupport implements IAlumnoDAO {

    private static final String SQL_BASE = "SELECT a.AlumnoId, a.DNI, a.Nombre, a.Apellido, a.NombreApoderado, "
            + "a.TelefonoApoderado, a.Estado, u.Username AS UsuarioApoderado, " + Mapeos.COLUMNAS_SECCION
            + " FROM Alumno a JOIN Seccion s ON s.SeccionId = a.SeccionId" + Mapeos.JOIN_SECCION
            + "LEFT JOIN Usuario u ON u.UsuarioId = a.UsuarioId ";

    @Override
    public List<Alumno> listar(Integer seccionId, boolean soloActivos) {
        StringBuilder sql = new StringBuilder(SQL_BASE).append("WHERE 1 = 1 ");
        if (seccionId != null) {
            sql.append("AND a.SeccionId = ? ");
        }
        if (soloActivos) {
            sql.append("AND a.Estado = 'ACTIVO' ");
        }
        sql.append("ORDER BY s.GradoId, s.Nombre, a.Apellido, a.Nombre");
        return consultar(sql.toString(), ps -> {
            if (seccionId != null) {
                ps.setInt(1, seccionId);
            }
        }, AlumnoDAOImpl::mapear, "No se pudo listar los alumnos");
    }

    @Override
    public Optional<Alumno> buscar(int alumnoId) {
        return consultarUno(SQL_BASE + "WHERE a.AlumnoId = ?", ps -> ps.setInt(1, alumnoId),
                AlumnoDAOImpl::mapear, "No se pudo consultar el alumno");
    }

    @Override
    public boolean existeDni(String dni, int excluirAlumnoId) {
        return consultarUno("SELECT AlumnoId FROM Alumno WHERE DNI = ? AND AlumnoId <> ?", ps -> {
            ps.setString(1, dni);
            ps.setInt(2, excluirAlumnoId);
        }, rs -> rs.getInt(1), "No se pudo validar el DNI").isPresent();
    }

    @Override
    public int inscribir(Alumno a, String usernameApoderado, String hashClave, int usuarioId) {
        return enTransaccion(cn -> {
            int cuentaId = insertar(cn, "INSERT INTO Usuario (Username, PasswordHash, RolId, DebeCambiarPassword) "
                    + "SELECT ?, ?, RolId, 1 FROM Rol WHERE Nombre = 'APODERADO'", ps -> {
                        ps.setString(1, usernameApoderado);
                        ps.setString(2, hashClave);
                    });
            int alumnoId = insertar(cn, "INSERT INTO Alumno (SeccionId, UsuarioId, DNI, Nombre, Apellido, "
                    + "NombreApoderado, TelefonoApoderado) VALUES (?, ?, ?, ?, ?, ?, ?)", ps -> {
                        ps.setInt(1, a.seccion().seccionId());
                        ps.setInt(2, cuentaId);
                        asignarDatos(ps, 3, a);
                    });
            auditar(cn, usuarioId, "Alumno", "AlumnoId=" + alumnoId, null,
                    "DNI " + a.dni() + " en " + a.seccion().etiqueta() + "; cuenta " + usernameApoderado, "INSCRIBIR");
            return alumnoId;
        }, "No se pudo inscribir al alumno");
    }

    @Override
    public void actualizar(Alumno a, int usuarioId) {
        enTransaccion(cn -> {
            Alumno antes = buscarEn(cn, a.alumnoId());
            actualizar(cn, "UPDATE Alumno SET SeccionId = ?, DNI = ?, Nombre = ?, Apellido = ?, "
                    + "NombreApoderado = ?, TelefonoApoderado = ? WHERE AlumnoId = ?", ps -> {
                        ps.setInt(1, a.seccion().seccionId());
                        asignarDatos(ps, 2, a);
                        ps.setInt(7, a.alumnoId());
                    });
            auditar(cn, usuarioId, "Alumno", "AlumnoId=" + a.alumnoId(), resumen(antes), resumen(a),
                    "ACTUALIZAR_ALUMNO");
            return null;
        }, "No se pudo actualizar los datos del alumno");
    }

    @Override
    public void cambiarEstado(int alumnoId, String estado, int usuarioId) {
        enTransaccion(cn -> {
            Alumno antes = buscarEn(cn, alumnoId);
            actualizar(cn, "UPDATE Alumno SET Estado = ? WHERE AlumnoId = ?", ps -> {
                ps.setString(1, estado);
                ps.setInt(2, alumnoId);
            });
            // Un alumno retirado no debe dejar activa la cuenta de su apoderado.
            actualizar(cn, "UPDATE Usuario SET Estado = ? WHERE UsuarioId = "
                    + "(SELECT UsuarioId FROM Alumno WHERE AlumnoId = ?)", ps -> {
                        ps.setBoolean(1, Alumno.ACTIVO.equals(estado));
                        ps.setInt(2, alumnoId);
                    });
            auditar(cn, usuarioId, "Alumno", "AlumnoId=" + alumnoId, antes.estado(), estado, "ACTUALIZAR_ALUMNO");
            return null;
        }, "No se pudo cambiar el estado del alumno");
    }

    private static Alumno buscarEn(Connection cn, int alumnoId) throws SQLException {
        List<Alumno> l = consultar(cn, SQL_BASE + "WHERE a.AlumnoId = ?", ps -> ps.setInt(1, alumnoId),
                AlumnoDAOImpl::mapear);
        if (l.isEmpty()) {
            throw new SQLException("No existe el alumno " + alumnoId);
        }
        return l.get(0);
    }

    /** DNI, nombre, apellido, apoderado y teléfono a partir de la posición indicada. */
    private static void asignarDatos(PreparedStatement ps, int desde, Alumno a) throws SQLException {
        ps.setString(desde, a.dni());
        ps.setString(desde + 1, a.nombre());
        ps.setString(desde + 2, a.apellido());
        ps.setString(desde + 3, a.nombreApoderado());
        setTextoONulo(ps, desde + 4, a.telefonoApoderado());
    }

    private static String resumen(Alumno a) {
        return a.dni() + " | " + a.nombreCompleto() + " | " + a.seccion().etiqueta() + " | "
                + a.nombreApoderado() + " | " + (a.telefonoApoderado() == null ? "-" : a.telefonoApoderado());
    }

    private static Alumno mapear(ResultSet rs) throws SQLException {
        return new Alumno(rs.getInt("AlumnoId"), Mapeos.seccion(rs), texto(rs, "DNI"), texto(rs, "Nombre"),
                texto(rs, "Apellido"), texto(rs, "NombreApoderado"), texto(rs, "TelefonoApoderado"),
                texto(rs, "Estado"), texto(rs, "UsuarioApoderado"));
    }
}
