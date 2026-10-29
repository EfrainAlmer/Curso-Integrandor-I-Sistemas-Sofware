package pe.edu.utp.pitagoras.model;

/**
 * Clase de análisis RegistroAsistencia y Justificación (Tabla 6, pág. 38 - RF12 / CU12).
 */
public class RegistroAsistencia {
    private final int estudianteId;
    private final String estudianteNombre;
    private final String seccion;
    private final String fecha;
    private String estado; // "Asistió", "Falta", "Tardanza"
    private boolean justificada;
    private String observacion;

    public RegistroAsistencia(int estudianteId, String estudianteNombre, String seccion,
                              String fecha, String estado, boolean justificada, String observacion) {
        this.estudianteId = estudianteId;
        this.estudianteNombre = estudianteNombre;
        this.seccion = seccion;
        this.fecha = fecha;
        this.estado = estado;
        this.justificada = justificada;
        this.observacion = observacion;
    }

    public RegistroAsistencia copiar() {
        return new RegistroAsistencia(estudianteId, estudianteNombre, seccion, fecha, estado, justificada, observacion);
    }

    public int getEstudianteId() { return estudianteId; }
    public String getEstudianteNombre() { return estudianteNombre; }
    public String getSeccion() { return seccion; }
    public String getFecha() { return fecha; }
    public String getEstado() { return estado; }

    public void setEstado(String estado) {
        this.estado = estado;
        // Regla de negocio RF12 / Figura 12: solo una Falta puede estar marcada como justificada
        if (!"Falta".equals(estado)) {
            this.justificada = false;
        }
    }

    public boolean isJustificada() { return justificada; }

    public void setJustificada(boolean justificada) {
        this.justificada = justificada;
        // Regla de negocio Figura 12 (pág. 35): "una falta justificada conserva el estado Falta"
        if (justificada) {
            this.estado = "Falta";
        }
    }

    public String getObservacion() { return observacion; }
    public void setObservacion(String observacion) { this.observacion = observacion; }
}
