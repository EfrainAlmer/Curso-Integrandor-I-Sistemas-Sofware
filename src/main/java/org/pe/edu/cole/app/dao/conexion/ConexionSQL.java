package org.pe.edu.cole.app.dao.conexion;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Esta clase única permite obtener la conexión a SQL Server.
 * Ajusta URL, usuario y contraseña al entorno.
 * Requiere el driver mssql-jdbc en el pom.xml.
 */
public class ConexionSQL {

    private static final String URL =
            "jdbc:sqlserver://JHOSER\\SQLEXPRESS;databaseName=BD_GestionAcademica;encrypt=true;trustServerCertificate=true";
    private static final String USUARIO = "app_login";
    private static final String CONTRASENA = "UnaClaveSegura123!";

    private static Connection conexion;

    /**
     * Constructor privado para evitar la instanciación de la clase.
     * Esta clase solo debe usarse mediante sus métodos estáticos.
     */
    private ConexionSQL() {
    }

    /**
     * Este constructor obtiene la conexión activa a la base de datos.
     * Si la conexión aún no ha sido creada o se cerró previamente, establece una nueva.
     *
     * @return la conexión a la base de datos SQL Server.
     * @throws SQLException si las credenciales son incorrectas o el servidor no responde.
     */
    public static Connection getConexion() throws SQLException {
        if (conexion == null || conexion.isClosed()) {
            conexion = DriverManager.getConnection(URL, USUARIO, CONTRASENA);
        }
        return conexion;
    }

    /**
     * Este método cierra la conexión activa a la base de datos de forma segura.
     * Libera los recursos de red y memoria asociados a JDBC.
     */
    public static void cerrarConexion() {
        try {
            if (conexion != null && !conexion.isClosed()) {
                conexion.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}//cierra class ConexionSQL
