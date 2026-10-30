package pe.edu.pitagoras.sga.service;

import com.google.common.base.Preconditions;
import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import pe.edu.pitagoras.sga.dao.interfaces.IAlumnoDAO;
import pe.edu.pitagoras.sga.dao.interfaces.IAsistenciaDAO;
import pe.edu.pitagoras.sga.dao.interfaces.ICargaAcademicaDAO;
import pe.edu.pitagoras.sga.dao.interfaces.IConclusionDAO;
import pe.edu.pitagoras.sga.dao.interfaces.IEstructuraDAO;
import pe.edu.pitagoras.sga.dao.interfaces.INotaDAO;
import pe.edu.pitagoras.sga.dao.interfaces.IPeriodoDAO;
import pe.edu.pitagoras.sga.exception.BusinessException;
import pe.edu.pitagoras.sga.model.academico.Alumno;
import pe.edu.pitagoras.sga.model.academico.CargaAcademica;
import pe.edu.pitagoras.sga.model.academico.Competencia;
import pe.edu.pitagoras.sga.model.academico.Periodo;
import pe.edu.pitagoras.sga.model.auth.Rol;
import pe.edu.pitagoras.sga.model.auth.Sesion;
import pe.edu.pitagoras.sga.model.evaluacion.AsistenciaRegistro;
import pe.edu.pitagoras.sga.model.evaluacion.Calificativo;
import pe.edu.pitagoras.sga.model.evaluacion.FilaConsultaNota;
import pe.edu.pitagoras.sga.model.evaluacion.FiltroAsistencia;
import pe.edu.pitagoras.sga.model.evaluacion.FiltroNotas;
import pe.edu.pitagoras.sga.model.evaluacion.Libreta;
import pe.edu.pitagoras.sga.model.evaluacion.NotaRegistro;
import pe.edu.pitagoras.sga.model.evaluacion.ReglaCalificativo;

/**
 * Consultas de solo lectura del Alumno/Apoderado (RF15, RF16, RF17). El alumno siempre
 * se toma de la sesión, nunca de un parámetro: así un apoderado no puede ver a otro estudiante (RNF03).
 */
public class ApoderadoService {

    private final IAlumnoDAO alumnoDAO;
    private final INotaDAO notaDAO;
    private final IAsistenciaDAO asistenciaDAO;
    private final IPeriodoDAO periodoDAO;
    private final ICargaAcademicaDAO cargaDAO;
    private final IEstructuraDAO estructuraDAO;
    private final ReporteService reporteService;
    private final IConclusionDAO conclusionDAO;

    public ApoderadoService(IAlumnoDAO alumnoDAO, INotaDAO notaDAO, IAsistenciaDAO asistenciaDAO,
                            IPeriodoDAO periodoDAO, ICargaAcademicaDAO cargaDAO, IEstructuraDAO estructuraDAO,
                            IConclusionDAO conclusionDAO, ReporteService reporteService) {
        this.conclusionDAO = Preconditions.checkNotNull(conclusionDAO);
        this.alumnoDAO = Preconditions.checkNotNull(alumnoDAO);
        this.notaDAO = Preconditions.checkNotNull(notaDAO);
        this.asistenciaDAO = Preconditions.checkNotNull(asistenciaDAO);
        this.periodoDAO = Preconditions.checkNotNull(periodoDAO);
        this.cargaDAO = Preconditions.checkNotNull(cargaDAO);
        this.estructuraDAO = Preconditions.checkNotNull(estructuraDAO);
        this.reporteService = Preconditions.checkNotNull(reporteService);
    }

    /** @return el alumno vinculado a la sesión. */
    public Alumno miAlumno(Sesion sesion) {
        return alumnoDAO.buscar(alumnoId(sesion))
                .orElseThrow(() -> new BusinessException("No se encontró al estudiante."));
    }

    public List<Periodo> periodos(Sesion sesion) {
        Permisos.exigir(sesion, Rol.APODERADO);
        return periodoDAO.listar();
    }

    /**
     * RF15: calificativos de cada competencia y del área en un periodo.
     *
     * @return por cada área, una fila de resumen seguida de sus competencias.
     */
    public List<FilaConsultaNota> notas(Sesion sesion, int periodoId) {
        Alumno alumno = miAlumno(sesion);
        Map<String, Calificativo> registradas = new LinkedHashMap<>();
        for (NotaRegistro n : notaDAO.listar(new FiltroNotas(periodoId, null, null, alumno.alumnoId()))) {
            registradas.put(n.cursoId() + "|" + n.codigoCompetencia(), n.calificativo());
        }
        Map<String, String> conclusiones = new LinkedHashMap<>();
        conclusionDAO.listar(new FiltroNotas(periodoId, null, null, alumno.alumnoId()))
                .forEach(c -> conclusiones.put(c.cursoId() + "|" + c.codigoCompetencia(), c.texto()));
        List<FilaConsultaNota> filas = new ArrayList<>();
        for (CargaAcademica carga : cargaDAO.listar()) {
            if (carga.seccion().seccionId() != alumno.seccion().seccionId()) {
                continue;
            }
            List<FilaConsultaNota> competencias = new ArrayList<>();
            List<Calificativo> valores = new ArrayList<>();
            for (Competencia c : estructuraDAO.listarCompetencias(carga.curso().cursoId())) {
                Calificativo cal = registradas.get(c.cursoId() + "|" + c.codigo());
                if (cal != null) {
                    valores.add(cal);
                }
                competencias.add(new FilaConsultaNota(carga.curso().nombre(), c.nombre(), cal, false,
                        conclusiones.get(c.cursoId() + "|" + c.codigo())));
            }
            filas.add(new FilaConsultaNota(carga.curso().nombre(), "Calificativo del área",
                    ReglaCalificativo.calificativoArea(valores).orElse(null), true, null));
            filas.addAll(competencias);
        }
        return filas;
    }

    /** RF16: marcas de asistencia del periodo (o del año si periodo es null), de la más reciente a la más antigua. */
    public List<AsistenciaRegistro> asistencia(Sesion sesion, Integer periodoId) {
        Alumno alumno = miAlumno(sesion);
        List<Periodo> periodos = periodoDAO.listar();
        if (periodos.isEmpty()) {
            return List.of();
        }
        FiltroAsistencia filtro;
        if (periodoId == null) {
            filtro = FiltroAsistencia.deAlumno(alumno.alumnoId(), periodos.get(0).fechaInicio(),
                    periodos.get(periodos.size() - 1).fechaFin());
        } else {
            Periodo p = periodoDAO.buscar(periodoId).orElseThrow(() -> new BusinessException("El periodo no existe."));
            filtro = FiltroAsistencia.deAlumno(alumno.alumnoId(), p.fechaInicio(), p.fechaFin());
        }
        List<AsistenciaRegistro> lista = new ArrayList<>(asistenciaDAO.listar(filtro));
        java.util.Collections.reverse(lista);
        return lista;
    }

    /** RF17: libreta del año. */
    public Libreta libreta(Sesion sesion) {
        return reporteService.libreta(sesion, alumnoId(sesion));
    }

    /** RF17: libreta en PDF para imprimir. */
    public File exportarLibreta(Sesion sesion, File destino) {
        return reporteService.exportarLibretaPdf(sesion, alumnoId(sesion), destino);
    }

    private static int alumnoId(Sesion sesion) {
        Permisos.exigir(sesion, Rol.APODERADO);
        return sesion.getAlumnoId();
    }
}
