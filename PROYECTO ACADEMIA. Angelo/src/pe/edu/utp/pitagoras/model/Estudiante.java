package pe.edu.utp.pitagoras.model;

/**
 * Clase de análisis Estudiante (Tabla 6, pág. 38 y Modelo Lógico/Físico, pág. 43).
 */
public class Estudiante {
    private final int estudianteId;
    private final String dni;
    private final String codigoModular;
    private final String nombresApellidos;
    private final String gradoSeccion;
    private final String apoderadoNombre;
    private final String telefonoApoderado;
    private final String estadoMatricula;

    public Estudiante(int estudianteId, String dni, String codigoModular, String nombresApellidos,
                      String gradoSeccion, String apoderadoNombre, String telefonoApoderado, String estadoMatricula) {
        this.estudianteId = estudianteId;
        this.dni = dni;
        this.codigoModular = codigoModular;
        this.nombresApellidos = nombresApellidos;
        this.gradoSeccion = gradoSeccion;
        this.apoderadoNombre = apoderadoNombre;
        this.telefonoApoderado = telefonoApoderado;
        this.estadoMatricula = estadoMatricula;
    }

    public int getEstudianteId() { return estudianteId; }
    public String getDni() { return dni; }
    public String getCodigoModular() { return codigoModular; }
    public String getNombresApellidos() { return nombresApellidos; }
    public String getGradoSeccion() { return gradoSeccion; }
    public String getApoderadoNombre() { return apoderadoNombre; }
    public String getTelefonoApoderado() { return telefonoApoderado; }
    public String getEstadoMatricula() { return estadoMatricula; }
}
