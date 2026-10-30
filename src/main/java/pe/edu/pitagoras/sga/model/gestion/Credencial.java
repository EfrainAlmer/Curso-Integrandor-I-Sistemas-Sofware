package pe.edu.pitagoras.sga.model.gestion;

/**
 * Usuario y clave temporal que el Director entrega en persona. La clave solo se muestra
 * una vez; en la BD queda únicamente su hash y se exige cambiarla en el primer ingreso.
 *
 * @param username      usuario creado.
 * @param claveTemporal clave generada al azar.
 */
public record Credencial(String username, String claveTemporal) {

    /** No muestra la clave para que no termine en un log. */
    @Override
    public String toString() {
        return "Credencial{" + username + "}";
    }
}
