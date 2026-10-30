package pe.edu.pitagoras.sga.controller;

import com.google.common.base.Preconditions;
import java.io.File;
import java.time.LocalDate;
import java.util.List;
import pe.edu.pitagoras.sga.model.academico.Alumno;
import pe.edu.pitagoras.sga.model.academico.CargaAcademica;
import pe.edu.pitagoras.sga.model.academico.Competencia;
import pe.edu.pitagoras.sga.model.academico.Periodo;
import pe.edu.pitagoras.sga.model.auth.Sesion;
import pe.edu.pitagoras.sga.model.evaluacion.FilaAsistencia;
import pe.edu.pitagoras.sga.model.evaluacion.FilaNotas;
import pe.edu.pitagoras.sga.model.evaluacion.ResumenAlumno;
import pe.edu.pitagoras.sga.service.DocenteService;
import pe.edu.pitagoras.sga.service.ReporteService;

/**
 * Controlador del módulo Docente. Guarda la sesión, así las vistas no la manejan, y
 * delega en los services. Las excepciones (BusinessException, AccesoDatosException)
 * llegan a la vista, que muestra su mensaje.
 */
public class DocenteController {

    private final DocenteService docenteService;
    private final ReporteService reporteService;
    private final Sesion sesion;

    public DocenteController(DocenteService docenteService, ReporteService reporteService, Sesion sesion) {
        this.docenteService = Preconditions.checkNotNull(docenteService);
        this.reporteService = Preconditions.checkNotNull(reporteService);
        this.sesion = Preconditions.checkNotNull(sesion);
    }

    public List<CargaAcademica> misCargas() {
        return docenteService.misCargas(sesion);
    }

    public List<Alumno> alumnos(CargaAcademica carga) {
        return docenteService.alumnosDeCarga(sesion, carga.cursoSeccionId());
    }

    public List<Periodo> periodos() {
        return docenteService.periodos();
    }

    public Periodo periodoActual() {
        return docenteService.periodoActual();
    }

    public List<Competencia> competencias(CargaAcademica carga) {
        return docenteService.competencias(carga.curso().cursoId());
    }

    public List<FilaNotas> notas(CargaAcademica carga, Periodo periodo) {
        return docenteService.notas(sesion, carga.cursoSeccionId(), periodo.periodoId());
    }

    public int guardarNotas(CargaAcademica carga, Periodo periodo, List<FilaNotas> filas) {
        return docenteService.guardarNotas(sesion, carga.cursoSeccionId(), periodo.periodoId(), filas);
    }

    public java.util.Map<String, String> conclusiones(CargaAcademica carga, Periodo periodo, int alumnoId) {
        return docenteService.conclusiones(sesion, carga.cursoSeccionId(), periodo.periodoId(), alumnoId);
    }

    public int guardarConclusiones(CargaAcademica carga, Periodo periodo, int alumnoId,
                                   java.util.Map<String, String> textos) {
        return docenteService.guardarConclusiones(sesion, carga.cursoSeccionId(), periodo.periodoId(), alumnoId, textos);
    }

    public List<FilaAsistencia> asistencia(CargaAcademica carga, LocalDate fecha) {
        return docenteService.asistencia(sesion, carga.cursoSeccionId(), fecha);
    }

    public int guardarAsistencia(CargaAcademica carga, LocalDate fecha, List<FilaAsistencia> filas) {
        return docenteService.guardarAsistencia(sesion, carga.cursoSeccionId(), fecha, filas);
    }

    public List<ResumenAlumno> resumenAsistencia(CargaAcademica carga, Periodo periodo) {
        return reporteService.asistenciaDeCarga(sesion, carga.cursoSeccionId(), periodo.periodoId());
    }

    public File exportarActaPdf(CargaAcademica carga, Periodo periodo, File destino) {
        return reporteService.exportarActaNotasPdf(sesion, carga.cursoSeccionId(), periodo.periodoId(), destino);
    }

    public File exportarActaExcel(CargaAcademica carga, Periodo periodo, File destino) {
        return reporteService.exportarActaNotasExcel(sesion, carga.cursoSeccionId(), periodo.periodoId(), destino);
    }

    public File exportarAsistenciaPdf(CargaAcademica carga, Periodo periodo, File destino) {
        return reporteService.exportarAsistenciaPdf(sesion, carga.cursoSeccionId(), periodo.periodoId(), destino);
    }

    public File exportarNominaPdf(CargaAcademica carga, File destino) {
        return reporteService.exportarNominaPdf(sesion, carga.seccion().seccionId(), destino);
    }
}
