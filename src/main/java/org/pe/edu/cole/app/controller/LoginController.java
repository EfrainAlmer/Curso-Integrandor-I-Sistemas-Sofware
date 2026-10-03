package org.pe.edu.cole.app.controller;

import org.pe.edu.cole.app.dao.CuentaBloqueadaException;
import org.pe.edu.cole.app.dao.UsuarioDAO;
import org.pe.edu.cole.app.model.Usuario;
import org.pe.edu.cole.app.view.LoginView;

import javax.swing.*;
import java.text.SimpleDateFormat;

/**
 * Controlador que gestiona la interacción entre la vista de inicio de sesión y el acceso a datos.
 * Se encarga de validar las credenciales y determinar qué ventana abrir
 * según el rol devuelto por la base de datos (sp_AutenticarUsuario).
 */
public class LoginController {

    private final LoginView vista;
    private final UsuarioDAO usuarioDAO;

    /**
     * Constructor que inicializa el controlador vinculando la vista y configurando los eventos de los botones.
     * @param vista la ventana principal de inicio de sesión de la aplicación.
     */
    public LoginController(LoginView vista) {
        this.vista = vista;
        this.usuarioDAO = new UsuarioDAO();
        this.vista.getBtnLogin().addActionListener(e -> iniciarSesion());
    }// cierre de constructor LoginController

    /**
     * Este método ejecuta el proceso de inicio de sesión al interactuar con el botón.
     * Valida que los campos no estén vacíos, consulta las credenciales en la base de datos
     * y cierra la ventana actual si la autenticación es exitosa.
     */
    private void iniciarSesion() {
        String usuario = vista.getUsuario();
        String password = vista.getPassword();

        if (usuario.isBlank() || password.isBlank()) {
            JOptionPane.showMessageDialog(vista, "Completa usuario y contraseña.");
            return;
        }

        Usuario u;
        try {
            u = usuarioDAO.autenticar(usuario, password);
        } catch (CuentaBloqueadaException e) {
            String hora = new SimpleDateFormat("HH:mm").format(e.getBloqueadoHasta());
            JOptionPane.showMessageDialog(vista,
                    "Cuenta bloqueada por intentos fallidos. Vuelve a intentar después de las " + hora + ".");
            return;
        }

        if (u == null) {
            JOptionPane.showMessageDialog(vista, "Usuario o contraseña incorrectos.");
            return;
        }

        if (u.isDebeCambiarPassword()) {
            // TODO (RF02): reemplazar este aviso por una pantalla real de cambio de contraseña.
            JOptionPane.showMessageDialog(vista,
                    "Debes cambiar tu contraseña en este ingreso.\n(Pantalla de cambio de contraseña pendiente de implementar.)");
        }

        vista.dispose();
        abrirVistaSegunRol(u);
    }//cierra método iniciarSesion

    /**
     * Este método redirige al usuario a su panel correspondiente basándose en sus permisos.
     * La evaluación del rol se realiza en mayúsculas para evitar errores tipográficos.
     * @param u el usuario previamente autenticado con sus datos cargados.
     */
    private void abrirVistaSegunRol(Usuario u) {
        // Nombres tal como se sembraron en la tabla Rol:
        // 'Administrador', 'Director', 'Docente', 'Estudiante'.
        switch (u.getRol().toUpperCase()) {
            case "ADMINISTRADOR" -> System.out.println("Abrir panel de Administrador para "
                    + u.getNombreCompleto());
            case "DIRECTOR" -> System.out.println("Abrir panel de Director para "
                    + u.getNombreCompleto());
            case "DOCENTE" -> System.out.println("Abrir panel de Docente para "
                    + u.getNombreCompleto());
            case "ESTUDIANTE" -> System.out.println("Abrir panel de Alumno para "
                    + u.getNombreCompleto());
            default -> JOptionPane.showMessageDialog(null, "Rol no reconocido.");
        }
        // Aquí en el siguiente paso instanciamos AdminView, DirectorView, DocenteView o AlumnoView
    }//cierra el método abrirVistaSegundoRol
}//cierre class LoginController