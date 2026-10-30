package pe.edu.pitagoras.sga.model.gestion;

/**
 * Datos de la institución que aparecen en la cabecera de la libreta (se leen de config.properties).
 *
 * @param nombre         nombre de la institución educativa.
 * @param dre            Dirección Regional de Educación.
 * @param ugel           Unidad de Gestión Educativa Local.
 * @param codigoModular  código modular del MINEDU.
 * @param lugar          ciudad.
 */
public record DatosColegio(String nombre, String dre, String ugel, String codigoModular, String lugar) {

    public static final DatosColegio POR_DEFECTO = new DatosColegio("I.E.P. Pitágoras", "Pasco", "Pasco",
            "0000000", "Cerro de Pasco");
}
