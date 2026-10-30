package pe.edu.pitagoras.sga.dao.impl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;
import pe.edu.pitagoras.sga.config.ConexionBD;
import pe.edu.pitagoras.sga.dao.interfaces.IUsuarioDAO;
import pe.edu.pitagoras.sga.exception.AccesoDatosException;
import pe.edu.pitagoras.sga.model.auth.Rol;
import pe.edu.pitagoras.sga.model.auth.Usuario;

/** JDBC de la tabla Usuario. Solo PreparedStatement y sin reglas de negocio. */
public class UsuarioDAOImpl implements IUsuarioDAO {

    private static final String SQL_BUSCAR =
            "SELECT u.UsuarioId, u.Username, u.PasswordHash, r.Nombre AS Rol, u.Estado, "
            + "u.DebeCambiarPassword, u.IntentosFallidos, u.BloqueadoHasta "
            + "FROM Usuario u JOIN Rol r ON r.RolId = u.RolId WHERE u.Username = ?";

    private static final String SQL_ACTUALIZAR_INTENTOS =
            "UPDATE Usuario SET IntentosFallidos = ?, BloqueadoHasta = ? WHERE UsuarioId = ?";

    private static final String SQL_REINICIAR_INTENTOS =
            "UPDATE Usuario SET IntentosFallidos = 0, BloqueadoHasta = NULL WHERE UsuarioId = ?";

    private static final String SQL_ACTUALIZAR_PASSWORD =
            "UPDATE Usuario SET PasswordHash = ?, DebeCambiarPassword = 0, "
            + "IntentosFallidos = 0, BloqueadoHasta = NULL WHERE UsuarioId = ?";

    private static final String SQL_ALUMNO_DEL_USUARIO =
            "SELECT AlumnoId FROM Alumno WHERE UsuarioId = ?";

    private static final String SQL_REGISTRAR_ACCESO =
            "INSERT INTO IntentoAcceso (Username, Resultado) VALUES (?, ?)";

    @Override
    public Optional<Usuario> buscarPorUsername(String username) {
        try (Connection cn = ConexionBD.getConexion();
             PreparedStatement ps = cn.prepareStatement(SQL_BUSCAR)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                return Optional.of(new Usuario(
                        rs.getInt("UsuarioId"),
                        rs.getString("Username"),
                        rs.getString("PasswordHash"),
                        Rol.desdeNombre(rs.getString("Rol")),
                        rs.getBoolean("Estado"),
                        rs.getBoolean("DebeCambiarPassword"),
                        rs.getInt("IntentosFallidos"),
                        rs.getObject("BloqueadoHasta", LocalDateTime.class)));
            }
        } catch (SQLException e) {
            throw new AccesoDatosException("No se pudo consultar la cuenta de usuario", e);
        }
    }

    @Override
    public void actualizarIntentos(int usuarioId, int intentosFallidos, LocalDateTime bloqueadoHasta) {
        try (Connection cn = ConexionBD.getConexion();
             PreparedStatement ps = cn.prepareStatement(SQL_ACTUALIZAR_INTENTOS)) {
            ps.setInt(1, intentosFallidos);
            if (bloqueadoHasta == null) {
                ps.setNull(2, Types.TIMESTAMP);
            } else {
                ps.setObject(2, bloqueadoHasta);
            }
            ps.setInt(3, usuarioId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new AccesoDatosException("No se pudo registrar el intento fallido", e);
        }
    }

    @Override
    public void reiniciarIntentos(int usuarioId) {
        try (Connection cn = ConexionBD.getConexion();
             PreparedStatement ps = cn.prepareStatement(SQL_REINICIAR_INTENTOS)) {
            ps.setInt(1, usuarioId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new AccesoDatosException("No se pudo reiniciar los intentos de acceso", e);
        }
    }

    @Override
    public void actualizarPassword(int usuarioId, String nuevoHash) {
        try (Connection cn = ConexionBD.getConexion();
             PreparedStatement ps = cn.prepareStatement(SQL_ACTUALIZAR_PASSWORD)) {
            ps.setString(1, nuevoHash);
            ps.setInt(2, usuarioId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new AccesoDatosException("No se pudo guardar la nueva contraseña", e);
        }
    }

    @Override
    public Optional<Integer> buscarAlumnoIdPorUsuario(int usuarioId) {
        try (Connection cn = ConexionBD.getConexion();
             PreparedStatement ps = cn.prepareStatement(SQL_ALUMNO_DEL_USUARIO)) {
            ps.setInt(1, usuarioId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(rs.getInt("AlumnoId")) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new AccesoDatosException("No se pudo consultar el alumno del apoderado", e);
        }
    }

    @Override
    public void registrarAcceso(String username, String resultado) {
        try (Connection cn = ConexionBD.getConexion();
             PreparedStatement ps = cn.prepareStatement(SQL_REGISTRAR_ACCESO)) {
            ps.setString(1, StringUtils.left(username, 50)); // la columna admite 50 caracteres
            ps.setString(2, resultado);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new AccesoDatosException("No se pudo registrar el acceso", e);
        }
    }
}