package pe.edu.utp.pitagoras.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Clase de análisis BitácoraEvento (Tabla 6, pág. 38 - RF09 / CU09).
 * Registra modificaciones críticas de notas y asistencia con usuario, fecha, valor anterior y valor nuevo.
 */
public class BitacoraEvento {
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final String fechaHora;
    private final String usuario;
    private final String modulo;
    private final String accion;
    private final String valorAnterior;
    private final String valorNuevo;

    public BitacoraEvento(String usuario, String modulo, String accion, String valorAnterior, String valorNuevo) {
        this.fechaHora = LocalDateTime.now().format(FMT);
        this.usuario = usuario;
        this.modulo = modulo;
        this.accion = accion;
        this.valorAnterior = valorAnterior;
        this.valorNuevo = valorNuevo;
    }

    public BitacoraEvento(String fechaHora, String usuario, String modulo, String accion, String valorAnterior, String valorNuevo) {
        this.fechaHora = fechaHora;
        this.usuario = usuario;
        this.modulo = modulo;
        this.accion = accion;
        this.valorAnterior = valorAnterior;
        this.valorNuevo = valorNuevo;
    }

    public String getFechaHora() { return fechaHora; }
    public String getUsuario() { return usuario; }
    public String getModulo() { return modulo; }
    public String getAccion() { return accion; }
    public String getValorAnterior() { return valorAnterior; }
    public String getValorNuevo() { return valorNuevo; }
}
