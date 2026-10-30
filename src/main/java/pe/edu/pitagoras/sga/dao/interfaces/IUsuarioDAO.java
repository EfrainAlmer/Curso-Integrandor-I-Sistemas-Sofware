package pe.edu.pitagoras.sga.dao.interfaces;

import java.time.LocalDateTime;
import java.util.Optional;
import pe.edu.pitagoras.sga.model.auth.Usuario;

/** Contrato de persistencia de cuentas de acceso. El service solo conoce esta interfaz (DIP). */
public interface IUsuarioDAO {

    /** @return la cuenta con ese username, o vacío si no existe. */
    Optional<Usuario> buscarPorUsername(String username);

    /** Guarda el contador de fallos y, si corresponde, hasta cuándo está bloqueada la cuenta. */
    void actualizarIntentos(int usuarioId, int intentosFallidos, LocalDateTime bloqueadoHasta);

    /** Pone en cero los fallos y quita el bloqueo. */
    void reiniciarIntentos(int usuarioId);

    /** Guarda el nuevo hash y marca que ya no debe cambiar la clave. */
    void actualizarPassword(int usuarioId, String nuevoHash);

    /** @return el alumno vinculado a la cuenta del apoderado, o vacío si no tiene. */
    Optional<Integer> buscarAlumnoIdPorUsuario(int usuarioId);

    /** Bitácora de accesos (RNF04). resultado: EXITOSO, FALLIDO o BLOQUEADO. Nunca recibe contraseñas. */
    void registrarAcceso(String username, String resultado);
}