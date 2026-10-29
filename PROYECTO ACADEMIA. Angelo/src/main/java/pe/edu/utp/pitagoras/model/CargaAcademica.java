package pe.edu.utp.pitagoras.model;

/**
 * Clase de análisis CargaAcadémica (Tabla 6, pág. 38 y Modelo Lógico, pág. 43).
 * Representa la asignación de un docente a un grado, sección y área curricular en un año escolar.
 * <p>
 * El vínculo con el docente es su {@code docenteUsername}: es lo que permite que la carga que
 * asigna el Director aparezca en la sesión de ese docente.
 */
public class CargaAcademica {
    private final String codigoCarga;
    private final int anioEscolar;
    private final String grado;
    private final String seccion;
    private final String turno;
    private final String areaCurricular;
    private final String docenteUsername;
    private final String docenteNombre;
    private final String[] competencias;

    public CargaAcademica(String codigoCarga, int anioEscolar, String grado, String seccion,
                          String turno, String areaCurricular, String docenteUsername,
                          String docenteNombre, String[] competencias) {
        this.codigoCarga = codigoCarga;
        this.anioEscolar = anioEscolar;
        this.grado = grado;
        this.seccion = seccion;
        this.turno = turno;
        this.areaCurricular = areaCurricular;
        this.docenteUsername = docenteUsername;
        this.docenteNombre = docenteNombre;
        this.competencias = competencias;
    }

    /** Constructor anterior, solo para que el proyecto compile hasta el paso 1.3. Se elimina luego. */
    @Deprecated
    public CargaAcademica(String codigoCarga, int anioEscolar, String grado, String seccion,
                          String turno, String areaCurricular, String docenteNombre, String[] competencias) {
        this(codigoCarga, anioEscolar, grado, seccion, turno, areaCurricular, null, docenteNombre, competencias);
    }

    public String getCodigoCarga() { return codigoCarga; }
    public int getAnioEscolar() { return anioEscolar; }
    public String getGrado() { return grado; }
    public String getSeccion() { return seccion; }
    public String getGradoSeccionCorto() { return grado + " " + seccion; }
    public String getTurno() { return turno; }
    public String getAreaCurricular() { return areaCurricular; }
    public String getDocenteUsername() { return docenteUsername; }
    public String getDocenteNombre() { return docenteNombre; }
    public String[] getCompetencias() { return competencias; }
}
