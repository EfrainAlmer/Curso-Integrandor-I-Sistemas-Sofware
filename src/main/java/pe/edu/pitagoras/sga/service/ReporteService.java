package pe.edu.pitagoras.sga.service;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import java.io.File;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.stream.Collectors;
import pe.edu.pitagoras.sga.dao.interfaces.IAlumnoDAO;
import pe.edu.pitagoras.sga.dao.interfaces.IAsistenciaDAO;
import pe.edu.pitagoras.sga.dao.interfaces.ICargaAcademicaDAO;
import pe.edu.pitagoras.sga.dao.interfaces.IConclusionDAO;
import pe.edu.pitagoras.sga.model.evaluacion.Conclusion;
import pe.edu.pitagoras.sga.model.evaluacion.ConclusionLibreta;
import pe.edu.pitagoras.sga.model.evaluacion.ResumenArea;
import pe.edu.pitagoras.sga.model.gestion.DatosColegio;
import pe.edu.pitagoras.sga.dao.interfaces.IEstructuraDAO;
import pe.edu.pitagoras.sga.dao.interfaces.INotaDAO;
import pe.edu.pitagoras.sga.dao.interfaces.IPeriodoDAO;
import pe.edu.pitagoras.sga.exception.AccesoDenegadoException;
import pe.edu.pitagoras.sga.exception.BusinessException;
import pe.edu.pitagoras.sga.model.academico.Alumno;
import pe.edu.pitagoras.sga.model.academico.CargaAcademica;
import pe.edu.pitagoras.sga.model.academico.Competencia;
import pe.edu.pitagoras.sga.model.academico.Curso;
import pe.edu.pitagoras.sga.model.academico.Periodo;
import pe.edu.pitagoras.sga.model.academico.Seccion;
import pe.edu.pitagoras.sga.model.auth.Rol;
import pe.edu.pitagoras.sga.model.auth.Sesion;
import pe.edu.pitagoras.sga.model.evaluacion.AsistenciaRegistro;
import pe.edu.pitagoras.sga.model.evaluacion.Calificativo;
import pe.edu.pitagoras.sga.model.evaluacion.Consolidado;
import pe.edu.pitagoras.sga.model.evaluacion.FilaConsolidado;
import pe.edu.pitagoras.sga.model.evaluacion.FilaNotas;
import pe.edu.pitagoras.sga.model.evaluacion.FiltroAsistencia;
import pe.edu.pitagoras.sga.model.evaluacion.FiltroNotas;
import pe.edu.pitagoras.sga.model.evaluacion.Libreta;
import pe.edu.pitagoras.sga.model.evaluacion.LineaLibreta;
import pe.edu.pitagoras.sga.model.evaluacion.NotaRegistro;
import pe.edu.pitagoras.sga.model.evaluacion.ReglaCalificativo;
import pe.edu.pitagoras.sga.model.evaluacion.ResumenAlumno;
import pe.edu.pitagoras.sga.model.evaluacion.ResumenAsistencia;
import pe.edu.pitagoras.sga.model.gestion.AlertaAlumno;
import pe.edu.pitagoras.sga.model.gestion.AvanceDocente;
import pe.edu.pitagoras.sga.model.gestion.Indicadores;
import pe.edu.pitagoras.sga.util.ExcelReportes;
import pe.edu.pitagoras.sga.util.PdfReportes;

/**
 * Panel de indicadores (RF08), libretas (RF17), reportes PDF (RF18) y exportación a Excel (RF19).
 * La Dirección ve todo; el docente solo sus cargas; el apoderado solo a su alumno.
 */
public class ReporteService {

    private final IAlumnoDAO alumnoDAO;
    private final INotaDAO notaDAO;
    private final IAsistenciaDAO asistenciaDAO;
    private final IPeriodoDAO periodoDAO;
    private final ICargaAcademicaDAO cargaDAO;
    private final IEstructuraDAO estructuraDAO;
    private final Clock reloj;
    private final IConclusionDAO conclusionDAO;
    private final DatosColegio colegio;

    public ReporteService(IAlumnoDAO alumnoDAO, INotaDAO notaDAO, IAsistenciaDAO asistenciaDAO,
                          IPeriodoDAO periodoDAO, ICargaAcademicaDAO cargaDAO, IEstructuraDAO estructuraDAO,
                          IConclusionDAO conclusionDAO, DatosColegio colegio, Clock reloj) {
        this.conclusionDAO = Preconditions.checkNotNull(conclusionDAO);
        this.colegio = Preconditions.checkNotNull(colegio);
        this.alumnoDAO = Preconditions.checkNotNull(alumnoDAO);
        this.notaDAO = Preconditions.checkNotNull(notaDAO);
        this.asistenciaDAO = Preconditions.checkNotNull(asistenciaDAO);
        this.periodoDAO = Preconditions.checkNotNull(periodoDAO);
        this.cargaDAO = Preconditions.checkNotNull(cargaDAO);
        this.estructuraDAO = Preconditions.checkNotNull(estructuraDAO);
        this.reloj = Preconditions.checkNotNull(reloj);
    }

    // ===================== Panel de indicadores (RF08) =====================

    public Indicadores indicadores(Sesion sesion, int periodoId) {
        Permisos.exigir(sesion, Rol.DIRECTOR);
        Periodo periodo = periodo(periodoId);
        List<Alumno> alumnos = alumnoDAO.listar(null, true);
        Map<Integer, Alumno> porId = alumnos.stream().collect(Collectors.toMap(Alumno::alumnoId, a -> a));
        List<CargaAcademica> cargas = cargaDAO.listar();
        Map<Integer, CargaAcademica> cargaPorId = cargas.stream()
                .collect(Collectors.toMap(CargaAcademica::cursoSeccionId, c -> c));
        List<NotaRegistro> notas = notaDAO.listar(FiltroNotas.dePeriodo(periodoId)).stream()
                .filter(n -> porId.containsKey(n.alumnoId())).toList();

        // Distribución de calificativos de área por grado y alertas de alumnos con C.
        Map<String, Map<Calificativo, Integer>> distribucion = new LinkedHashMap<>();
        Map<String, Map<Calificativo, Integer>> porSeccion = new LinkedHashMap<>();
        Map<String, Map<Calificativo, Integer>> porArea = new LinkedHashMap<>();
        estructuraDAO.listarGrados().forEach(g -> distribucion.put(g.nombre(), contadorVacio()));
        estructuraDAO.listarSecciones().forEach(s -> porSeccion.put(s.etiqueta(), contadorVacio()));
        estructuraDAO.listarCursos().forEach(c -> porArea.put(c.nombre(), contadorVacio()));
        Map<Integer, List<String>> areasEnC = new TreeMap<>();
        CalculoAcademico.areas(notas).forEach((clave, cal) -> {
            Alumno a = porId.get(clave.alumnoId());
            distribucion.computeIfAbsent(a.seccion().grado(), g -> contadorVacio()).merge(cal, 1, Integer::sum);
            porSeccion.computeIfAbsent(a.seccion().etiqueta(), g -> contadorVacio()).merge(cal, 1, Integer::sum);
            porArea.computeIfAbsent(cargaPorId.get(clave.cursoSeccionId()).curso().nombre(), g -> contadorVacio())
                    .merge(cal, 1, Integer::sum);
            if (cal == Calificativo.C) {
                areasEnC.computeIfAbsent(a.alumnoId(), k -> new ArrayList<>())
                        .add(cargaPorId.get(clave.cursoSeccionId()).curso().nombre());
            }
        });
        List<AlertaAlumno> alertas = areasEnC.entrySet().stream()
                .map(e -> {
                    Alumno a = porId.get(e.getKey());
                    return new AlertaAlumno(a.alumnoId(), a.nombreCompleto(), a.seccion().etiqueta(), e.getValue());
                })
                .sorted(Comparator.comparingInt((AlertaAlumno x) -> -x.areas().size()).thenComparing(AlertaAlumno::seccion))
                .toList();

        // Avance del registro: calificativos registrados / alumnos activos x competencias.
        Map<Integer, Long> alumnosPorSeccion = alumnos.stream()
                .collect(Collectors.groupingBy(a -> a.seccion().seccionId(), Collectors.counting()));
        Map<Integer, Integer> competenciasPorCurso = new HashMap<>();
        Map<Integer, Long> notasPorCarga = notas.stream()
                .collect(Collectors.groupingBy(NotaRegistro::cursoSeccionId, Collectors.counting()));
        Map<String, int[]> avance = new TreeMap<>();
        for (CargaAcademica c : cargas) {
            int comp = competenciasPorCurso.computeIfAbsent(c.curso().cursoId(),
                    id -> estructuraDAO.listarCompetencias(id).size());
            int[] v = avance.computeIfAbsent(c.docenteNombre(), k -> new int[2]);
            v[0] += notasPorCarga.getOrDefault(c.cursoSeccionId(), 0L).intValue();
            v[1] += alumnosPorSeccion.getOrDefault(c.seccion().seccionId(), 0L).intValue() * comp;
        }
        List<AvanceDocente> avances = avance.entrySet().stream()
                .map(e -> new AvanceDocente(e.getKey(), e.getValue()[0], e.getValue()[1])).toList();

        double asistencia = ResumenAsistencia.de(asistenciaDAO.listar(
                FiltroAsistencia.entre(periodo.fechaInicio(), periodo.fechaFin()))).porcentajeAsistencia();
        int secciones = alumnosPorSeccion.size();
        return new Indicadores(periodo, alumnos.size(), secciones, asistencia, distribucion, porSeccion, porArea,
                avances, alertas);
    }

    // ===================== Libreta (RF14, RF17) =====================

    /** Libreta de un alumno. Director: cualquiera. Apoderado: solo el suyo. */
    public Libreta libreta(Sesion sesion, int alumnoId) {
        Permisos.exigir(sesion, Rol.DIRECTOR, Rol.APODERADO);
        if (sesion.tieneRol(Rol.APODERADO) && !Objects.equals(sesion.getAlumnoId(), alumnoId)) {
            throw new AccesoDenegadoException("Solo puede consultar la libreta de su estudiante.");
        }
        Alumno alumno = alumnoDAO.buscar(alumnoId)
                .orElseThrow(() -> new BusinessException("El alumno no existe."));
        int anio = anioActual();
        List<Periodo> delAnio = periodoDAO.listar().stream().filter(p -> p.anioLectivo() == anio).toList();
        java.util.Set<Integer> periodosDelAnio = delAnio.stream().map(Periodo::periodoId)
                .collect(Collectors.toSet());
        List<CargaAcademica> cargas = cargaDAO.listar().stream()
                .filter(c -> c.seccion().seccionId() == alumno.seccion().seccionId()).toList();

        // curso|competencia → bimestre → calificativo
        Map<String, Map<Integer, Calificativo>> notas = new HashMap<>();
        for (NotaRegistro n : notaDAO.listar(FiltroNotas.deAlumno(alumnoId))) {
            if (periodosDelAnio.contains(n.periodoId())) {
                notas.computeIfAbsent(n.cursoId() + "|" + n.codigoCompetencia(), k -> new TreeMap<>())
                        .put(n.bimestre(), n.calificativo());
            }
        }
        List<LineaLibreta> lineas = new ArrayList<>();
        List<ResumenArea> areas = new ArrayList<>();
        Map<String, String> nombreCompetencia = new HashMap<>();
        Map<Integer, String> nombreArea = new HashMap<>();
        for (CargaAcademica c : cargas) {
            Map<Integer, List<Calificativo>> porBimestreArea = new TreeMap<>();
            nombreArea.put(c.curso().cursoId(), c.curso().nombre());
            for (Competencia k : estructuraDAO.listarCompetencias(c.curso().cursoId())) {
                Map<Integer, Calificativo> porBimestre = notas.getOrDefault(k.cursoId() + "|" + k.codigo(), Map.of());
                porBimestre.forEach((b, cal) -> porBimestreArea.computeIfAbsent(b, x -> new ArrayList<>()).add(cal));
                lineas.add(new LineaLibreta(c.curso().nombre(), k.nombre(), porBimestre,
                        ReglaCalificativo.calificativoAnual(porBimestre)));
                nombreCompetencia.put(k.cursoId() + "|" + k.codigo(), k.nombre());
            }
            Map<Integer, Calificativo> area = new TreeMap<>();
            porBimestreArea.forEach((b, lista) -> ReglaCalificativo.calificativoArea(lista)
                    .ifPresent(cal -> area.put(b, cal)));
            areas.add(new ResumenArea(c.curso().nombre(), area, ReglaCalificativo.calificativoAnual(area)));
        }

        List<ConclusionLibreta> conclusiones = new ArrayList<>();
        for (Conclusion cd : conclusionDAO.listar(FiltroNotas.deAlumno(alumnoId))) {
            String clave = cd.cursoId() + "|" + cd.codigoCompetencia();
            if (periodosDelAnio.contains(cd.periodoId()) && nombreCompetencia.containsKey(clave)) {
                conclusiones.add(new ConclusionLibreta(nombreArea.get(cd.cursoId()), nombreCompetencia.get(clave),
                        cd.bimestre(), notas.getOrDefault(clave, Map.of()).get(cd.bimestre()), cd.texto()));
            }
        }
        // Mismo orden que la tabla de competencias (área y competencia), luego por bimestre.
        Map<String, Integer> orden = new HashMap<>();
        for (int i = 0; i < lineas.size(); i++) {
            orden.put(lineas.get(i).area() + "|" + lineas.get(i).competencia(), i);
        }
        conclusiones.sort(Comparator.comparingInt((ConclusionLibreta c) -> orden.getOrDefault(c.area() + "|"
                + c.competencia(), 0)).thenComparingInt(ConclusionLibreta::bimestre));

        Map<Integer, ResumenAsistencia> asistenciaBimestre = new TreeMap<>();
        List<AsistenciaRegistro> todas = new ArrayList<>();
        for (Periodo p : delAnio) {
            List<AsistenciaRegistro> registros = asistenciaDAO.listar(
                    FiltroAsistencia.deAlumno(alumnoId, p.fechaInicio(), p.fechaFin()));
            asistenciaBimestre.put(p.bimestre(), ResumenAsistencia.de(registros));
            todas.addAll(registros);
        }
        String situacion = ReglaCalificativo.situacionFinal(areas.stream().map(ResumenArea::anual).toList());
        return new Libreta(colegio, alumno, anio, ImmutableList.copyOf(lineas), ImmutableList.copyOf(areas),
                ImmutableList.copyOf(conclusiones), asistenciaBimestre, ResumenAsistencia.de(todas), situacion);
    }

    public File exportarLibretaPdf(Sesion sesion, int alumnoId, File destino) {
        PdfReportes.libreta(libreta(sesion, alumnoId), destino);
        return destino;
    }

    // ===================== Nómina (RF18, RF19) =====================

    public List<Alumno> nomina(Sesion sesion, Integer seccionId) {
        Permisos.exigir(sesion, Rol.DIRECTOR, Rol.DOCENTE);
        if (sesion.tieneRol(Rol.DOCENTE)) {
            if (seccionId == null || misCargas(sesion).stream().noneMatch(c -> c.seccion().seccionId() == seccionId)) {
                throw new AccesoDenegadoException("Solo puede ver la nómina de sus secciones.");
            }
        }
        return alumnoDAO.listar(seccionId, true);
    }

    public File exportarNominaPdf(Sesion sesion, int seccionId, File destino) {
        List<Alumno> alumnos = nomina(sesion, seccionId);
        PdfReportes.nomina(seccion(seccionId), anioActual(), alumnos, destino);
        return destino;
    }

    public File exportarNominaExcel(Sesion sesion, Integer seccionId, File destino) {
        ExcelReportes.nomina(nomina(sesion, seccionId), destino);
        return destino;
    }

    // ===================== Consolidado bimestral por sección (RF19) =====================

    public Consolidado consolidado(Sesion sesion, int seccionId, int periodoId) {
        Permisos.exigir(sesion, Rol.DIRECTOR);
        Seccion seccion = seccion(seccionId);
        Periodo periodo = periodo(periodoId);
        List<CargaAcademica> cargas = cargaDAO.listar().stream()
                .filter(c -> c.seccion().seccionId() == seccionId).toList();
        Map<Integer, Integer> cursoDeCarga = cargas.stream()
                .collect(Collectors.toMap(CargaAcademica::cursoSeccionId, c -> c.curso().cursoId()));
        Map<CalculoAcademico.Clave, Calificativo> areas =
                CalculoAcademico.areas(notaDAO.listar(FiltroNotas.deSeccion(seccionId, periodoId)));
        Map<Integer, List<AsistenciaRegistro>> asistencia = asistenciaDAO.listar(
                        FiltroAsistencia.deSeccion(seccionId, periodo.fechaInicio(), periodo.fechaFin())).stream()
                .collect(Collectors.groupingBy(AsistenciaRegistro::alumnoId));
        List<FilaConsolidado> filas = new ArrayList<>();
        for (Alumno a : alumnoDAO.listar(seccionId, true)) {
            Map<Integer, Calificativo> porCurso = new HashMap<>();
            areas.forEach((k, cal) -> {
                if (k.alumnoId() == a.alumnoId() && cursoDeCarga.containsKey(k.cursoSeccionId())) {
                    porCurso.put(cursoDeCarga.get(k.cursoSeccionId()), cal);
                }
            });
            filas.add(new FilaConsolidado(a, porCurso,
                    ResumenAsistencia.de(asistencia.getOrDefault(a.alumnoId(), List.of()))));
        }
        List<Curso> cursos = cargas.stream().map(CargaAcademica::curso).toList();
        return new Consolidado(seccion, periodo, cursos, filas);
    }

    public File exportarConsolidadoPdf(Sesion sesion, int seccionId, int periodoId, File destino) {
        PdfReportes.consolidado(consolidado(sesion, seccionId, periodoId), destino);
        return destino;
    }

    public File exportarConsolidadoExcel(Sesion sesion, int seccionId, int periodoId, File destino) {
        ExcelReportes.consolidado(consolidado(sesion, seccionId, periodoId), destino);
        return destino;
    }

    // ===================== Acta de notas y asistencia por carga (RF18) =====================

    /** @return la grilla de notas de una carga (docente dueño o Dirección). */
    public List<FilaNotas> actaNotas(Sesion sesion, int cargaId, int periodoId) {
        CargaAcademica carga = cargaPermitida(sesion, cargaId);
        return CalculoAcademico.filasNotas(alumnoDAO.listar(carga.seccion().seccionId(), true),
                notaDAO.listar(FiltroNotas.deCarga(cargaId, periodoId)));
    }

    public File exportarActaNotasPdf(Sesion sesion, int cargaId, int periodoId, File destino) {
        CargaAcademica carga = cargaPermitida(sesion, cargaId);
        List<Competencia> comp = estructuraDAO.listarCompetencias(carga.curso().cursoId());
        PdfReportes.actaNotas(carga, periodo(periodoId), comp, actaNotas(sesion, cargaId, periodoId), destino);
        return destino;
    }

    public File exportarActaNotasExcel(Sesion sesion, int cargaId, int periodoId, File destino) {
        CargaAcademica carga = cargaPermitida(sesion, cargaId);
        List<Competencia> comp = estructuraDAO.listarCompetencias(carga.curso().cursoId());
        ExcelReportes.actaNotas(carga, periodo(periodoId), comp, actaNotas(sesion, cargaId, periodoId), destino);
        return destino;
    }

    /** @return totales de asistencia por alumno de una carga en un periodo. */
    public List<ResumenAlumno> asistenciaDeCarga(Sesion sesion, int cargaId, int periodoId) {
        CargaAcademica carga = cargaPermitida(sesion, cargaId);
        Periodo periodo = periodo(periodoId);
        Map<Integer, List<AsistenciaRegistro>> porAlumno = asistenciaDAO.listar(new FiltroAsistencia(null, cargaId,
                        null, periodo.fechaInicio(), periodo.fechaFin())).stream()
                .collect(Collectors.groupingBy(AsistenciaRegistro::alumnoId));
        return alumnoDAO.listar(carga.seccion().seccionId(), true).stream()
                .map(a -> new ResumenAlumno(a, ResumenAsistencia.de(porAlumno.getOrDefault(a.alumnoId(), List.of()))))
                .toList();
    }

    public File exportarAsistenciaPdf(Sesion sesion, int cargaId, int periodoId, File destino) {
        CargaAcademica carga = cargaPermitida(sesion, cargaId);
        PdfReportes.asistencia(carga, periodo(periodoId), asistenciaDeCarga(sesion, cargaId, periodoId), destino);
        return destino;
    }

    // ===================== apoyo =====================

    private CargaAcademica cargaPermitida(Sesion sesion, int cargaId) {
        Permisos.exigir(sesion, Rol.DIRECTOR, Rol.DOCENTE);
        CargaAcademica carga = cargaDAO.buscar(cargaId)
                .orElseThrow(() -> new BusinessException("La carga académica no existe."));
        if (sesion.tieneRol(Rol.DOCENTE) && misCargas(sesion).stream()
                .noneMatch(c -> c.cursoSeccionId() == cargaId)) {
            throw new AccesoDenegadoException("Esta sección y área no están asignadas a usted.");
        }
        return carga;
    }

    private List<CargaAcademica> misCargas(Sesion sesion) {
        int docenteId = cargaDAO.buscarDocenteIdPorUsuario(sesion.getUsuarioId())
                .orElseThrow(() -> new AccesoDenegadoException("La cuenta no está vinculada a un docente."));
        return cargaDAO.listarPorDocente(docenteId);
    }

    private Periodo periodo(int periodoId) {
        return periodoDAO.buscar(periodoId).orElseThrow(() -> new BusinessException("El periodo no existe."));
    }

    private Seccion seccion(int seccionId) {
        return estructuraDAO.listarSecciones().stream().filter(s -> s.seccionId() == seccionId).findFirst()
                .orElseThrow(() -> new BusinessException("La sección no existe."));
    }

    private int anioActual() {
        return periodoDAO.listar().stream().mapToInt(Periodo::anioLectivo).max()
                .orElse(java.time.LocalDate.now(reloj).getYear());
    }

    private static Map<Calificativo, Integer> contadorVacio() {
        Map<Calificativo, Integer> m = new EnumMap<>(Calificativo.class);
        for (Calificativo c : Calificativo.values()) {
            m.put(c, 0);
        }
        return m;
    }
}
