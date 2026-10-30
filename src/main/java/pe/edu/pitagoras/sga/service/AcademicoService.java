package pe.edu.pitagoras.sga.service;

import com.google.common.base.Preconditions;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pe.edu.pitagoras.sga.dao.interfaces.IAsistenciaDAO;
import pe.edu.pitagoras.sga.dao.interfaces.ICargaAcademicaDAO;
import pe.edu.pitagoras.sga.dao.interfaces.ICuentaDAO;
import pe.edu.pitagoras.sga.dao.interfaces.IEstructuraDAO;
import pe.edu.pitagoras.sga.dao.interfaces.INotaDAO;
import pe.edu.pitagoras.sga.dao.interfaces.IPeriodoDAO;
import pe.edu.pitagoras.sga.exception.BusinessException;
import pe.edu.pitagoras.sga.model.academico.CargaAcademica;
import pe.edu.pitagoras.sga.model.academico.Competencia;
import pe.edu.pitagoras.sga.model.academico.Curso;
import pe.edu.pitagoras.sga.model.academico.Docente;
import pe.edu.pitagoras.sga.model.academico.Grado;
import pe.edu.pitagoras.sga.model.academico.Periodo;
import pe.edu.pitagoras.sga.model.academico.Seccion;
import pe.edu.pitagoras.sga.model.auth.Rol;
import pe.edu.pitagoras.sga.model.auth.Sesion;
import pe.edu.pitagoras.sga.model.evaluacion.Calificativo;
import pe.edu.pitagoras.sga.model.evaluacion.DetalleRendimiento;
import pe.edu.pitagoras.sga.model.evaluacion.FiltroAsistencia;
import pe.edu.pitagoras.sga.model.evaluacion.FiltroNotas;
import pe.edu.pitagoras.sga.model.evaluacion.ResumenAsistencia;
import pe.edu.pitagoras.sga.util.Validaciones;

/**
 * Configuración académica (RF05), carga académica (RF06) y cierre/reapertura de periodos (RF07).
 */
public class AcademicoService {

    private static final Logger LOG = LoggerFactory.getLogger(AcademicoService.class);
    private static final Pattern LETRA = Pattern.compile("[A-Z]");
    private static final int MAX_COMPETENCIAS = 4;

    private final IEstructuraDAO estructuraDAO;
    private final ICargaAcademicaDAO cargaDAO;
    private final IPeriodoDAO periodoDAO;
    private final INotaDAO notaDAO;
    private final IAsistenciaDAO asistenciaDAO;
    private final ICuentaDAO cuentaDAO;

    public AcademicoService(IEstructuraDAO estructuraDAO, ICargaAcademicaDAO cargaDAO, IPeriodoDAO periodoDAO,
                            INotaDAO notaDAO, IAsistenciaDAO asistenciaDAO, ICuentaDAO cuentaDAO) {
        this.cuentaDAO = Preconditions.checkNotNull(cuentaDAO);
        this.estructuraDAO = Preconditions.checkNotNull(estructuraDAO);
        this.cargaDAO = Preconditions.checkNotNull(cargaDAO);
        this.periodoDAO = Preconditions.checkNotNull(periodoDAO);
        this.notaDAO = Preconditions.checkNotNull(notaDAO);
        this.asistenciaDAO = Preconditions.checkNotNull(asistenciaDAO);
    }

    // ---------- consultas (cualquier usuario con sesión) ----------

    public List<Grado> grados() {
        return estructuraDAO.listarGrados();
    }

    public List<Seccion> secciones() {
        return estructuraDAO.listarSecciones();
    }

    public List<String> turnos() {
        return estructuraDAO.listarTurnos();
    }

    public List<Curso> cursos() {
        return estructuraDAO.listarCursos();
    }

    public List<Competencia> competencias(int cursoId) {
        return estructuraDAO.listarCompetencias(cursoId);
    }

    public List<Periodo> periodos() {
        return periodoDAO.listar();
    }

    // ---------- RF05: estructura ----------

    public void crearSeccion(Sesion sesion, Grado grado, String letra, String turno) {
        Permisos.exigir(sesion, Rol.DIRECTOR);
        if (grado == null || turno == null) {
            throw new BusinessException("Seleccione el grado y el turno.");
        }
        String l = letra == null ? "" : letra.trim().toUpperCase(java.util.Locale.ROOT);
        if (!LETRA.matcher(l).matches()) {
            throw new BusinessException("La sección debe ser una sola letra (A-Z).");
        }
        boolean existe = estructuraDAO.listarSecciones().stream()
                .anyMatch(s -> s.gradoId() == grado.gradoId() && s.letra().equals(l));
        if (existe) {
            throw new BusinessException("La sección " + grado.nombre() + " " + l + " ya existe.");
        }
        estructuraDAO.crearSeccion(grado.gradoId(), l, turno, sesion.getUsuarioId());
    }

    public void crearCurso(Sesion sesion, String nombre, List<String> competencias) {
        Permisos.exigir(sesion, Rol.DIRECTOR);
        String n = Validaciones.requerido(nombre, "el nombre del área", 100);
        List<String> lista = new ArrayList<>();
        for (String c : competencias) {
            if (c != null && !c.isBlank()) {
                lista.add(Validaciones.requerido(c, "la competencia", 150));
            }
        }
        if (lista.isEmpty() || lista.size() > MAX_COMPETENCIAS) {
            throw new BusinessException("Ingrese de 1 a " + MAX_COMPETENCIAS + " competencias.");
        }
        if (estructuraDAO.listarCursos().stream().anyMatch(c -> c.nombre().equalsIgnoreCase(n))) {
            throw new BusinessException("El área " + n + " ya existe.");
        }
        estructuraDAO.crearCurso(n, lista, sesion.getUsuarioId());
    }

    // ---------- RF06: carga académica ----------

    public List<CargaAcademica> cargas(Sesion sesion) {
        Permisos.exigir(sesion, Rol.DIRECTOR);
        return cargaDAO.listar();
    }

    public void asignarCarga(Sesion sesion, Curso curso, Seccion seccion, Docente docente) {
        Permisos.exigir(sesion, Rol.DIRECTOR);
        if (curso == null || seccion == null || docente == null) {
            throw new BusinessException("Seleccione el área, la sección y el docente.");
        }
        boolean dictaEseArea = cuentaDAO.areasPorUsuario().getOrDefault(docente.usuarioId(), List.of()).stream()
                .anyMatch(c -> c.cursoId() == curso.cursoId());
        if (!dictaEseArea) {
            throw new BusinessException(docente.nombreCompleto() + " no tiene registrada el área " + curso.nombre()
                    + ". Agréguela en Usuarios > Editar datos.");
        }
        cargaDAO.asignar(curso.cursoId(), seccion.seccionId(), docente.docenteId(), sesion.getUsuarioId());
    }

    // ---------- RF05 y RF07: periodos ----------

    public void actualizarFechas(Sesion sesion, Periodo periodo, LocalDate inicio, LocalDate fin) {
        Permisos.exigir(sesion, Rol.DIRECTOR);
        if (inicio == null || fin == null || !fin.isAfter(inicio)) {
            throw new BusinessException("La fecha de fin debe ser posterior a la de inicio.");
        }
        boolean cruza = periodoDAO.listar().stream()
                .filter(p -> p.periodoId() != periodo.periodoId() && p.anioLectivo() == periodo.anioLectivo())
                .anyMatch(p -> !inicio.isAfter(p.fechaFin()) && !fin.isBefore(p.fechaInicio()));
        if (cruza) {
            throw new BusinessException("Las fechas se cruzan con otro bimestre del mismo año.");
        }
        periodoDAO.actualizarFechas(periodo.periodoId(), inicio, fin, sesion.getUsuarioId());
    }

    /**
     * RF07: cierra el periodo y guarda el consolidado de cada alumno por área
     * (calificativo del área y totales de asistencia).
     *
     * @return cantidad de registros consolidados.
     */
    public int cerrarPeriodo(Sesion sesion, Periodo periodo) {
        Permisos.exigir(sesion, Rol.DIRECTOR);
        Periodo actual = periodoDAO.buscar(periodo.periodoId())
                .orElseThrow(() -> new BusinessException("El periodo no existe."));
        if (actual.cerrado()) {
            throw new BusinessException("El " + actual.etiqueta() + " ya está cerrado.");
        }
        Map<CalculoAcademico.Clave, Calificativo> areas =
                CalculoAcademico.areas(notaDAO.listar(FiltroNotas.dePeriodo(actual.periodoId())));
        Map<CalculoAcademico.Clave, ResumenAsistencia> asistencia = CalculoAcademico.asistencia(
                asistenciaDAO.listar(FiltroAsistencia.entre(actual.fechaInicio(), actual.fechaFin())));
        List<DetalleRendimiento> consolidado = new ArrayList<>();
        areas.forEach((k, cal) -> consolidado.add(new DetalleRendimiento(k.alumnoId(), k.cursoSeccionId(), cal,
                asistencia.getOrDefault(k, ResumenAsistencia.VACIO))));
        periodoDAO.cerrar(actual.periodoId(), sesion.getUsuarioId(), consolidado);
        LOG.info("Periodo cerrado: {} ({} registros)", actual.etiqueta(), consolidado.size());
        return consolidado.size();
    }

    /** RF07: reabre un periodo cerrado dejando el motivo en la bitácora. */
    public void reabrirPeriodo(Sesion sesion, Periodo periodo, String motivo) {
        Permisos.exigir(sesion, Rol.DIRECTOR);
        String m = Validaciones.requerido(motivo, "el motivo de la reapertura", 200);
        Periodo actual = periodoDAO.buscar(periodo.periodoId())
                .orElseThrow(() -> new BusinessException("El periodo no existe."));
        if (!actual.cerrado()) {
            throw new BusinessException("El " + actual.etiqueta() + " ya está abierto.");
        }
        periodoDAO.reabrir(actual.periodoId(), sesion.getUsuarioId(), m);
        LOG.info("Periodo reabierto: {}", actual.etiqueta());
    }
}
