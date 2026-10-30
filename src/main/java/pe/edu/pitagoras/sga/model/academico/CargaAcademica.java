package pe.edu.pitagoras.sga.model.academico;

/**
 * Un área dictada por un docente en una sección (tabla CursoSeccion, RF06).
 *
 * @param cursoSeccionId identificador de la carga.
 * @param curso          área curricular.
 * @param seccion        sección donde se dicta.
 * @param docenteId      docente asignado.
 * @param docenteNombre  "Apellido, Nombre" del docente.
 */
public record CargaAcademica(int cursoSeccionId, Curso curso, Seccion seccion, int docenteId, String docenteNombre) {

    @Override
    public String toString() {
        return curso.nombre() + " - " + seccion.etiqueta();
    }
}
