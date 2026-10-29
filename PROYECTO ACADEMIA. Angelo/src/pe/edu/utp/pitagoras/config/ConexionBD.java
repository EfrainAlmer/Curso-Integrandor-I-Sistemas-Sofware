package pe.edu.utp.pitagoras.config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Configuración de conexión JDBC hacia SQL Server (Puerto 1433)
 * para el Sistema de Gestión Académica del Colegio Particular Pitágoras.
 */
public class ConexionBD {
    private static final String HOST = "localhost";
    private static final String PORT = "1433";
    private static final String DATABASE = "BD_GestionAcademica_Pitagoras";
    private static final String USER = "sa";
    private static final String PASSWORD = "TuPasswordSQL2026";

    private static final String URL = "jdbc:sqlserver://" + HOST + ":" + PORT + ";"
            + "databaseName=" + DATABASE + ";"
            + "encrypt=true;trustServerCertificate=true;";

    private ConexionBD() {
        // Clase utilitaria
    }

    public static Connection obtenerConexion() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}
