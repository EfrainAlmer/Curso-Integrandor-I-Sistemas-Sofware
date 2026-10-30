package pe.edu.pitagoras.sga.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import pe.edu.pitagoras.sga.dao.interfaces.IAlumnoDAO;
import pe.edu.pitagoras.sga.dao.interfaces.IAsistenciaDAO;
import pe.edu.pitagoras.sga.dao.interfaces.ICargaAcademicaDAO;
import pe.edu.pitagoras.sga.dao.interfaces.IConclusionDAO;
import pe.edu.pitagoras.sga.model.academico.Competencia;
import pe.edu.pitagoras.sga.model.gestion.DatosColegio;
import pe.edu.pitagoras.sga.dao.interfaces.IEstructuraDAO;
import pe.edu.pitagoras.sga.dao.interfaces.INotaDAO;
import pe.edu.pitagoras.sga.dao.interfaces.IPeriodoDAO;
import pe.edu.pitagoras.sga.exception.AccesoDenegadoException;
import pe.edu.pitagoras.sga.model.academico.Alumno;
import pe.edu.pitagoras.sga.model.academico.CargaAcademica;
import pe.edu.pitagoras.sga.model.academico.Curso;
import pe.edu.pitagoras.sga.model.academico.Periodo;
import pe.edu.pitagoras.sga.model.academico.Seccion;
import pe.edu.pitagoras.sga.model.auth.Rol;
import pe.edu.pitagoras.sga.model.auth.Sesion;
import pe.edu.pitagoras.sga.model.evaluacion.Calificativo;
import pe.edu.pitagoras.sga.model.evaluacion.Libreta;
import pe.edu.pitagoras.sga.model.evaluacion.NotaRegistro;

/** Consultas del apoderado (RF15-RF17) y control de acceso a su propio estudiante (RNF03). */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ConsultasTest {

    private static final Seccion SECCION = new Seccion(1, 1, "1.°", "A", "Mañana");
    private static final Alumno ALUMNO = new Alumno(5, SECCION, "70000001", "Luis", "Torres", "Ana Torres", null,
            Alumno.ACTIVO, "apo70000001");
    private static final Sesion APODERADO = new Sesion(20, "apo70000001", Rol.APODERADO, 5);

    @Mock private IAlumnoDAO alumnoDAO;
    @Mock private INotaDAO notaDAO;
    @Mock private IAsistenciaDAO asistenciaDAO;
    @Mock private IPeriodoDAO periodoDAO;
    @Mock private ICargaAcademicaDAO cargaDAO;
    @Mock private IEstructuraDAO estructuraDAO;
    @Mock private IConclusionDAO conclusionDAO;
    private ReporteService reportes;
    private ApoderadoService apoderado;

    @BeforeEach
    void preparar() {
        reportes = new ReporteService(alumnoDAO, notaDAO, asistenciaDAO, periodoDAO, cargaDAO, estructuraDAO,
                conclusionDAO, DatosColegio.POR_DEFECTO, Clock.systemDefaultZone());
        apoderado = new ApoderadoService(alumnoDAO, notaDAO, asistenciaDAO, periodoDAO, cargaDAO, estructuraDAO,
                conclusionDAO, reportes);
        when(estructuraDAO.listarCompetencias(1)).thenReturn(List.of(new Competencia(1, "1", "Resuelve problemas de cantidad")));
        when(alumnoDAO.buscar(5)).thenReturn(Optional.of(ALUMNO));
        when(periodoDAO.listar()).thenReturn(List.of(
                new Periodo(1, 2026, 1, LocalDate.of(2026, 3, 16), LocalDate.of(2026, 5, 15), true, null, null),
                new Periodo(4, 2026, 4, LocalDate.of(2026, 10, 19), LocalDate.of(2026, 12, 18), false, null, null)));
        when(cargaDAO.listar()).thenReturn(List.of(new CargaAcademica(10, new Curso(1, "Matemática"), SECCION, 7, "X")));
    }

    @Test
    void apoderado_noPuedeVerLaLibretaDeOtroAlumno() {
        assertThrows(AccesoDenegadoException.class, () -> reportes.libreta(APODERADO, 6));
    }

    @Test
    void apoderado_siempreConsultaAlAlumnoDeSuSesion() {
        assertEquals(ALUMNO, apoderado.miAlumno(APODERADO));
        Sesion docente = new Sesion(3, "docente01", Rol.DOCENTE, null);
        assertThrows(AccesoDenegadoException.class, () -> apoderado.miAlumno(docente));
    }

    @Test
    void libreta_anualEsElBimestre4_RF14() {
        when(notaDAO.listar(any())).thenReturn(List.of(
                new NotaRegistro(5, 10, 1, "Matemática", "1", 1, 1, Calificativo.C),
                new NotaRegistro(5, 10, 1, "Matemática", "1", 4, 4, Calificativo.AD)));

        Libreta l = apoderado.libreta(APODERADO);

        assertEquals(Calificativo.C, l.lineas().get(0).bimestre(1));
        assertEquals(Optional.of(Calificativo.AD), l.lineas().get(0).nivelFinal());
        assertEquals(Optional.of(Calificativo.AD), l.areas().get(0).anual());
        assertEquals("Promovido de grado (PRO)", l.situacionFinal());
    }

    @Test
    void libreta_sinBimestre4QuedaPendiente() {
        when(notaDAO.listar(any())).thenReturn(List.of(
                new NotaRegistro(5, 10, 1, "Matemática", "1", 1, 1, Calificativo.A)));
        Libreta l = apoderado.libreta(APODERADO);
        assertTrue(l.lineas().get(0).nivelFinal().isEmpty());
        assertTrue(l.situacionFinal().startsWith("En proceso"));
    }

    @Test
    void indicadores_soloDireccion() {
        assertThrows(AccesoDenegadoException.class, () -> reportes.indicadores(APODERADO, 4));
    }
}
