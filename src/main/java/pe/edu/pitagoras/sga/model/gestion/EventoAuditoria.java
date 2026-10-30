package pe.edu.pitagoras.sga.model.gestion;

import java.time.LocalDateTime;

/**
 * Registro de la bitácora (RF09).
 *
 * @param fecha         cuándo ocurrió.
 * @param usuario       quién lo hizo.
 * @param tabla         tabla afectada.
 * @param clave         identificador del registro afectado.
 * @param valorAnterior valor previo (null si es nuevo).
 * @param valorNuevo    valor final.
 * @param accion        REGISTRAR, MODIFICAR, CERRAR_PERIODO, ...
 */
public record EventoAuditoria(LocalDateTime fecha, String usuario, String tabla, String clave,
                              String valorAnterior, String valorNuevo, String accion) {
}
