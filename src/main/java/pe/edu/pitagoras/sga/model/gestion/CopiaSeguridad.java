package pe.edu.pitagoras.sga.model.gestion;

import java.time.LocalDateTime;

/**
 * Copia de seguridad registrada (RF10).
 *
 * @param copiaId identificador.
 * @param ruta    archivo .bak en el servidor.
 * @param fecha   cuándo se generó.
 * @param usuario quién la generó.
 * @param estado  EXITOSO o FALLIDO.
 */
public record CopiaSeguridad(int copiaId, String ruta, LocalDateTime fecha, String usuario, String estado) {
}
