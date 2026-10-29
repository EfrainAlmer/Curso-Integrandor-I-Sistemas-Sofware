package pe.edu.utp.pitagoras.model;

public class Usuario {
    private int id;
    private String nombre;
    private String apellido;
    private String correo;
    private String tipoUsuario;
    private String estado;

    public Usuario() {}

    public Usuario(int id, String nombre, String apellido, String correo, String tipoUsuario, String estado) {
        this.id = id;
        this.nombre = nombre;
        this.apellido = apellido;
        this.correo = correo;
        this.tipoUsuario = tipoUsuario;
        this.estado = estado;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getApellido() { return apellido; }
    public void setApellido(String apellido) { this.apellido = apellido; }

    public String getCorreo() { return correo; }
    public void setCorreo(String correo) { this.correo = correo; }

    public String getTipoUsuario() { return tipoUsuario; }
    public void setTipoUsuario(String tipoUsuario) { this.tipoUsuario = tipoUsuario; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
}
