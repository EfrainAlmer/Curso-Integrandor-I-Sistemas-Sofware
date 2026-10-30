package pe.edu.pitagoras.sga.model.evaluacion;

/** Una fila de la lista de asistencia del día. Mutable porque la vista la edita. */
public class FilaAsistencia {

    private final int alumnoId;
    private final String dni;
    private final String alumno;
    private final boolean registrada;
    private EstadoAsistencia estado;
    private String observacion;

    /**
     * @param registrada true si ya existía una marca guardada para esa fecha.
     */
    public FilaAsistencia(int alumnoId, String dni, String alumno, EstadoAsistencia estado,
                          String observacion, boolean registrada) {
        this.alumnoId = alumnoId;
        this.dni = dni;
        this.alumno = alumno;
        this.estado = estado;
        this.observacion = observacion;
        this.registrada = registrada;
    }

    public int getAlumnoId() {
        return alumnoId;
    }

    public String getDni() {
        return dni;
    }

    public String getAlumno() {
        return alumno;
    }

    public boolean isRegistrada() {
        return registrada;
    }

    public EstadoAsistencia getEstado() {
        return estado;
    }

    public void setEstado(EstadoAsistencia estado) {
        this.estado = estado;
    }

    public String getObservacion() {
        return observacion;
    }

    public void setObservacion(String observacion) {
        this.observacion = observacion;
    }

    /** @return la marca lista para guardar. */
    public MarcaAsistencia aMarca() {
        return new MarcaAsistencia(alumnoId, estado, observacion);
    }
}
