package pe.edu.pitagoras.sga.model.auth;

/**
 * Lo que recibe la vista tras un login correcto.
 *
 * @param sesion              quién ingresó y con qué rol.
 * @param debeCambiarPassword true en el primer ingreso: la vista debe abrir el cambio de clave (RF02).
 */
public record ResultadoLogin(Sesion sesion, boolean debeCambiarPassword) {
}