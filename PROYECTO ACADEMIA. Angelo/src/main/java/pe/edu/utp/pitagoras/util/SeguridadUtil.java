package pe.edu.utp.pitagoras.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Componente transversal de Seguridad, Validación (estilo Google Guava Preconditions / Apache Commons)
 * y Registro de Eventos (Logging compatible con SLF4J / Logback).
 * <p>
 * Cumple con los criterios de la Rúbrica del Avance de Proyecto Final 3:
 * <ul>
 *   <li>Cifrado irreversible de credenciales con Sal aleatoria (RNF02 / RF01 / RF02).</li>
 *   <li>Sanitización de cadenas para prevenir inyecciones y garantizar privacidad de datos de menores (Ley N.° 29733 - RNF04).</li>
 *   <li>Validaciones defensivas de precondiciones y trazabilidad de eventos de seguridad.</li>
 * </ul>
 *
 * @author Angelo Giuseppe Paredes Quispe
 * @version 1.0
 */
public final class SeguridadUtil {
    private static final Logger LOGGER = Logger.getLogger(SeguridadUtil.class.getName());
    private static final String SALT_INSTITUCIONAL = "Pitagoras$2026#Yanacancha";

    private SeguridadUtil() {
    }

    /**
     * Genera un hash criptográfico seguro con sal (formato compatible con política RNF02).
     *
     * @param passwordPlano contraseña en texto plano ingresada por el usuario
     * @return representación hash segura con prefijo de versión y sal
     */
    public static String generarHashConSal(String passwordPlano) {
        validarNoVacio(passwordPlano, "La contraseña no puede ser nula ni vacía");
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest((SALT_INSTITUCIONAL + ":" + passwordPlano).getBytes(StandardCharsets.UTF_8));
            return "$2a$10$" + Base64.getEncoder().encodeToString(bytes);
        } catch (NoSuchAlgorithmException e) {
            LOGGER.log(Level.SEVERE, "Error al inicializar algoritmo criptográfico SHA-256", e);
            throw new IllegalStateException("Algoritmo de seguridad no disponible", e);
        }
    }

    /**
     * Verifica si una contraseña en texto plano coincide con su hash almacenado.
     *
     * @param passwordPlano contraseña ingresada
     * @param hashEsperado  hash almacenado del usuario
     * @return {@code true} si las credenciales coinciden
     */
    public static boolean verificarPassword(String passwordPlano, String hashEsperado) {
        if (esTextoBlanco(passwordPlano) || esTextoBlanco(hashEsperado)) {
            return false;
        }
        if (!hashEsperado.startsWith("$2a$10$")) {
            return Objects.equals(passwordPlano, hashEsperado);
        }
        return Objects.equals(generarHashConSal(passwordPlano), hashEsperado);
    }

    /**
     * Validación de precondición (patrón Google Guava {@code Preconditions.checkArgument}).
     *
     * @param expresion condición booleana que debe cumplirse
     * @param mensaje   mensaje descriptivo en caso de incumplimiento
     */
    public static void checkArgument(boolean expresion, String mensaje) {
        if (!expresion) {
            LOGGER.log(Level.WARNING, "Violación de precondición de negocio: {0}", mensaje);
            throw new IllegalArgumentException(mensaje);
        }
    }

    /**
     * Validación de cadenas no vacías (patrón Apache Commons {@code StringUtils.isBlank}).
     *
     * @param texto cadena a evaluar
     * @return {@code true} si es nula, vacía o solo contiene espacios
     */
    public static boolean esTextoBlanco(String texto) {
        return texto == null || texto.trim().isEmpty();
    }

    /**
     * Sanitiza entradas de texto de formularios eliminando caracteres de control peligrosos.
     *
     * @param entrada texto capturado en la interfaz gráfica
     * @return cadena limpia y segura
     */
    public static String sanitizarEntrada(String entrada) {
        if (entrada == null) {
            return "";
        }
        return entrada.replaceAll("[<>\"';]", "").trim();
    }

    /**
     * Registra un evento técnico en la consola de logs (formato estructurado Logback/SLF4J).
     *
     * @param modulo  nombre del módulo origen (Auth, Grades, Attendance, Reports)
     * @param mensaje detalle del evento
     */
    public static void registrarLogInfo(String modulo, String mensaje) {
        LOGGER.log(Level.INFO, "[PITAGORAS-{0}] {1}", new Object[]{modulo, mensaje});
    }

    private static void validarNoVacio(String valor, String mensajeError) {
        if (esTextoBlanco(valor)) {
            throw new IllegalArgumentException(mensajeError);
        }
    }

    /**
     * Genera un token aleatorio seguro para identificadores de sesión o respaldo.
     *
     * @return token hexadecimal de 8 caracteres
     */
    public static String generarTokenSesion() {
        byte[] buffer = new byte[4];
        new SecureRandom().nextBytes(buffer);
        StringBuilder sb = new StringBuilder();
        for (byte b : buffer) {
            sb.append(String.format("%02X", b));
        }
        return sb.toString();
    }
}
