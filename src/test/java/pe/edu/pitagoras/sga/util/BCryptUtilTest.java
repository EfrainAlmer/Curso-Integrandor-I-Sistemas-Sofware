package pe.edu.pitagoras.sga.util;

import at.favre.lib.crypto.bcrypt.BCrypt;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BCryptUtilTest {

    private static final char[] CLAVE = "Clave#Segura1".toCharArray();

    @Test
    void hashear_generaHashDistintoEnCadaLlamada() {
        // La sal es aleatoria: la misma clave nunca produce el mismo hash.
        assertNotEquals(BCryptUtil.hashear(CLAVE), BCryptUtil.hashear(CLAVE));
    }

    @Test
    void hashear_usaPrefijo2bYCosto12() {
        assertTrue(BCryptUtil.hashear(CLAVE).startsWith("$2b$12$"));
    }

    @Test
    void verificar_claveCorrecta() {
        assertTrue(BCryptUtil.verificar(CLAVE, BCryptUtil.hashear(CLAVE)));
    }

    @Test
    void verificar_claveIncorrecta() {
        String hash = BCryptUtil.hashear(CLAVE);
        assertFalse(BCryptUtil.verificar("otra-clave".toCharArray(), hash));
    }

    @Test
    void verificar_aceptaPrefijos2a2by2y() {
        BCrypt.Version[] versiones = {
            BCrypt.Version.VERSION_2A, BCrypt.Version.VERSION_2B, BCrypt.Version.VERSION_2Y
        };
        for (BCrypt.Version v : versiones) {
            String hash = BCrypt.with(v).hashToString(4, CLAVE); // costo 4: solo para que el test sea rápido
            assertTrue(BCryptUtil.verificar(CLAVE, hash), "Debe aceptar " + hash.substring(0, 4));
        }
    }

    @Test
    void verificar_entradasInvalidasDevuelvenFalse() {
        String hash = BCryptUtil.hashear(CLAVE);
        assertFalse(BCryptUtil.verificar(CLAVE, null));
        assertFalse(BCryptUtil.verificar(CLAVE, ""));
        assertFalse(BCryptUtil.verificar(CLAVE, "no-es-un-hash"));
        assertFalse(BCryptUtil.verificar(null, hash));
        assertFalse(BCryptUtil.verificar(new char[0], hash));
    }

    @Test
    void hashear_claveVaciaONulaLanzaExcepcion() {
        assertThrows(IllegalArgumentException.class, () -> BCryptUtil.hashear(null));
        assertThrows(IllegalArgumentException.class, () -> BCryptUtil.hashear(new char[0]));
    }
}
