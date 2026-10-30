package pe.edu.pitagoras.sga.dao.impl;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pe.edu.pitagoras.sga.config.ConexionBD;
import pe.edu.pitagoras.sga.dao.interfaces.IRespaldoDAO;
import pe.edu.pitagoras.sga.dao.support.JdbcSupport;
import pe.edu.pitagoras.sga.exception.AccesoDatosException;
import pe.edu.pitagoras.sga.exception.BusinessException;
import pe.edu.pitagoras.sga.model.gestion.CopiaSeguridad;

/**
 * Copias de seguridad con BACKUP/RESTORE DATABASE de SQL Server (RF10). El archivo .bak
 * queda en el disco del servidor. BACKUP y RESTORE no admiten parámetros en todas las
 * versiones, por eso la ruta se valida con una lista blanca de caracteres antes de usarla.
 */
public class RespaldoDAOImpl extends JdbcSupport implements IRespaldoDAO {

    private static final Logger LOG = LoggerFactory.getLogger(RespaldoDAOImpl.class);
    private static final Pattern RUTA_SEGURA = Pattern.compile("[A-Za-z0-9 _:\\\\/.\\-]{3,250}");
    private static final Pattern NOMBRE_BD = Pattern.compile("[A-Za-z0-9_]{1,120}");
    private static final DateTimeFormatter SELLO = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    @Override
    public List<CopiaSeguridad> listar() {
        return consultar("SELECT c.CopiaId, c.Ruta, c.Fecha, u.Username, c.Estado FROM CopiaSeguridad c "
                + "JOIN Usuario u ON u.UsuarioId = c.UsuarioId ORDER BY c.Fecha DESC", ps -> { },
                rs -> new CopiaSeguridad(rs.getInt("CopiaId"), texto(rs, "Ruta"), fechaHora(rs, "Fecha"),
                        texto(rs, "Username"), texto(rs, "Estado")), "No se pudo listar las copias de seguridad");
    }

    @Override
    public CopiaSeguridad generar(String carpeta, int usuarioId) {
        String bd = nombreBaseDatos();
        String destino = StringUtils.isBlank(carpeta) ? carpetaPorDefecto() : carpeta.trim();
        String ruta = StringUtils.removeEnd(StringUtils.removeEnd(destino, "\\"), "/")
                + "\\" + bd + "_" + LocalDateTime.now().format(SELLO) + ".bak";
        validarRuta(ruta);

        String estado = "EXITOSO";
        AccesoDatosException error = null;
        try (Connection cn = ConexionBD.getConexion(); Statement st = cn.createStatement()) {
            // BACKUP no puede ir dentro de una transacción: se usa una conexión en autocommit.
            ejecutarHastaElFinal(st, "BACKUP DATABASE [" + bd + "] TO DISK = N'" + ruta + "' WITH INIT, "
                    + "NAME = N'Respaldo SGA " + LocalDateTime.now().format(SELLO) + "'");
        } catch (SQLException e) {
            estado = "FALLIDO";
            error = new AccesoDatosException("No se pudo generar la copia de seguridad. Verifique que la carpeta "
                    + "exista en el servidor y que el usuario de la aplicación tenga el rol db_backupoperator.", e);
        }
        String estadoFinal = estado;
        int id = enTransaccion(cn -> {
            int copiaId = insertar(cn, "INSERT INTO CopiaSeguridad (Ruta, Fecha, UsuarioId, Estado) VALUES (?, ?, ?, ?)",
                    ps -> {
                        ps.setString(1, ruta);
                        ps.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()));
                        ps.setInt(3, usuarioId);
                        ps.setString(4, estadoFinal);
                    });
            auditar(cn, usuarioId, "CopiaSeguridad", "CopiaId=" + copiaId, null, estadoFinal + ": " + ruta, "BACKUP");
            return copiaId;
        }, "No se pudo registrar la copia de seguridad");
        if (error != null) {
            throw error;
        }
        LOG.info("Copia de seguridad generada: {}", ruta);
        return new CopiaSeguridad(id, ruta, LocalDateTime.now(), null, estadoFinal);
    }

    @Override
    public void restaurar(String ruta, int usuarioId) {
        validarRuta(ruta);
        String bd = nombreBaseDatos();
        ConexionBD.liberarConexiones();
        try (Connection cn = ConexionBD.abrirConexionMaster(); Statement st = cn.createStatement()) {
            ejecutarHastaElFinal(st, "ALTER DATABASE [" + bd + "] SET SINGLE_USER WITH ROLLBACK IMMEDIATE");
            try {
                ejecutarHastaElFinal(st, "RESTORE DATABASE [" + bd + "] FROM DISK = N'" + ruta + "' WITH REPLACE");
            } finally {
                ejecutarHastaElFinal(st, "ALTER DATABASE [" + bd + "] SET MULTI_USER");
            }
        } catch (SQLException e) {
            throw new AccesoDatosException("No se pudo restaurar la copia. Verifique la ruta y que el usuario de la "
                    + "aplicación tenga el rol dbcreator.", e);
        }
        ConexionBD.liberarConexiones();
        // La bitácora restaurada no conoce esta acción: se registra después de restaurar.
        enTransaccion(cn -> {
            auditar(cn, usuarioId, "CopiaSeguridad", "Restauración", null, ruta, "RESTAURAR");
            return null;
        }, "No se pudo registrar la restauración en la bitácora");
        LOG.warn("Base de datos restaurada desde {}", ruta);
    }

    private String nombreBaseDatos() {
        String bd = consultarUno("SELECT DB_NAME() AS Nombre", ps -> { }, rs -> texto(rs, "Nombre"),
                "No se pudo consultar el nombre de la base de datos").orElseThrow();
        if (!NOMBRE_BD.matcher(bd).matches()) {
            throw new BusinessException("Nombre de base de datos no válido para respaldo: " + bd);
        }
        return bd;
    }

    private String carpetaPorDefecto() {
        return consultarUno("SELECT CAST(SERVERPROPERTY('InstanceDefaultBackupPath') AS NVARCHAR(260)) AS Ruta",
                ps -> { }, rs -> texto(rs, "Ruta"), "No se pudo obtener la carpeta de respaldos del servidor")
                .filter(StringUtils::isNotBlank)
                .orElseThrow(() -> new BusinessException("Configure backup.carpeta en config.properties."));
    }

    private static void validarRuta(String ruta) {
        if (ruta == null || !RUTA_SEGURA.matcher(ruta).matches() || ruta.contains("..")) {
            throw new BusinessException("La ruta del respaldo contiene caracteres no permitidos: use letras, "
                    + "números, espacios y \\ : _ - .");
        }
    }

    /** BACKUP y RESTORE devuelven mensajes de avance; hay que leerlos todos para que terminen. */
    private static void ejecutarHastaElFinal(Statement st, String sql) throws SQLException {
        boolean hayResultado = st.execute(sql);
        while (hayResultado || st.getUpdateCount() != -1) {
            hayResultado = st.getMoreResults();
        }
    }
}
