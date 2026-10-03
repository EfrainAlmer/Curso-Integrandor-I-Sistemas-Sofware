package org.pe.edu.cole.app.model;

/**
 * Modelo simple: Clase base que representa a un usuario autenticado en el sistema.
 * Diseñada para ser extendida en el futuro si cada rol requiere atributos específicos
 * (por ejemplo, subclases como Docente con "especialidad" o Alumno con "grado").
 */
public class Usuario {

    private final int id;
    private final String nombreUsuario;
    private final String rol; // "Administrador, Director, Docente, Estudiante"
    private final String nombreCompleto; // Nombre del profesor/alumno, o el username
    private final boolean debeCambiarPassword; // RF02: true si debe cambiar su contraseña en este ingreso

    /**
     * Este constructor construye una nueva instancia de Usuario con sus datos principales.
     *
     * @param id             el identificador único en la base de datos.
     * @param nombreUsuario  el nombre de cuenta utilizado para iniciar sesión.
     * @param rol            el nivel de acceso (ej. "ADMINISTRADOR", "DIRECTOR", "DOCENTE", "ALUMNO").
     * @param nombreCompleto el nombre real de la persona vinculada a la cuenta.
     * @param debeCambiarPassword si el sistema debe forzar el cambio de contraseña en este ingreso (RF02).
     */
    public Usuario(int id, String nombreUsuario, String rol, String nombreCompleto, boolean debeCambiarPassword) {
        this.id = id;
        this.nombreUsuario = nombreUsuario;
        this.rol = rol;
        this.nombreCompleto = nombreCompleto;
        this.debeCambiarPassword = debeCambiarPassword;
    }//cierra constructor Usuario

    /**
     * Este método obtiene el identificador de la base de datos.
     * @return el identificador único del usuario.
     */
    public int getId() {
        return id;
    }

    /**
     * Este método obtiene el identificador de acceso al sistema.
     * @return el nombre de cuenta (username).
     */
    public String getNombreUsuario() {
        return nombreUsuario;
    }

    /**
     * Este método obtiene los permisos asociados a este usuario.
     * @return el perfil o nivel de acceso.
     */
    public String getRol() {
        return rol;
    }

    /**
     * Este método obtiene la identidad real de la persona.
     * @return el nombre completo asociado a la cuenta.
     */
    public String getNombreCompleto() {
        return nombreCompleto;
    }

    /**
     * Este método indica si el sistema debe forzar el cambio de contraseña.
     * @return true si es el primer ingreso o un administrador lo marcó así.
     */
    public boolean isDebeCambiarPassword() {
        return debeCambiarPassword;
    }
}//cierra class Usuario