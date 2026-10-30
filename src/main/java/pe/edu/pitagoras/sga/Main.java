package pe.edu.pitagoras.sga;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pe.edu.pitagoras.sga.config.AppConfig;
import pe.edu.pitagoras.sga.config.ConexionBD;
import pe.edu.pitagoras.sga.exception.AccesoDatosException;

/** Punto de entrada. Por ahora solo prueba la conexión (Paso 3). */
public class Main {

    private static final Logger LOG = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        try {
            ConexionBD.iniciar(AppConfig.cargar());
            String sql = "SELECT u.Username, r.Nombre FROM Usuario u "
                    + "JOIN Rol r ON r.RolId = u.RolId ORDER BY u.UsuarioId";
            try (Connection cn = ConexionBD.getConexion();
                 PreparedStatement ps = cn.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    LOG.info("Usuario '{}' con rol {}", rs.getString(1), rs.getString(2));
                }
            }
        } catch (SQLException | AccesoDatosException | IllegalStateException e) {
            LOG.error("Falló la prueba de conexión: {}", e.getMessage(), e);
        } finally {
            ConexionBD.cerrar();
        }
    }
}