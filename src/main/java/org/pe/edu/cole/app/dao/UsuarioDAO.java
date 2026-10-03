package org.pe.edu.cole.app.dao;

import at.favre.lib.crypto.bcrypt.BCrypt;
import org.pe.edu.cole.app.dao.conexion.ConexionSQL;
import org.pe.edu.cole.app.model.Usuario;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;


/**
 * Autenticación de usuarios contra la base de datos.
 * Ya no calcula SHA-256: con BCrypt la verificación no puede hacerse con
 * "WHERE PasswordHash = ?" en SQL, porque la sal va embebida en el propio
 * hash. Por eso el flujo cambia a 3 pasos, cada uno con su propio SP:
 * (1) sp_ObtenerUsuarioParaLogin trae los datos (incluido el hash guardado),
 * (2) esta clase verifica la contraseña en Java con BCrypt,
 * (3) según el resultado, se llama a sp_RegistrarLoginExitoso o
 *     sp_RegistrarIntentoFallido para actualizar el contador/bloqueo (RF02).
 */
public class UsuarioDAO {

    /**
     * Este método verifica las credenciales del usuario y aplica las reglas de bloqueo (RF02).
     *
     * @param nombreUsuario el nombre de cuenta ingresado en el formulario.
     * @param passwordPlano la contraseña en texto plano sin encriptar.
     * @return un objeto Usuario con los datos cargados si las credenciales son válidas,
     *         o null si el usuario no existe, está inactivo, o la contraseña no coincide.
     * @throws CuentaBloqueadaException si la cuenta sigue bloqueada por intentos fallidos previos.
     */
    public Usuario autenticar(String nombreUsuario, String passwordPlano) throws CuentaBloqueadaException {
        FilaLogin fila = obtenerFilaParaLogin(nombreUsuario);

        if (fila == null) {
            return null; // usuario no existe o Estado = 0
        }

        Timestamp ahora = new Timestamp(System.currentTimeMillis());
        if (fila.bloqueadoHasta != null && fila.bloqueadoHasta.after(ahora)) {
            throw new CuentaBloqueadaException(fila.bloqueadoHasta);
        }

        boolean coincide = BCrypt.verifyer()
                .verify(passwordPlano.toCharArray(), fila.passwordHash)
                .verified;

        if (!coincide) {
            registrarIntentoFallido(fila.usuarioId);
            return null;
        }

        registrarLoginExitoso(fila.usuarioId);

        return new Usuario(fila.usuarioId, fila.username, fila.rol, fila.nombreCompleto, fila.debeCambiarPassword);
    }//cierra método autenticar

    /**
     * Este método trae del SP los datos necesarios para validar el login (incluido el
     * hash guardado), sin comparar la contraseña en SQL.
     *
     * @param nombreUsuario el nombre de cuenta a buscar.
     * @return los datos crudos de la fila, o null si no existe el usuario o está inactivo.
     */
    private FilaLogin obtenerFilaParaLogin(String nombreUsuario) {
        String sql = "{call sp_ObtenerUsuarioParaLogin(?)}";

        try (Connection con = ConexionSQL.getConexion();
             CallableStatement cs = con.prepareCall(sql)) {

            cs.setString(1, nombreUsuario);

            try (ResultSet rs = cs.executeQuery()) {
                if (rs.next()) {
                    FilaLogin fila = new FilaLogin();
                    fila.usuarioId = rs.getInt("UsuarioId");
                    fila.username = rs.getString("Username");
                    fila.passwordHash = rs.getString("PasswordHash");
                    fila.debeCambiarPassword = rs.getBoolean("DebeCambiarPassword");
                    fila.bloqueadoHasta = rs.getTimestamp("BloqueadoHasta");
                    fila.rol = rs.getString("Rol");
                    fila.nombreCompleto = rs.getString("NombreCompleto");
                    return fila;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }//cierra método obtenerFilaParaLogin

    /**
     * Este método suma un intento fallido; el SP decide si corresponde bloquear la cuenta (RF02).
     * @param usuarioId el identificador del usuario que falló el intento.
     */
    private void registrarIntentoFallido(int usuarioId) {
        ejecutarSpConUsuarioId("{call sp_RegistrarIntentoFallido(?)}", usuarioId);
    }//cierra método registrarIntentoFallido

    /**
     * Este método limpia el contador de intentos fallidos tras un login correcto.
     * @param usuarioId el identificador del usuario que inició sesión con éxito.
     */
    private void registrarLoginExitoso(int usuarioId) {
        ejecutarSpConUsuarioId("{call sp_RegistrarLoginExitoso(?)}", usuarioId);
    }//cierra método registrarLoginExitoso

    /**
     * Este método Helper para llamar un SP que solo recibe un
     * @UsuarioId y no devuelve resultados.
     */
    private void ejecutarSpConUsuarioId(String sql, int usuarioId) {
        try (Connection con = ConexionSQL.getConexion();
             CallableStatement cs = con.prepareCall(sql)) {
            cs.setInt(1, usuarioId);
            cs.execute();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }//cierra método ejecutarSpConUsuarioId

    /** Fila cruda devuelta por sp_ObtenerUsuarioParaLogin, antes de verificar la contraseña. */
    private static class FilaLogin {
        int usuarioId;
        String username;
        String passwordHash;
        boolean debeCambiarPassword;
        Timestamp bloqueadoHasta;
        String rol;
        String nombreCompleto;
    }//cierra clase FilaLogin
}//cierra class UsuarioDAO