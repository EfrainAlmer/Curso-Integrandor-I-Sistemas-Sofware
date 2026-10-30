package pe.edu.pitagoras.sga.dao.impl;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import pe.edu.pitagoras.sga.model.academico.Curso;
import java.util.Optional;
import pe.edu.pitagoras.sga.dao.interfaces.ICuentaDAO;
import pe.edu.pitagoras.sga.dao.support.JdbcSupport;
import pe.edu.pitagoras.sga.model.academico.Docente;
import pe.edu.pitagoras.sga.model.auth.Rol;
import pe.edu.pitagoras.sga.model.gestion.CuentaUsuario;

/** JDBC de la gestión de cuentas (Usuario y Docente). Nunca devuelve hashes. */
public class CuentaDAOImpl extends JdbcSupport implements ICuentaDAO {

    private static final String SQL_LISTAR = "SELECT u.UsuarioId, u.Username, r.Nombre AS Rol, u.Estado, "
            + "u.DebeCambiarPassword, u.BloqueadoHasta, "
            + "d.Nombre AS DocNombre, d.Apellido AS DocApellido, a.NombreApoderado, "
            + "a.Nombre AS AluNombre, a.Apellido AS AluApellido "
            + "FROM Usuario u JOIN Rol r ON r.RolId = u.RolId "
            + "LEFT JOIN Docente d ON d.UsuarioId = u.UsuarioId "
            + "LEFT JOIN Alumno a ON a.UsuarioId = u.UsuarioId ";

    @Override
    public List<CuentaUsuario> listar() {
        return consultar(SQL_LISTAR + "ORDER BY r.RolId, u.Username", ps -> { }, rs -> new CuentaUsuario(
                rs.getInt("UsuarioId"), texto(rs, "Username"), Rol.desdeNombre(rs.getString("Rol")),
                nombre(rs), rs.getBoolean("Estado"), rs.getBoolean("DebeCambiarPassword"),
                fechaHora(rs, "BloqueadoHasta")), "No se pudo listar los usuarios");
    }

    @Override
    public List<Docente> listarDocentes() {
        return consultar("SELECT d.DocenteId, d.UsuarioId, d.DNI, d.Nombre, d.Apellido, d.Email "
                + "FROM Docente d JOIN Usuario u ON u.UsuarioId = d.UsuarioId WHERE u.Estado = 1 "
                + "ORDER BY d.Apellido, d.Nombre", ps -> { }, rs -> new Docente(rs.getInt("DocenteId"),
                rs.getInt("UsuarioId"), texto(rs, "DNI"), texto(rs, "Nombre"), texto(rs, "Apellido"),
                texto(rs, "Email")), "No se pudo listar los docentes");
    }

    @Override
    public boolean existeUsername(String username) {
        return consultarUno("SELECT UsuarioId FROM Usuario WHERE Username = ?", ps -> ps.setString(1, username),
                rs -> rs.getInt(1), "No se pudo validar el usuario").isPresent();
    }

    @Override
    public int crearDocente(String username, String hashClave, Docente datos, List<Integer> cursoIds, int usuarioId) {
        return enTransaccion(cn -> {
            int cuentaId = crearCuenta(cn, username, hashClave, Rol.DOCENTE);
            int docenteId = insertar(cn, "INSERT INTO Docente (UsuarioId, DNI, Nombre, Apellido, Email) VALUES (?, ?, ?, ?, ?)",
                    ps -> {
                        ps.setInt(1, cuentaId);
                        ps.setString(2, datos.dni());
                        ps.setString(3, datos.nombre());
                        ps.setString(4, datos.apellido());
                        setTextoONulo(ps, 5, datos.email());
                    });
            guardarAreas(cn, docenteId, cursoIds);
            auditar(cn, usuarioId, "Usuario", "UsuarioId=" + cuentaId, null,
                    username + " (DOCENTE: " + datos.nombreCompleto() + ", áreas " + cursoIds + ")", "CREAR_USUARIO");
            return cuentaId;
        }, "No se pudo crear la cuenta del docente");
    }

    @Override
    public Optional<Docente> buscarDocentePorUsuario(int usuarioId) {
        return consultarUno("SELECT DocenteId, UsuarioId, DNI, Nombre, Apellido, Email FROM Docente WHERE UsuarioId = ?",
                ps -> ps.setInt(1, usuarioId), rs -> new Docente(rs.getInt("DocenteId"), rs.getInt("UsuarioId"),
                        texto(rs, "DNI"), texto(rs, "Nombre"), texto(rs, "Apellido"), texto(rs, "Email")),
                "No se pudo consultar el docente");
    }

    @Override
    public void actualizarDocente(Docente d, List<Integer> cursoIds, int usuarioId) {
        enTransaccion(cn -> {
            List<String> antes = consultar(cn, "SELECT DNI, Nombre, Apellido, Email FROM Docente WHERE DocenteId = ?",
                    ps -> ps.setInt(1, d.docenteId()), rs -> texto(rs, "DNI") + " | " + texto(rs, "Nombre") + " "
                            + texto(rs, "Apellido") + " | " + texto(rs, "Email"));
            actualizar(cn, "UPDATE Docente SET DNI = ?, Nombre = ?, Apellido = ?, Email = ? WHERE DocenteId = ?", ps -> {
                ps.setString(1, d.dni());
                ps.setString(2, d.nombre());
                ps.setString(3, d.apellido());
                setTextoONulo(ps, 4, d.email());
                ps.setInt(5, d.docenteId());
            });
            guardarAreas(cn, d.docenteId(), cursoIds);
            auditar(cn, usuarioId, "Docente", "DocenteId=" + d.docenteId(), antes.isEmpty() ? null : antes.get(0),
                    d.dni() + " | " + d.nombre() + " " + d.apellido() + " | " + d.email() + " | áreas " + cursoIds, "ACTUALIZAR_DOCENTE");
            return null;
        }, "No se pudo actualizar los datos del docente");
    }

    @Override
    public int crearDirector(String username, String hashClave, int usuarioId) {
        return enTransaccion(cn -> {
            int cuentaId = crearCuenta(cn, username, hashClave, Rol.DIRECTOR);
            auditar(cn, usuarioId, "Usuario", "UsuarioId=" + cuentaId, null, username + " (DIRECTOR)",
                    "CREAR_USUARIO");
            return cuentaId;
        }, "No se pudo crear la cuenta");
    }

    @Override
    public void cambiarEstado(int usuarioIdObjetivo, boolean activo, int usuarioId) {
        enTransaccion(cn -> {
            actualizar(cn, "UPDATE Usuario SET Estado = ? WHERE UsuarioId = ?", ps -> {
                ps.setBoolean(1, activo);
                ps.setInt(2, usuarioIdObjetivo);
            });
            auditar(cn, usuarioId, "Usuario", "UsuarioId=" + usuarioIdObjetivo, activo ? "INACTIVO" : "ACTIVO",
                    activo ? "ACTIVO" : "INACTIVO", "ESTADO_USUARIO");
            return null;
        }, "No se pudo cambiar el estado de la cuenta");
    }

    @Override
    public void restablecerClave(int usuarioIdObjetivo, String hashClave, int usuarioId) {
        enTransaccion(cn -> {
            actualizar(cn, "UPDATE Usuario SET PasswordHash = ?, DebeCambiarPassword = 1, IntentosFallidos = 0, "
                    + "BloqueadoHasta = NULL WHERE UsuarioId = ?", ps -> {
                        ps.setString(1, hashClave);
                        ps.setInt(2, usuarioIdObjetivo);
                    });
            // Nunca se guarda la clave en la bitácora, solo que se restableció.
            auditar(cn, usuarioId, "Usuario", "UsuarioId=" + usuarioIdObjetivo, null,
                    "Clave temporal asignada", "RESTABLECER_CLAVE");
            return null;
        }, "No se pudo restablecer la contraseña");
    }

    @Override
    public Optional<String> nombreParaMostrar(int usuarioId) {
        return consultarUno(SQL_LISTAR + "WHERE u.UsuarioId = ?", ps -> ps.setInt(1, usuarioId),
                CuentaDAOImpl::nombre, "No se pudo consultar el usuario").filter(n -> !n.isBlank());
    }

    @Override
    public Map<Integer, List<Curso>> areasPorUsuario() {
        Map<Integer, List<Curso>> mapa = new LinkedHashMap<>();
        consultar("SELECT d.UsuarioId, c.CursoId, c.Nombre AS Curso FROM DocenteArea da "
                + "JOIN Docente d ON d.DocenteId = da.DocenteId JOIN Curso c ON c.CursoId = da.CursoId "
                + "ORDER BY d.UsuarioId, c.CursoId", ps -> { }, rs -> {
                    mapa.computeIfAbsent(rs.getInt("UsuarioId"), k -> new ArrayList<>())
                            .add(new Curso(rs.getInt("CursoId"), texto(rs, "Curso")));
                    return null;
                }, "No se pudo consultar las áreas de los docentes");
        return mapa;
    }

    /** Reemplaza las áreas (especialidad) del docente. */
    private static void guardarAreas(Connection cn, int docenteId, List<Integer> cursoIds) throws SQLException {
        actualizar(cn, "DELETE FROM DocenteArea WHERE DocenteId = ?", ps -> ps.setInt(1, docenteId));
        for (Integer cursoId : cursoIds) {
            actualizar(cn, "INSERT INTO DocenteArea (DocenteId, CursoId) VALUES (?, ?)", ps -> {
                ps.setInt(1, docenteId);
                ps.setInt(2, cursoId);
            });
        }
    }

    private static int crearCuenta(Connection cn, String username, String hash, Rol rol) throws SQLException {
        return insertar(cn, "INSERT INTO Usuario (Username, PasswordHash, RolId, DebeCambiarPassword) "
                + "SELECT ?, ?, RolId, 1 FROM Rol WHERE Nombre = ?", ps -> {
                    ps.setString(1, username);
                    ps.setString(2, hash);
                    ps.setString(3, rol.name());
                });
    }

    /** Docente: su nombre. Apoderado: su nombre y el del alumno. Director: vacío. */
    private static String nombre(ResultSet rs) throws SQLException {
        String doc = texto(rs, "DocNombre");
        if (doc != null) {
            return doc + " " + texto(rs, "DocApellido");
        }
        String apoderado = texto(rs, "NombreApoderado");
        if (apoderado != null) {
            return apoderado + " (apoderado de " + texto(rs, "AluNombre") + " " + texto(rs, "AluApellido") + ")";
        }
        return "";
    }
}
