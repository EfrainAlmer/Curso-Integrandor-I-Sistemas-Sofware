package pe.edu.utp.pitagoras.model;

public class UsuarioSesion {
    private int usuarioId;
    private String username;
    private String passwordHash;
    private String nombreCompleto;
    private String rol; // "Director / Coordinador", "Docente", "Alumno / Apoderado"
    private boolean debeCambiarPassword;
    private int intentosFallidos;
    private boolean bloqueado;

    public UsuarioSesion(int usuarioId, String username, String passwordHash, String nombreCompleto,
                         String rol, boolean debeCambiarPassword) {
        this.usuarioId = usuarioId;
        this.username = username;
        this.passwordHash = passwordHash;
        this.nombreCompleto = nombreCompleto;
        this.rol = rol;
        this.debeCambiarPassword = debeCambiarPassword;
        this.intentosFallidos = 0;
        this.bloqueado = false;
    }

    public int getUsuarioId() { return usuarioId; }
    public String getUsername() { return username; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public String getNombreCompleto() { return nombreCompleto; }
    public String getRol() { return rol; }
    public boolean isDebeCambiarPassword() { return debeCambiarPassword; }
    public void setDebeCambiarPassword(boolean debeCambiarPassword) { this.debeCambiarPassword = debeCambiarPassword; }
    public int getIntentosFallidos() { return intentosFallidos; }
    public void setIntentosFallidos(int intentosFallidos) { this.intentosFallidos = intentosFallidos; }
    public boolean isBloqueado() { return bloqueado; }
    public void setBloqueado(boolean bloqueado) { this.bloqueado = bloqueado; }
}
