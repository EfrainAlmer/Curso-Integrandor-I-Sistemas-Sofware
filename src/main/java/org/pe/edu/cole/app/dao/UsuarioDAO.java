package org.pe.edu.cole.app.dao;

import org.pe.edu.cole.app.dao.conexion.ConexionSQL;
import org.pe.edu.cole.app.model.Usuario;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Toda la SQL de autenticación vive en el stored procedure sp_AutenticarUsuario.
 * Aquí solo:
 * (1) calculamos el hash SHA-256 de la contraseña ingresada,
 * (2) llamamos al SP,
 * (3) mapeamos el resultado a un Usuario.
 */
public class UsuarioDAO {

    /**
     * Este método verifica las credenciales del usuario contra la base de datos.
     * Encripta la contraseña ingresada en SHA-256 antes de enviarla al procedimiento almacenado
     * y mapea el resultado devuelto a un objeto Usuario.
     *
     * @param nombreUsuario el nombre de cuenta ingresado en el formulario.
     * @param passwordPlano la contraseña en texto plano sin encriptar.
     * @return un objeto Usuario con los datos cargados si las credenciales son válidas, o null si fallan o el usuario está inactivo.
     */
    public Usuario autenticar(String nombreUsuario, String passwordPlano) {
        String passwordHash = hashSHA256(passwordPlano);

        String sql = "{call sp_AutenticarUsuario(?, ?)}";

        try (Connection con = ConexionSQL.getConexion();
             CallableStatement cs = con.prepareCall(sql)) {

            cs.setString(1, nombreUsuario);
            cs.setString(2, passwordHash);

            try (ResultSet rs = cs.executeQuery()) {
                if (rs.next()) {
                    return new Usuario(
                            rs.getInt("UsuarioId"),
                            rs.getString("Username"),
                            rs.getString("Rol"),
                            rs.getString("NombreCompleto")
                    );
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null; // credenciales inválidas o usuario no existe o Estado = 0.
    }//cierre constructor Usario autenticar

    /**
     * Este método calcula el hash criptográfico SHA-256 de una cadena de texto.
     *
     * @param texto la cadena de texto original (contraseña).
     * @return una cadena hexadecimal que representa el hash generado.
     * @throws RuntimeException si el algoritmo SHA-256 no está disponible en el entorno de ejecución.
     */
    private String hashSHA256(String texto) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(texto.getBytes(StandardCharsets.UTF_8));

            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 siempre está disponible en el JDK, esto no debería ocurrir nunca
            throw new RuntimeException("No se pudo calcular el hash de la contraseña", e);
        }
    }//cierra método String hashSHA256
}//cierra class UsuarioDAO