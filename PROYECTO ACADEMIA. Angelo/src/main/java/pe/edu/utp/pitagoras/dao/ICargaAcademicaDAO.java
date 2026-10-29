package pe.edu.utp.pitagoras.dao;

import pe.edu.utp.pitagoras.model.CargaAcademica;

import java.util.List;

/**
 * Contrato de persistencia de la carga académica (RF06 / RF11).
 * <p>
 * Es la única fuente de datos de la carga: el panel del Director la escribe y el módulo del
 * Docente la lee. Hoy la implementa {@link CargaAcademicaDAOMemoria}; luego una clase con SQL
 * Server la reemplaza sin tocar vistas ni controladores (principio D de SOLID).
 */
public interface ICargaAcademicaDAO {

    /** @return todas las asignaciones del año escolar, en orden de registro */
    List<CargaAcademica> listarTodas();

    /** @return las asignaciones de un docente (RF11); lista vacía si no tiene carga */
    List<CargaAcademica> listarPorDocente(String docenteUsername);

    /** Registra una nueva asignación (RF06). */
    void guardar(CargaAcademica carga);

    /** @return {@code true} si existía y fue eliminada */
    boolean eliminar(String codigoCarga);

    /** @return el siguiente código libre, por ejemplo {@code CA-2026-21} */
    String siguienteCodigo();
}
