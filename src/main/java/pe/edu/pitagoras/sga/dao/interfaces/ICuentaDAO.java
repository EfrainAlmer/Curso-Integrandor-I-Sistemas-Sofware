package pe.edu.pitagoras.sga.dao.interfaces;

import java.util.List;
import java.util.Map;
import pe.edu.pitagoras.sga.model.academico.Curso;
import java.util.Optional;
import pe.edu.pitagoras.sga.model.academico.Docente;
import pe.edu.pitagoras.sga.model.gestion.CuentaUsuario;

/** Gestión de cuentas de acceso por la Dirección (RF03). */
public interface ICuentaDAO {

    List<CuentaUsuario> listar();

    List<Docente> listarDocentes();

    boolean existeUsername(String username);

    /** Crea la cuenta, la ficha del docente y sus áreas. @return id de la cuenta. */
    int crearDocente(String username, String hashClave, Docente datos, List<Integer> cursoIds, int usuarioId);

    /** @return usuarioId del docente → áreas que puede dictar (especialidad). */
    Map<Integer, List<Curso>> areasPorUsuario();

    /** @return la ficha del docente dueño de la cuenta, o vacío. */
    Optional<Docente> buscarDocentePorUsuario(int usuarioId);

    /** Modifica DNI, nombres, apellidos, correo y áreas del docente (RF03). */
    void actualizarDocente(Docente datos, List<Integer> cursoIds, int usuarioId);

    /** @return id de la cuenta. */
    int crearDirector(String username, String hashClave, int usuarioId);

    void cambiarEstado(int usuarioIdObjetivo, boolean activo, int usuarioId);

    /** Pone una clave temporal, quita el bloqueo y exige cambiarla al ingresar. */
    void restablecerClave(int usuarioIdObjetivo, String hashClave, int usuarioId);

    /** @return nombre de la persona dueña de la cuenta (docente o apoderado), o vacío. */
    Optional<String> nombreParaMostrar(int usuarioId);
}
