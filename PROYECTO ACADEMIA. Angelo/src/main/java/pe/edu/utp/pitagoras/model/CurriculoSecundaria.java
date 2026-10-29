package pe.edu.utp.pitagoras.model;

import java.util.List;

/**
 * Currículo de Secundaria usado en la libreta: 11 áreas y 28 competencias, en el orden del
 * Informe de Progreso del Aprendizaje del Estudiante. Mientras no exista el DAO de
 * configuración académica (fase 2), esta clase es la fuente única de áreas y competencias.
 */
public final class CurriculoSecundaria {

    /** El colegio trabaja con un solo turno. */
    public static final String TURNO = "Mañana";

    public static final List<AreaCurricular> AREAS = List.of(
        new AreaCurricular("Desarrollo Personal, Ciudadanía y Cívica", "DPCC", List.of(
            "Construye su identidad",
            "Convive y participa democráticamente en la búsqueda del bien común")),
        new AreaCurricular("Ciencias Sociales", "C. Sociales", List.of(
            "Construye interpretaciones históricas",
            "Gestiona responsablemente el espacio y el ambiente",
            "Gestiona responsablemente los recursos económicos")),
        new AreaCurricular("Educación Física", "Ed. Física", List.of(
            "Se desenvuelve de manera autónoma a través de su motricidad",
            "Asume una vida saludable",
            "Interactúa a través de sus habilidades sociomotrices")),
        new AreaCurricular("Comunicación", "Comunicación", List.of(
            "Se comunica oralmente en su lengua materna",
            "Lee diversos tipos de textos escritos en su lengua materna",
            "Escribe diversos tipos de textos en su lengua materna")),
        new AreaCurricular("Arte y Cultura", "Arte y Cultura", List.of(
            "Aprecia de manera crítica manifestaciones artístico-culturales",
            "Crea proyectos desde los lenguajes artísticos")),
        new AreaCurricular("Inglés", "Inglés", List.of(
            "Se comunica oralmente en inglés como lengua extranjera",
            "Lee diversos tipos de textos escritos en inglés",
            "Escribe diversos tipos de textos en inglés")),
        new AreaCurricular("Matemática", "Matemática", List.of(
            "Resuelve problemas de cantidad",
            "Resuelve problemas de regularidad, equivalencia y cambio",
            "Resuelve problemas de forma, movimiento y localización",
            "Resuelve problemas de gestión de datos e incertidumbre")),
        new AreaCurricular("Ciencia y Tecnología", "Ciencia y Tec.", List.of(
            "Indaga mediante métodos científicos para construir conocimientos",
            "Explica el mundo físico basándose en conocimientos sobre los seres vivos, materia y energía, biodiversidad, Tierra y universo",
            "Diseña y construye soluciones tecnológicas para resolver problemas de su entorno")),
        new AreaCurricular("Educación Religiosa", "Ed. Religiosa", List.of(
            "Construye su identidad como persona humana, amada por Dios, digna, libre y trascendente",
            "Asume la experiencia del encuentro personal y comunitario con Dios")),
        new AreaCurricular("Educación para el Trabajo", "Ed. Trabajo", List.of(
            "Gestiona proyectos de emprendimiento económico o social")),
        new AreaCurricular("Competencias transversales", "Comp. Transversales", List.of(
            "Se desenvuelve en entornos virtuales generados por las TIC",
            "Gestiona su aprendizaje de manera autónoma"))
    );

    private CurriculoSecundaria() { }

    public static int totalCompetencias() {
        int total = 0;
        for (AreaCurricular a : AREAS) total += a.competencias().size();
        return total;
    }
}
