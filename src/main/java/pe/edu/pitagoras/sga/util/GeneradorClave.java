package pe.edu.pitagoras.sga.util;

import java.security.SecureRandom;

/**
 * Genera claves temporales al azar con SecureRandom. Omite caracteres que se confunden
 * al dictarlos (0/O, 1/l/I) y siempre incluye letras y números, como exige RF02.
 */
public final class GeneradorClave {

    private static final String LETRAS = "abcdefghjkmnpqrstuvwxyzABCDEFGHJKLMNPQRSTUVWXYZ";
    private static final String NUMEROS = "23456789";
    private static final String TODOS = LETRAS + NUMEROS;
    private static final int LARGO = 10;
    private static final SecureRandom AZAR = new SecureRandom();

    private GeneradorClave() {
    }

    /** @return una clave de 10 caracteres con al menos una letra y un número. */
    public static String generar() {
        char[] c = new char[LARGO];
        c[0] = LETRAS.charAt(AZAR.nextInt(LETRAS.length()));
        c[1] = NUMEROS.charAt(AZAR.nextInt(NUMEROS.length()));
        for (int i = 2; i < LARGO; i++) {
            c[i] = TODOS.charAt(AZAR.nextInt(TODOS.length()));
        }
        // Mezcla para que la letra y el número no queden siempre al inicio.
        for (int i = LARGO - 1; i > 0; i--) {
            int j = AZAR.nextInt(i + 1);
            char t = c[i];
            c[i] = c[j];
            c[j] = t;
        }
        return new String(c);
    }
}
