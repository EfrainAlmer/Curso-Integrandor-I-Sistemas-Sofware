package pe.edu.pitagoras.sga.model.evaluacion;

/**
 * Estados del registro diario (RF12). En la BD la falta justificada se guarda como
 * Estado = 'F' con Justificada = 1.
 */
public enum EstadoAsistencia {
    ASISTIO("A", false, "Asistió"),
    TARDANZA("T", false, "Tardanza"),
    FALTA("F", false, "Falta"),
    FALTA_JUSTIFICADA("F", true, "Falta justificada");

    private final String codigo;
    private final boolean justificada;
    private final String texto;

    EstadoAsistencia(String codigo, boolean justificada, String texto) {
        this.codigo = codigo;
        this.justificada = justificada;
        this.texto = texto;
    }

    /** @return código de la columna Estado: A, T o F. */
    public String getCodigo() {
        return codigo;
    }

    public boolean isJustificada() {
        return justificada;
    }

    /**
     * @param codigo      'A', 'T' o 'F'.
     * @param justificada valor de la columna Justificada.
     * @return el estado correspondiente.
     * @throws IllegalArgumentException si el código no existe.
     */
    public static EstadoAsistencia desde(String codigo, boolean justificada) {
        for (EstadoAsistencia e : values()) {
            if (e.codigo.equalsIgnoreCase(codigo == null ? "" : codigo.trim()) && e.justificada == justificada) {
                return e;
            }
        }
        throw new IllegalArgumentException("Estado de asistencia desconocido: " + codigo);
    }

    @Override
    public String toString() {
        return texto;
    }
}
