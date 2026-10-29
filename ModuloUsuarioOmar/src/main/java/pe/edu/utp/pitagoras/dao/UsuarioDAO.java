package pe.edu.utp.pitagoras.dao;

import pe.edu.utp.pitagoras.conexion.ConexionBD;
import pe.edu.utp.pitagoras.model.Usuario;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UsuarioDAO {

    public List<Usuario> listarUsuarios() {
        List<Usuario> lista = new ArrayList<>();
        String sql = "SELECT id, nombre, apellido, correo, tipo_usuario, estado FROM usuario";
        try (Connection conn = ConexionBD.conectar();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                Usuario u = new Usuario(
                    rs.getInt("id"),
                    rs.getString("nombre"),
                    rs.getString("apellido"),
                    rs.getString("correo"),
                    rs.getString("tipo_usuario"),
                    rs.getString("estado")
                );
                lista.add(u);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return lista;
    }

    public boolean insertarUsuario(Usuario u) {
        String sql = "INSERT INTO usuario (nombre, apellido, correo, tipo_usuario, estado) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = ConexionBD.conectar();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, u.getNombre());
            pstmt.setString(2, u.getApellido());
            pstmt.setString(3, u.getCorreo());
            pstmt.setString(4, u.getTipoUsuario());
            pstmt.setString(5, u.getEstado());
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean actualizarUsuario(Usuario u) {
        String sql = "UPDATE usuario SET nombre=?, apellido=?, correo=?, tipo_usuario=?, estado=? WHERE id=?";
        try (Connection conn = ConexionBD.conectar();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, u.getNombre());
            pstmt.setString(2, u.getApellido());
            pstmt.setString(3, u.getCorreo());
            pstmt.setString(4, u.getTipoUsuario());
            pstmt.setString(5, u.getEstado());
            pstmt.setInt(6, u.getId());
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean eliminarUsuario(int id) {
        String sql = "DELETE FROM usuario WHERE id=?";
        try (Connection conn = ConexionBD.conectar();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}
