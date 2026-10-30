package pe.edu.pitagoras.sga.dao.support;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import pe.edu.pitagoras.sga.config.ConexionBD;
import pe.edu.pitagoras.sga.exception.AccesoDatosException;

/**
 * Base de los DAO: abre la conexión del pool, ejecuta siempre con PreparedStatement
 * (evita inyección SQL) y convierte SQLException en AccesoDatosException. Las operaciones
 * de varias sentencias usan {@link #enTransaccion}: o se guarda todo o nada.
 */
public abstract class JdbcSupport {

    /** Asigna los parámetros de una sentencia. */
    @FunctionalInterface
    protected interface Parametros {
        void asignar(PreparedStatement ps) throws SQLException;
    }

    /** Convierte la fila actual del ResultSet en un objeto. */
    @FunctionalInterface
    protected interface Mapeo<T> {
        T mapear(ResultSet rs) throws SQLException;
    }

    /** Trabajo que se ejecuta dentro de una transacción. */
    @FunctionalInterface
    protected interface Trabajo<T> {
        T ejecutar(Connection cn) throws SQLException;
    }

    protected <T> List<T> consultar(String sql, Parametros parametros, Mapeo<T> mapeo, String error) {
        try (Connection cn = ConexionBD.getConexion()) {
            return consultar(cn, sql, parametros, mapeo);
        } catch (SQLException e) {
            throw new AccesoDatosException(error, e);
        }
    }

    protected <T> Optional<T> consultarUno(String sql, Parametros parametros, Mapeo<T> mapeo, String error) {
        List<T> lista = consultar(sql, parametros, mapeo, error);
        return lista.isEmpty() ? Optional.empty() : Optional.of(lista.get(0));
    }

    protected static <T> List<T> consultar(Connection cn, String sql, Parametros parametros, Mapeo<T> mapeo)
            throws SQLException {
        try (PreparedStatement ps = cn.prepareStatement(sql)) {
            parametros.asignar(ps);
            try (ResultSet rs = ps.executeQuery()) {
                List<T> lista = new ArrayList<>();
                while (rs.next()) {
                    lista.add(mapeo.mapear(rs));
                }
                return lista;
            }
        }
    }

    protected static int actualizar(Connection cn, String sql, Parametros parametros) throws SQLException {
        try (PreparedStatement ps = cn.prepareStatement(sql)) {
            parametros.asignar(ps);
            return ps.executeUpdate();
        }
    }

    /** Ejecuta un INSERT y devuelve la clave IDENTITY generada. */
    protected static int insertar(Connection cn, String sql, Parametros parametros) throws SQLException {
        try (PreparedStatement ps = cn.prepareStatement(sql, java.sql.Statement.RETURN_GENERATED_KEYS)) {
            parametros.asignar(ps);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (!rs.next()) {
                    throw new SQLException("La base de datos no devolvió el identificador generado");
                }
                return rs.getInt(1);
            }
        }
    }

    protected <T> T enTransaccion(Trabajo<T> trabajo, String error) {
        try (Connection cn = ConexionBD.getConexion()) {
            boolean auto = cn.getAutoCommit();
            cn.setAutoCommit(false);
            try {
                T resultado = trabajo.ejecutar(cn);
                cn.commit();
                return resultado;
            } catch (SQLException | RuntimeException e) {
                cn.rollback();
                throw e;
            } finally {
                cn.setAutoCommit(auto);
            }
        } catch (SQLException e) {
            throw new AccesoDatosException(error, e);
        }
    }

    /** Registra un evento en la bitácora usando la misma conexión (misma transacción). */
    protected static void auditar(Connection cn, int usuarioId, String tabla, String clave,
                                  String anterior, String nuevo, String accion) throws SQLException {
        actualizar(cn, "INSERT INTO Auditoria (Tabla, ClavePrimaria, UsuarioId, FechaCambio, ValorAnterior, "
                + "ValorNuevo, Accion) VALUES (?, ?, ?, ?, ?, ?, ?)", ps -> {
                    ps.setString(1, tabla);
                    ps.setString(2, recortar(clave, 100));
                    ps.setInt(3, usuarioId);
                    ps.setTimestamp(4, Timestamp.valueOf(LocalDateTime.now()));
                    ps.setString(5, recortar(anterior, 300));
                    ps.setString(6, recortar(nuevo, 300));
                    ps.setString(7, accion);
                });
    }

    // ---------- utilidades de lectura y escritura ----------

    protected static String recortar(String texto, int max) {
        return texto == null || texto.length() <= max ? texto : texto.substring(0, max);
    }

    protected static String texto(ResultSet rs, String columna) throws SQLException {
        String v = rs.getString(columna);
        return v == null ? null : v.trim(); // CHAR(n) llega con espacios a la derecha
    }

    protected static LocalDate fecha(ResultSet rs, String columna) throws SQLException {
        java.sql.Date d = rs.getDate(columna);
        return d == null ? null : d.toLocalDate();
    }

    protected static LocalDateTime fechaHora(ResultSet rs, String columna) throws SQLException {
        Timestamp t = rs.getTimestamp(columna);
        return t == null ? null : t.toLocalDateTime();
    }

    protected static void setTextoONulo(PreparedStatement ps, int i, String valor) throws SQLException {
        if (valor == null || valor.isBlank()) {
            ps.setNull(i, Types.VARCHAR);
        } else {
            ps.setString(i, valor.trim());
        }
    }
}
