package pe.edu.utp.pitagoras.model;

import java.util.Arrays;

/**
 * Clase de análisis Calificación y Calificativo (Tabla 6, pág. 38 - RF13, RF14 / CU13, CU14).
 * Soporta áreas curriculares de 2, 3 o 4 competencias según el Informe de Progreso del Aprendizaje
 * del Estudiante 2026 (SIAGIE / MINEDU - RVM 094-2020-MINEDU) y conclusiones descriptivas por periodo.
 */
public class CalificacionCompetencia {
    private final int estudianteId;
    private final String estudianteNombre;
    private final String bimestre;
    private final String seccion;
    private final String area;
    private final String[] calificativos; // Escala literal MINEDU: AD, A, B, C por cada competencia del área
    private String conclusionDescriptiva;

    public CalificacionCompetencia(int estudianteId, String estudianteNombre, String bimestre,
                                   String seccion, String area, String[] calificativos) {
        this(estudianteId, estudianteNombre, bimestre, seccion, area, calificativos, "");
    }

    public CalificacionCompetencia(int estudianteId, String estudianteNombre, String bimestre,
                                   String seccion, String area, String[] calificativos, String conclusionDescriptiva) {
        this.estudianteId = estudianteId;
        this.estudianteNombre = estudianteNombre;
        this.bimestre = bimestre;
        this.seccion = seccion;
        this.area = area;
        this.calificativos = Arrays.copyOf(calificativos, calificativos.length);
        this.conclusionDescriptiva = conclusionDescriptiva != null ? conclusionDescriptiva : "";
    }

    public CalificacionCompetencia copiar() {
        return new CalificacionCompetencia(
                estudianteId, estudianteNombre, bimestre, seccion, area, calificativos, conclusionDescriptiva);
    }

    public int getEstudianteId() { return estudianteId; }
    public String getEstudianteNombre() { return estudianteNombre; }
    public String getBimestre() { return bimestre; }
    public String getSeccion() { return seccion; }
    public String getArea() { return area; }
    public int getNumeroCompetencias() { return calificativos.length; }

    public String getCalificativo(int indiceCompetencia) {
        if (indiceCompetencia >= 0 && indiceCompetencia < calificativos.length) {
            return calificativos[indiceCompetencia];
        }
        return "A";
    }

    public void setCalificativo(int indiceCompetencia, String valor) {
        if (indiceCompetencia >= 0 && indiceCompetencia < calificativos.length
                && ReglaCalificativo.esCalificativoValido(valor)) {
            calificativos[indiceCompetencia] = valor;
        }
    }

    public String[] getCalificativos() {
        return Arrays.copyOf(calificativos, calificativos.length);
    }

    public String getConclusionDescriptiva() {
        return conclusionDescriptiva;
    }

    public void setConclusionDescriptiva(String conclusionDescriptiva) {
        this.conclusionDescriptiva = conclusionDescriptiva != null ? conclusionDescriptiva : "";
    }
}
