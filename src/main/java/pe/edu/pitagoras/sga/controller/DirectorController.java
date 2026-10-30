package pe.edu.pitagoras.sga.controller;

import com.google.common.base.Preconditions;
import java.io.File;
import java.time.LocalDate;
import java.util.List;
import pe.edu.pitagoras.sga.model.academico.Alumno;
import pe.edu.pitagoras.sga.model.academico.CargaAcademica;
import pe.edu.pitagoras.sga.model.academico.Competencia;
import pe.edu.pitagoras.sga.model.academico.Curso;
import pe.edu.pitagoras.sga.model.academico.Docente;
import pe.edu.pitagoras.sga.model.academico.Grado;
import pe.edu.pitagoras.sga.model.academico.Periodo;
import pe.edu.pitagoras.sga.model.academico.Seccion;
import pe.edu.pitagoras.sga.model.auth.Sesion;
import pe.edu.pitagoras.sga.model.evaluacion.Consolidado;
import pe.edu.pitagoras.sga.model.evaluacion.Libreta;
import pe.edu.pitagoras.sga.model.gestion.CopiaSeguridad;
import pe.edu.pitagoras.sga.model.gestion.Credencial;
import pe.edu.pitagoras.sga.model.gestion.CuentaUsuario;
import pe.edu.pitagoras.sga.model.gestion.EventoAuditoria;
import pe.edu.pitagoras.sga.model.gestion.Indicadores;
import pe.edu.pitagoras.sga.service.AcademicoService;
import pe.edu.pitagoras.sga.service.AlumnoService;
import pe.edu.pitagoras.sga.service.AuditoriaService;
import pe.edu.pitagoras.sga.service.ReporteService;
import pe.edu.pitagoras.sga.service.UsuarioService;

/**
 * Controlador del módulo Dirección/Coordinación: usuarios, inscripción, estructura académica,
 * carga, periodos, indicadores, reportes, auditoría y respaldo.
 */
public class DirectorController {

    private final UsuarioService usuarioService;
    private final AlumnoService alumnoService;
    private final AcademicoService academicoService;
    private final ReporteService reporteService;
    private final AuditoriaService auditoriaService;
    private final Sesion sesion;

    public DirectorController(UsuarioService usuarioService, AlumnoService alumnoService,
                              AcademicoService academicoService, ReporteService reporteService,
                              AuditoriaService auditoriaService, Sesion sesion) {
        this.usuarioService = Preconditions.checkNotNull(usuarioService);
        this.alumnoService = Preconditions.checkNotNull(alumnoService);
        this.academicoService = Preconditions.checkNotNull(academicoService);
        this.reporteService = Preconditions.checkNotNull(reporteService);
        this.auditoriaService = Preconditions.checkNotNull(auditoriaService);
        this.sesion = Preconditions.checkNotNull(sesion);
    }

    // ----- Panel (RF08) -----
    public Indicadores indicadores(Periodo periodo) {
        return reporteService.indicadores(sesion, periodo.periodoId());
    }

    // ----- Usuarios (RF03) -----
    public List<CuentaUsuario> usuarios() {
        return usuarioService.listar(sesion);
    }

    public List<Docente> docentes() {
        return usuarioService.docentes(sesion);
    }

    public Credencial crearDocente(String usuario, String dni, String nombre, String apellido, String email,
                                   List<Curso> areas) {
        return usuarioService.crearDocente(sesion, usuario, dni, nombre, apellido, email, areas);
    }

    /** @return usuarioId del docente → áreas que puede dictar. */
    public java.util.Map<Integer, List<Curso>> areasDocentes() {
        return usuarioService.areasDocentes(sesion);
    }

    public Docente docenteDeCuenta(CuentaUsuario cuenta) {
        return usuarioService.docenteDeCuenta(sesion, cuenta);
    }

    public void actualizarDocente(Docente actual, String dni, String nombre, String apellido, String email,
                                  List<Curso> areas) {
        usuarioService.actualizarDocente(sesion, actual, dni, nombre, apellido, email, areas);
    }

    public Credencial crearDirector(String usuario) {
        return usuarioService.crearDirector(sesion, usuario);
    }

    public void cambiarEstado(CuentaUsuario cuenta, boolean activo) {
        usuarioService.cambiarEstado(sesion, cuenta, activo);
    }

    public Credencial restablecerClave(CuentaUsuario cuenta) {
        return usuarioService.restablecerClave(sesion, cuenta);
    }

    // ----- Inscripción (RF04) -----
    public List<Alumno> alumnos(Seccion seccion, boolean soloActivos) {
        return alumnoService.listar(sesion, seccion == null ? null : seccion.seccionId(), soloActivos);
    }

    public Credencial inscribir(Seccion seccion, String dni, String nombre, String apellido, String apoderado,
                                String telefono) {
        return alumnoService.inscribir(sesion, seccion, dni, nombre, apellido, apoderado, telefono);
    }

    public void actualizarAlumno(Alumno alumno, Seccion seccion, String dni, String nombre, String apellido,
                                 String apoderado, String telefono) {
        alumnoService.actualizar(sesion, alumno.alumnoId(), seccion, dni, nombre, apellido, apoderado, telefono);
    }

    public void cambiarEstado(Alumno alumno, boolean activo) {
        alumnoService.cambiarEstado(sesion, alumno, activo);
    }

    // ----- Configuración académica (RF05) -----
    public List<Grado> grados() {
        return academicoService.grados();
    }

    public List<Seccion> secciones() {
        return academicoService.secciones();
    }

    public List<String> turnos() {
        return academicoService.turnos();
    }

    public List<Curso> cursos() {
        return academicoService.cursos();
    }

    public List<Competencia> competencias(Curso curso) {
        return academicoService.competencias(curso.cursoId());
    }

    public void crearSeccion(Grado grado, String letra, String turno) {
        academicoService.crearSeccion(sesion, grado, letra, turno);
    }

    public void crearCurso(String nombre, List<String> competencias) {
        academicoService.crearCurso(sesion, nombre, competencias);
    }

    // ----- Carga académica (RF06) -----
    public List<CargaAcademica> cargas() {
        return academicoService.cargas(sesion);
    }

    public void asignarCarga(Curso curso, Seccion seccion, Docente docente) {
        academicoService.asignarCarga(sesion, curso, seccion, docente);
    }

    // ----- Periodos (RF07) -----
    public List<Periodo> periodos() {
        return academicoService.periodos();
    }

    public void actualizarFechas(Periodo periodo, LocalDate inicio, LocalDate fin) {
        academicoService.actualizarFechas(sesion, periodo, inicio, fin);
    }

    public int cerrarPeriodo(Periodo periodo) {
        return academicoService.cerrarPeriodo(sesion, periodo);
    }

    public void reabrirPeriodo(Periodo periodo, String motivo) {
        academicoService.reabrirPeriodo(sesion, periodo, motivo);
    }

    // ----- Reportes (RF17, RF18, RF19) -----
    public Libreta libreta(Alumno alumno) {
        return reporteService.libreta(sesion, alumno.alumnoId());
    }

    public Consolidado consolidado(Seccion seccion, Periodo periodo) {
        return reporteService.consolidado(sesion, seccion.seccionId(), periodo.periodoId());
    }

    public File exportarLibretaPdf(Alumno alumno, File destino) {
        return reporteService.exportarLibretaPdf(sesion, alumno.alumnoId(), destino);
    }

    public File exportarNominaPdf(Seccion seccion, File destino) {
        return reporteService.exportarNominaPdf(sesion, seccion.seccionId(), destino);
    }

    /** @param seccion sección, o null para todo el colegio. */
    public File exportarNominaExcel(Seccion seccion, File destino) {
        return reporteService.exportarNominaExcel(sesion, seccion == null ? null : seccion.seccionId(), destino);
    }

    public File exportarConsolidadoPdf(Seccion seccion, Periodo periodo, File destino) {
        return reporteService.exportarConsolidadoPdf(sesion, seccion.seccionId(), periodo.periodoId(), destino);
    }

    public File exportarConsolidadoExcel(Seccion seccion, Periodo periodo, File destino) {
        return reporteService.exportarConsolidadoExcel(sesion, seccion.seccionId(), periodo.periodoId(), destino);
    }

    // ----- Auditoría y respaldo (RF09, RF10) -----
    public List<EventoAuditoria> eventos(LocalDate desde, LocalDate hasta, String accion) {
        return auditoriaService.eventos(sesion, desde, hasta, accion);
    }

    public List<CopiaSeguridad> copias() {
        return auditoriaService.copias(sesion);
    }

    public CopiaSeguridad generarCopia() {
        return auditoriaService.generarCopia(sesion);
    }

    public void restaurar(CopiaSeguridad copia) {
        auditoriaService.restaurar(sesion, copia);
    }
}
