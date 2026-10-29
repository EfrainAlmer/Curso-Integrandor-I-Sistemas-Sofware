package pe.edu.utp.pitagoras.model;

import java.util.List;

/**
 * Área curricular del Informe de Progreso del Aprendizaje (SIAGIE / MINEDU) con sus competencias.
 *
 * @param nombre      nombre completo, como aparece en la libreta
 * @param nombreCorto nombre abreviado para columnas de reportes y planillas
 * @param competencias competencias del área, en el orden de la libreta
 */
public record AreaCurricular(String nombre, String nombreCorto, List<String> competencias) {
    public AreaCurricular {
        competencias = List.copyOf(competencias);
    }
}
