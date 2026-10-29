package pe.edu.utp.pitagoras.controller;

import pe.edu.utp.pitagoras.model.UsuarioSesion;
import java.util.HashMap;
import java.util.Map;

public class AuthController {
    private final Map<String, UsuarioSesion> usuariosMock = new HashMap<>();

    public AuthController() {
        // Usuarios de prueba para los perfiles del PDF + 2 Docentes con cursos diferenciados
        usuariosMock.put("director", new UsuarioSesion(
                1, "director", "123456", "Director General", "Director / Coordinador", false));
        usuariosMock.put("docente", new UsuarioSesion(
                2, "docente", "123456", "Prof. Carlos Mendoza", "Docente", false));
        usuariosMock.put("docente2", new UsuarioSesion(
                4, "docente2", "123456", "Prof. María Quispe", "Docente", false));
        usuariosMock.put("apoderado", new UsuarioSesion(
                3, "apoderado", "123456", "Estudiante A · 3.° A", "Alumno / Apoderado", true));
    }

    public ResultadoLogin autenticar(String username, String password) {
        UsuarioSesion user = usuariosMock.get(username.toLowerCase().trim());
        if (user == null) {
            return new ResultadoLogin(false, "Usuario o contraseña incorrectos.", null);
        }

        if (user.isBloqueado()) {
            return new ResultadoLogin(false,
                    "Cuenta bloqueada temporalmente tras 3 intentos fallidos (RF02). Contacte a Dirección.", null);
        }

        if (!user.getPasswordHash().equals(password)) {
            int intentos = user.getIntentosFallidos() + 1;
            user.setIntentosFallidos(intentos);
            if (intentos >= 3) {
                user.setBloqueado(true);
                return new ResultadoLogin(false,
                        "Ha alcanzado 3 intentos fallidos. Su cuenta ha sido bloqueada temporalmente (RF02).", null);
            }
            return new ResultadoLogin(false,
                    "Contraseña incorrecta. Intento " + intentos + " de 3.", null);
        }

        user.setIntentosFallidos(0);
        if ("Docente".equals(user.getRol())) {
            DocenteController.getInstancia().establecerDocenteActivo(user.getUsername(), user.getNombreCompleto());
        }
        return new ResultadoLogin(true, "OK", user);
    }

    public void actualizarPassword(UsuarioSesion user, String nuevaPassword) {
        user.setPasswordHash(nuevaPassword);
        user.setDebeCambiarPassword(false);
    }

    public record ResultadoLogin(boolean exito, String mensaje, UsuarioSesion usuario) {}
}
