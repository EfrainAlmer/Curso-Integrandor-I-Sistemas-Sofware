package pe.edu.pitagoras.sga.config;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import org.apache.commons.lang3.StringUtils;

/**
 * Lee config.properties. Primero busca un archivo junto a la aplicación (útil al
 * entregar el JAR) y, si no existe, usa el de src/main/resources. Los secretos
 * nunca van en el código ni en Git (sección 6 de la guía).
 */
public final class AppConfig {

    private static final String ARCHIVO = "config.properties";

    private final Properties propiedades;

    private AppConfig(Properties propiedades) {
        this.propiedades = propiedades;
    }

    /**
     * Crea la configuración a partir de propiedades ya cargadas (útil en pruebas).
     *
     * @param propiedades valores de configuración.
     * @return la configuración.
     */
    public static AppConfig de(Properties propiedades) {
        Properties copia = new Properties();
        copia.putAll(propiedades);
        return new AppConfig(copia);
    }

    /**
     * @return la configuración cargada.
     * @throws IllegalStateException si falta el archivo o no se puede leer.
     */
    public static AppConfig cargar() {
        Properties props = new Properties();
        Path externo = Path.of(ARCHIVO);
        try {
            if (Files.exists(externo)) {
                try (InputStream in = Files.newInputStream(externo)) {
                    props.load(new InputStreamReader(in, StandardCharsets.UTF_8));
                }
            } else {
                try (InputStream in = AppConfig.class.getClassLoader().getResourceAsStream(ARCHIVO)) {
                    if (in == null) {
                        throw new IllegalStateException("No se encontró config.properties. "
                                + "Copie config.example.properties como config.properties y complete sus datos.");
                    }
                    props.load(new InputStreamReader(in, StandardCharsets.UTF_8));
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo leer config.properties", e);
        }
        return new AppConfig(props);
    }

    /**
     * @param clave nombre de la propiedad, por ejemplo "db.url".
     * @return el valor sin espacios sobrantes.
     * @throws IllegalStateException si la propiedad no existe o está vacía.
     */
    public String obtener(String clave) {
        String valor = propiedades.getProperty(clave);
        if (StringUtils.isBlank(valor)) {
            throw new IllegalStateException("Falta la propiedad '" + clave + "' en config.properties");
        }
        return valor.trim();
    }

    /**
     * @param clave   nombre de la propiedad.
     * @param defecto valor si la propiedad no existe o está vacía.
     * @return el valor sin espacios sobrantes, o el valor por defecto.
     */
    public String obtenerOpcional(String clave, String defecto) {
        String valor = propiedades.getProperty(clave);
        return StringUtils.isBlank(valor) ? defecto : valor.trim();
    }
}
