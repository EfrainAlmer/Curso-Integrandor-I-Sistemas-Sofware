package pe.edu.pitagoras.sga.dao.interfaces;

import java.util.List;
import java.util.Optional;
import pe.edu.pitagoras.sga.model.academico.Alumno;

/** Estudiantes e inscripción (RF04). */
public interface IAlumnoDAO {

    /**
     * @param seccionId   sección, o null para todas.
     * @param soloActivos true para omitir a los retirados.
     * @return alumnos ordenados por sección y apellido.
     */
    List<Alumno> listar(Integer seccionId, boolean soloActivos);

    Optional<Alumno> buscar(int alumnoId);

    /** @return true si otro alumno (distinto de excluirAlumnoId) ya tiene ese DNI. */
    boolean existeDni(String dni, int excluirAlumnoId);

    /**
     * Crea la cuenta del apoderado y el alumno en una sola transacción.
     *
     * @return id del alumno.
     */
    int inscribir(Alumno alumno, String usernameApoderado, String hashClave, int usuarioId);

    void actualizar(Alumno alumno, int usuarioId);

    /** Cambia el estado (ACTIVO o RETIRADO) y desactiva o activa la cuenta del apoderado. */
    void cambiarEstado(int alumnoId, String estado, int usuarioId);
}
