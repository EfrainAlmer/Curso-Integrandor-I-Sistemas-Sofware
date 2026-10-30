package pe.edu.pitagoras.sga.config;

import com.google.common.base.Preconditions;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.Connection;
import java.sql.SQLException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pe.edu.pitagoras.sga.exception.AccesoDatosException;

/**
 * Punto único de acceso a SQL Server. Mantiene un pool HikariCP pequeño
 * (máximo 3 conexiones por estación, RNF05). Los DAO piden la conexión con
 * try-with-resources y al cerrarla vuelve al pool.
 */
public final class ConexionBD {

    private static final Logger LOG = LoggerFactory.getLogger(ConexionBD.class);
    private static final int MAX_CONEXIONES = 3;

    private static HikariDataSource pool;

    private ConexionBD() {
    }

    /**
     * Crea el pool. Se llama una sola vez desde Main.
     *
     * @param config configuración con db.url, db.user y db.password.
     * @throws AccesoDatosException si no se puede conectar a la base de datos.
     */
    public static synchronized void iniciar(AppConfig config) {
        if (pool != null) {
            return;
        }
        HikariConfig hc = new HikariConfig();
        hc.setJdbcUrl(config.obtener("db.url"));
        hc.setUsername(config.obtener("db.user"));
        hc.setPassword(config.obtener("db.password"));
        hc.setMaximumPoolSize(MAX_CONEXIONES);
        hc.setMinimumIdle(1);
        hc.setConnectionTimeout(3000);
        hc.setPoolName("sga-pool");
        try {
            pool = new HikariDataSource(hc);
        } catch (RuntimeException e) {
            // Nunca se registra la contraseña, solo el motivo técnico.
            throw new AccesoDatosException("No se pudo conectar a la base de datos. "
                    + "Verifique que SQL Server esté encendido y revise config.properties.", e);
        }
        LOG.info("Pool de conexiones iniciado (máximo {} conexiones)", MAX_CONEXIONES);
    }

    /**
     * @return una conexión del pool; quien la pide debe cerrarla (try-with-resources).
     * @throws AccesoDatosException si el pool no responde.
     */
    public static Connection getConexion() {
        Preconditions.checkState(pool != null, "ConexionBD.iniciar() no fue llamado");
        try {
            return pool.getConnection();
        } catch (SQLException e) {
            throw new AccesoDatosException("No se pudo obtener una conexión a la base de datos", e);
        }
    }

    /** Cierra el pool al salir de la aplicación. */
    public static synchronized void cerrar() {
        if (pool != null) {
            pool.close();
            pool = null;
            LOG.info("Pool de conexiones cerrado");
        }
    }
}