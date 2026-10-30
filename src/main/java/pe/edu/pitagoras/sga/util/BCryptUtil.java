package pe.edu.pitagoras.sga.util;

import at.favre.lib.crypto.bcrypt.BCrypt;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;

/**
 * Hash y verificación de contraseñas con BCrypt (RNF02). Las claves se manejan
 * como char[] para poder borrarlas de memoria después de usarlas.
 */
public final class BCryptUtil {

    /** Costo exigido por la sección 6 de la guía. */
    private static final int COSTO = 12;

    private BCryptUtil() {
    }

    /**
     * @param clave contraseña en texto claro; no puede ser nula ni vacía.
     * @return hash con sal aleatoria, prefijo $2b$ y costo 12.
     * @throws IllegalArgumentException si la clave es nula o vacía.
     */
    public static String hashear(char[] clave) {
        Validate.isTrue(clave != null && clave.length > 0, "La contraseña no puede estar vacía");
        return BCrypt.with(BCrypt.Version.VERSION_2B).hashToString(COSTO, clave);
    }

    /**
     * @param clave contraseña escrita por el usuario.
     * @param hash  hash guardado en la base ($2a$, $2b$ o $2y$).
     * @return true solo si la clave corresponde al hash; false ante cualquier dato inválido.
     */
    public static boolean verificar(char[] clave, String hash) {
        if (clave == null || clave.length == 0 || StringUtils.isBlank(hash)) {
            return false;
        }
        try {
            return BCrypt.verifyer().verify(clave, hash).verified;
        } catch (RuntimeException e) {
            return false; // hash con formato inválido: nunca se autentica
        }
    }
}