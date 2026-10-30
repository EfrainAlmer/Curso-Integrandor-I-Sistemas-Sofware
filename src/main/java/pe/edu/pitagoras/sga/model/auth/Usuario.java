package pe.edu.pitagoras.sga.model.auth;

import java.time.LocalDateTime;

/** Cuenta de acceso tal como está en la tabla Usuario. */
public class Usuario {

    private final int usuarioId;
    private final String username;
    private final String passwordHash;
    private final Rol rol;
    private final boolean activo;
    private final boolean debeCambiarPassword;
    private final int intentosFallidos;
    private final LocalDateTime bloqueadoHasta;

    public Usuario(int usuarioId, String username, String passwordHash, Rol rol, boolean activo,
                   boolean debeCambiarPassword, int intentosFallidos, LocalDateTime bloqueadoHasta) {
        this.usuarioId = usuarioId;
        this.username = username;
        this.passwordHash = passwordHash;
        this.rol = rol;
        this.activo = activo;
        this.debeCambiarPassword = debeCambiarPassword;
        this.intentosFallidos = intentosFallidos;
        this.bloqueadoHasta = bloqueadoHasta;
    }

    /** @return true si la cuenta sigue bloqueada en el instante indicado. */
    public boolean estaBloqueado(LocalDateTime ahora) {
        return bloqueadoHasta != null && ahora.isBefore(bloqueadoHasta);
    }

    public int getUsuarioId() { return usuarioId; }
    public String getUsername() { return username; }
    public String getPasswordHash() { return passwordHash; }
    public Rol getRol() { return rol; }
    public boolean isActivo() { return activo; }
    public boolean isDebeCambiarPassword() { return debeCambiarPassword; }
    public int getIntentosFallidos() { return intentosFallidos; }
    public LocalDateTime getBloqueadoHasta() { return bloqueadoHasta; }

    /** No incluye el hash para que nunca aparezca en los logs. */
    @Override
    public String toString() {
        return "Usuario{id=" + usuarioId + ", rol=" + rol + "}";
    }
}