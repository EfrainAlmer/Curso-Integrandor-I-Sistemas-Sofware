package pe.edu.pitagoras.sga.util;

import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;
import pe.edu.pitagoras.sga.exception.BusinessException;

/**
 * Validaciones de entrada comunes. Lanzan BusinessException con un mensaje que la vista
 * muestra tal cual. Todo dato que va a la BD pasa por aquí antes de llegar al DAO.
 */
public final class Validaciones {

    private static final Pattern DNI = Pattern.compile("\\d{8}");
    private static final Pattern USUARIO = Pattern.compile("[a-z0-9._]{4,30}");
    private static final Pattern EMAIL = Pattern.compile("[^@\\s]+@[^@\\s]+\\.[^@\\s]+");
    private static final Pattern TELEFONO = Pattern.compile("[0-9 +\\-]{6,20}");
    private static final Pattern NOMBRE = Pattern.compile("[\\p{L} .'\\-]+");

    private Validaciones() {
    }

    /** @return el texto sin espacios sobrantes. */
    public static String requerido(String valor, String campo, int max) {
        String limpio = StringUtils.normalizeSpace(valor);
        if (StringUtils.isEmpty(limpio)) {
            throw new BusinessException("Ingrese " + campo + ".");
        }
        if (limpio.length() > max) {
            throw new BusinessException(StringUtils.capitalize(campo) + " admite como máximo " + max + " caracteres.");
        }
        return limpio;
    }

    /** Nombres y apellidos: solo letras, espacios, punto, apóstrofo y guion. */
    public static String nombre(String valor, String campo, int max) {
        String limpio = requerido(valor, campo, max);
        if (!NOMBRE.matcher(limpio).matches()) {
            throw new BusinessException(StringUtils.capitalize(campo) + " solo puede contener letras.");
        }
        return limpio;
    }

    public static String dni(String valor) {
        String limpio = StringUtils.trimToEmpty(valor);
        if (!DNI.matcher(limpio).matches()) {
            throw new BusinessException("El DNI debe tener exactamente 8 dígitos.");
        }
        return limpio;
    }

    public static String usuario(String valor) {
        String limpio = StringUtils.trimToEmpty(valor).toLowerCase(java.util.Locale.ROOT);
        if (!USUARIO.matcher(limpio).matches()) {
            throw new BusinessException("El usuario debe tener de 4 a 30 caracteres: letras minúsculas, "
                    + "números, punto o guion bajo.");
        }
        return limpio;
    }

    /** @return el correo limpio, o null si se dejó vacío. */
    public static String emailOpcional(String valor) {
        String limpio = StringUtils.trimToNull(valor);
        if (limpio != null && (limpio.length() > 100 || !EMAIL.matcher(limpio).matches())) {
            throw new BusinessException("El correo electrónico no es válido.");
        }
        return limpio;
    }

    /** @return el teléfono limpio, o null si se dejó vacío. */
    public static String telefonoOpcional(String valor) {
        String limpio = StringUtils.trimToNull(valor);
        if (limpio != null && !TELEFONO.matcher(limpio).matches()) {
            throw new BusinessException("El teléfono solo puede tener números, espacios, + y - (6 a 20).");
        }
        return limpio;
    }
}
