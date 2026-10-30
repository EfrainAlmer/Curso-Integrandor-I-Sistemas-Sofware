package pe.edu.pitagoras.sga.service;

import com.google.common.base.Preconditions;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pe.edu.pitagoras.sga.dao.interfaces.IAlumnoDAO;
import pe.edu.pitagoras.sga.dao.interfaces.IAsistenciaDAO;
import pe.edu.pitagoras.sga.dao.interfaces.ICargaAcademicaDAO;
import pe.edu.pitagoras.sga.dao.interfaces.IConclusionDAO;
import pe.edu.pitagoras.sga.dao.interfaces.IEstructuraDAO;
import pe.edu.pitagoras.sga.dao.interfaces.INotaDAO;
import pe.edu.pitagoras.sga.dao.interfaces.IPeriodoDAO;
import pe.edu.pitagoras.sga.exception.AccesoDenegadoException;
import pe.edu.pitagoras.sga.exception.BusinessException;
import pe.edu.pitagoras.sga.model.academico.Alumno;
import pe.edu.pitagoras.sga.model.academico.CargaAcademica;
import pe.edu.pitagoras.sga.model.academico.Competencia;
import pe.edu.pitagoras.sga.model.academico.Periodo;
import pe.edu.pitagoras.sga.model.auth.Rol;
import pe.edu.pitagoras.sga.model.auth.Sesion;
import pe.edu.pitagoras.sga.model.evaluacion.AsistenciaRegistro;
import pe.edu.pitagoras.sga.model.evaluacion.CambioNota;
import pe.edu.pitagoras.sga.model.evaluacion.EstadoAsistencia;
import pe.edu.pitagoras.sga.model.evaluacion.FilaAsistencia;
import pe.edu.pitagoras.sga.model.evaluacion.FilaNotas;
import pe.edu.pitagoras.sga.model.evaluacion.FiltroAsistencia;
import pe.edu.pitagoras.sga.model.evaluacion.FiltroNotas;
import pe.edu.pitagoras.sga.model.evaluacion.MarcaAsistencia;

/**
 * Reglas del módulo Docente: consultar su carga (RF11), registrar asistencia (RF12) y
 * calificativos (RF13). Un docente solo puede tocar sus propias cargas y solo en
 * periodos abiertos (RF07).
 */
public class DocenteService {

    private static final Logger LOG = LoggerFactory.getLogger(DocenteService.class);
    static final int MAX_CONCLUSION = 500;

    private final ICargaAcademicaDAO cargaDAO;
    private final IAlumnoDAO alumnoDAO;
    private final IPeriodoDAO periodoDAO;
    private final IEstructuraDAO estructuraDAO;
    private final INotaDAO notaDAO;
    private final IAsistenciaDAO asistenciaDAO;
    private final Clock reloj;
    private final IConclusionDAO conclusionDAO;

    public DocenteService(ICargaAcademicaDAO cargaDAO, IAlumnoDAO alumnoDAO, IPeriodoDAO periodoDAO,
                          IEstructuraDAO estructuraDAO, INotaDAO notaDAO, IAsistenciaDAO asistenciaDAO,
                          IConclusionDAO conclusionDAO, Clock reloj) {
        this.conclusionDAO = Preconditions.checkNotNull(conclusionDAO);
        this.cargaDAO = Preconditions.checkNotNull(cargaDAO);
        this.alumnoDAO = Preconditions.checkNotNull(alumnoDAO);
        this.periodoDAO = Preconditions.checkNotNull(periodoDAO);
        this.estructuraDAO = Preconditions.checkNotNull(estructuraDAO);
        this.notaDAO = Preconditions.checkNotNull(notaDAO);
        this.asistenciaDAO = Preconditions.checkNotNull(asistenciaDAO);
        this.reloj = Preconditions.checkNotNull(reloj);
    }

    /** RF11: áreas y secciones asignadas al docente de la sesión. */
    public List<CargaAcademica> misCargas(Sesion sesion) {
        return cargaDAO.listarPorDocente(docenteId(sesion));
    }

    /** RF11: alumnos activos de la sección de una carga propia. */
    public List<Alumno> alumnosDeCarga(Sesion sesion, int cargaId) {
        CargaAcademica carga = cargaPropia(sesion, cargaId);
        return alumnoDAO.listar(carga.seccion().seccionId(), true);
    }

    public List<Periodo> periodos() {
        return periodoDAO.listar();
    }

    /** @return el periodo que contiene la fecha de hoy; si no hay, el último abierto o el último. */
    public Periodo periodoActual() {
        List<Periodo> periodos = periodoDAO.listar();
        if (periodos.isEmpty()) {
            throw new BusinessException("No hay periodos configurados. Comuníquese con Dirección.");
        }
        LocalDate hoy = LocalDate.now(reloj);
        return periodos.stream().filter(p -> p.contiene(hoy)).findFirst()
                .or(() -> periodos.stream().filter(p -> !p.cerrado()).reduce((a, b) -> b))
                .orElse(periodos.get(periodos.size() - 1));
    }

    public List<Competencia> competencias(int cursoId) {
        return estructuraDAO.listarCompetencias(cursoId);
    }

    /** RF13: grilla de calificativos de una carga propia en un periodo. */
    public List<FilaNotas> notas(Sesion sesion, int cargaId, int periodoId) {
        CargaAcademica carga = cargaPropia(sesion, cargaId);
        return CalculoAcademico.filasNotas(alumnoDAO.listar(carga.seccion().seccionId(), true),
                notaDAO.listar(FiltroNotas.deCarga(cargaId, periodoId)));
    }

    /**
     * RF13: guarda en lote los calificativos modificados.
     *
     * @return cantidad de calificativos guardados.
     * @throws BusinessException       si el periodo está cerrado o no hay cambios.
     * @throws AccesoDenegadoException si la carga no es del docente.
     */
    public int guardarNotas(Sesion sesion, int cargaId, int periodoId, List<FilaNotas> filas) {
        cargaPropia(sesion, cargaId);
        exigirPeriodoAbierto(periodoId);
        List<CambioNota> cambios = new ArrayList<>();
        for (FilaNotas f : filas) {
            if (f.tieneCambios()) {
                f.getValores().forEach((codigo, valor) -> cambios.add(new CambioNota(f.getAlumnoId(), codigo, valor)));
            }
        }
        if (cambios.isEmpty()) {
            throw new BusinessException("No hay cambios por guardar.");
        }
        int guardados = notaDAO.guardar(cargaId, periodoId, cambios, sesion.getUsuarioId());
        filas.forEach(FilaNotas::confirmar);
        LOG.info("Calificativos guardados: carga={}, periodo={}, cambios={}", cargaId, periodoId, guardados);
        return guardados;
    }

    /** @return código de competencia → conclusión descriptiva del alumno en la carga y periodo. */
    public Map<String, String> conclusiones(Sesion sesion, int cargaId, int periodoId, int alumnoId) {
        cargaPropia(sesion, cargaId);
        Map<String, String> textos = new java.util.LinkedHashMap<>();
        conclusionDAO.listar(new FiltroNotas(periodoId, null, cargaId, alumnoId))
                .forEach(c -> textos.put(c.codigoCompetencia(), c.texto()));
        return textos;
    }

    /**
     * Guarda las conclusiones descriptivas de un alumno (logros, dificultades y recomendaciones).
     *
     * @return cantidad de conclusiones que cambiaron.
     */
    public int guardarConclusiones(Sesion sesion, int cargaId, int periodoId, int alumnoId, Map<String, String> textos) {
        cargaPropia(sesion, cargaId);
        exigirPeriodoAbierto(periodoId);
        for (String t : textos.values()) {
            if (t != null && t.trim().length() > MAX_CONCLUSION) {
                throw new BusinessException("Cada conclusión admite como máximo " + MAX_CONCLUSION + " caracteres.");
            }
        }
        return conclusionDAO.guardar(cargaId, periodoId, alumnoId, textos, sesion.getUsuarioId());
    }

    /** RF12: lista del día. Quien no tiene marca aparece como "Asistió" (sin guardar). */
    public List<FilaAsistencia> asistencia(Sesion sesion, int cargaId, LocalDate fecha) {
        CargaAcademica carga = cargaPropia(sesion, cargaId);
        Map<Integer, AsistenciaRegistro> marcas = new HashMap<>();
        for (AsistenciaRegistro r : asistenciaDAO.listar(FiltroAsistencia.deCargaEnFecha(cargaId, fecha))) {
            marcas.put(r.alumnoId(), r);
        }
        List<FilaAsistencia> filas = new ArrayList<>();
        for (Alumno a : alumnoDAO.listar(carga.seccion().seccionId(), true)) {
            AsistenciaRegistro r = marcas.get(a.alumnoId());
            filas.add(r == null
                    ? new FilaAsistencia(a.alumnoId(), a.dni(), a.nombreCompleto(), EstadoAsistencia.ASISTIO, null, false)
                    : new FilaAsistencia(a.alumnoId(), a.dni(), a.nombreCompleto(), r.estado(), r.observacion(), true));
        }
        return filas;
    }

    /**
     * RF12: guarda la asistencia del día en lote.
     *
     * @throws BusinessException si la fecha es futura, está fuera de los bimestres o en uno cerrado.
     */
    public int guardarAsistencia(Sesion sesion, int cargaId, LocalDate fecha, List<FilaAsistencia> filas) {
        cargaPropia(sesion, cargaId);
        Preconditions.checkNotNull(fecha, "fecha");
        if (fecha.isAfter(LocalDate.now(reloj))) {
            throw new BusinessException("No se puede registrar asistencia de una fecha futura.");
        }
        Periodo periodo = periodoDAO.listar().stream().filter(p -> p.contiene(fecha)).findFirst()
                .orElseThrow(() -> new BusinessException("La fecha no pertenece a ningún bimestre."));
        if (periodo.cerrado()) {
            throw new BusinessException("El " + periodo.etiqueta() + " está cerrado: no se puede modificar la asistencia.");
        }
        List<MarcaAsistencia> marcas = filas.stream().map(FilaAsistencia::aMarca).toList();
        if (marcas.isEmpty()) {
            throw new BusinessException("La sección no tiene alumnos activos.");
        }
        int guardadas = asistenciaDAO.guardar(cargaId, fecha, marcas, sesion.getUsuarioId());
        LOG.info("Asistencia guardada: carga={}, fecha={}, cambios={}", cargaId, fecha, guardadas);
        return guardadas;
    }

    /** @return la carga, verificando que pertenezca al docente de la sesión (RNF03). */
    CargaAcademica cargaPropia(Sesion sesion, int cargaId) {
        int docenteId = docenteId(sesion);
        CargaAcademica carga = cargaDAO.buscar(cargaId)
                .orElseThrow(() -> new BusinessException("La carga académica no existe."));
        if (carga.docenteId() != docenteId) {
            throw new AccesoDenegadoException("Esta sección y área no están asignadas a usted.");
        }
        return carga;
    }

    private int docenteId(Sesion sesion) {
        Permisos.exigir(sesion, Rol.DOCENTE);
        return cargaDAO.buscarDocenteIdPorUsuario(sesion.getUsuarioId())
                .orElseThrow(() -> new AccesoDenegadoException("La cuenta no está vinculada a un docente."));
    }

    private void exigirPeriodoAbierto(int periodoId) {
        Periodo periodo = periodoDAO.buscar(periodoId)
                .orElseThrow(() -> new BusinessException("El periodo no existe."));
        if (periodo.cerrado()) {
            throw new BusinessException("El " + periodo.etiqueta() + " está cerrado: no se pueden modificar notas.");
        }
    }
}
