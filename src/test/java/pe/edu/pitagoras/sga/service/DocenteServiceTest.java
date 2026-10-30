package pe.edu.pitagoras.sga.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
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
import pe.edu.pitagoras.sga.model.academico.Curso;
import pe.edu.pitagoras.sga.model.academico.Periodo;
import pe.edu.pitagoras.sga.model.academico.Seccion;
import pe.edu.pitagoras.sga.model.auth.Rol;
import pe.edu.pitagoras.sga.model.auth.Sesion;
import pe.edu.pitagoras.sga.model.evaluacion.Calificativo;
import pe.edu.pitagoras.sga.model.evaluacion.CambioNota;
import pe.edu.pitagoras.sga.model.evaluacion.EstadoAsistencia;
import pe.edu.pitagoras.sga.model.evaluacion.FilaAsistencia;
import pe.edu.pitagoras.sga.model.evaluacion.FilaNotas;

/** Reglas del módulo Docente: RF11, RF12, RF13 y bloqueo por periodo cerrado (RF07). */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DocenteServiceTest {

    private static final Clock RELOJ = Clock.fixed(Instant.parse("2026-10-30T15:00:00Z"), ZoneOffset.UTC);
    private static final Sesion DOCENTE = new Sesion(3, "docente01", Rol.DOCENTE, null);
    private static final Seccion SECCION = new Seccion(1, 1, "1.°", "A", "Mañana");
    private static final CargaAcademica PROPIA = new CargaAcademica(10, new Curso(1, "Matemática"), SECCION, 7, "Mendoza");
    private static final CargaAcademica AJENA = new CargaAcademica(11, new Curso(2, "Comunicación"), SECCION, 8, "Quispe");
    private static final Periodo ABIERTO = new Periodo(4, 2026, 4, LocalDate.of(2026, 10, 19),
            LocalDate.of(2026, 12, 18), false, null, null);
    private static final Periodo CERRADO = new Periodo(3, 2026, 3, LocalDate.of(2026, 8, 10),
            LocalDate.of(2026, 10, 9), true, null, "director");

    @Mock private ICargaAcademicaDAO cargaDAO;
    @Mock private IAlumnoDAO alumnoDAO;
    @Mock private IPeriodoDAO periodoDAO;
    @Mock private IEstructuraDAO estructuraDAO;
    @Mock private INotaDAO notaDAO;
    @Mock private IAsistenciaDAO asistenciaDAO;
    @Mock private IConclusionDAO conclusionDAO;
    private DocenteService service;

    @BeforeEach
    void preparar() {
        service = new DocenteService(cargaDAO, alumnoDAO, periodoDAO, estructuraDAO, notaDAO, asistenciaDAO,
                conclusionDAO, RELOJ);
        when(cargaDAO.buscarDocenteIdPorUsuario(3)).thenReturn(Optional.of(7));
        when(cargaDAO.buscar(10)).thenReturn(Optional.of(PROPIA));
        when(cargaDAO.buscar(11)).thenReturn(Optional.of(AJENA));
        when(periodoDAO.buscar(4)).thenReturn(Optional.of(ABIERTO));
        when(periodoDAO.buscar(3)).thenReturn(Optional.of(CERRADO));
        when(periodoDAO.listar()).thenReturn(List.of(CERRADO, ABIERTO));
        when(alumnoDAO.listar(1, true)).thenReturn(List.of(new Alumno(5, SECCION, "70000001", "Luis", "Torres",
                "Ana Torres", null, Alumno.ACTIVO, "apo70000001")));
    }

    private static List<FilaNotas> filaConCambio() {
        FilaNotas f = new FilaNotas(5, "70000001", "Torres, Luis");
        f.cargar("1", Calificativo.B);
        f.poner("1", Calificativo.A);
        return List.of(f);
    }

    @Test
    void guardarNotas_enviaSoloLosCambiosAlDAO() {
        when(notaDAO.guardar(eq(10), eq(4), any(), eq(3))).thenReturn(1);

        assertEquals(1, service.guardarNotas(DOCENTE, 10, 4, filaConCambio()));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<CambioNota>> cambios = ArgumentCaptor.forClass(List.class);
        verify(notaDAO).guardar(eq(10), eq(4), cambios.capture(), eq(3));
        assertEquals(List.of(new CambioNota(5, "1", Calificativo.A)), cambios.getValue());
    }

    @Test
    void guardarNotas_periodoCerrado_RF07() {
        assertThrows(BusinessException.class, () -> service.guardarNotas(DOCENTE, 10, 3, filaConCambio()));
        verify(notaDAO, never()).guardar(anyInt(), anyInt(), any(), anyInt());
    }

    @Test
    void guardarNotas_cargaDeOtroDocente_RNF03() {
        assertThrows(AccesoDenegadoException.class, () -> service.guardarNotas(DOCENTE, 11, 4, filaConCambio()));
    }

    @Test
    void guardarNotas_sinCambios() {
        FilaNotas f = new FilaNotas(5, "70000001", "Torres, Luis");
        f.cargar("1", Calificativo.B);
        assertThrows(BusinessException.class, () -> service.guardarNotas(DOCENTE, 10, 4, List.of(f)));
    }

    @Test
    void soloElDocentePuedeUsarElModulo() {
        Sesion director = new Sesion(1, "director", Rol.DIRECTOR, null);
        assertThrows(AccesoDenegadoException.class, () -> service.misCargas(director));
    }

    @Test
    void asistencia_sinMarcaApareceComoAsistio() {
        List<FilaAsistencia> filas = service.asistencia(DOCENTE, 10, LocalDate.of(2026, 10, 30));
        assertEquals(EstadoAsistencia.ASISTIO, filas.get(0).getEstado());
        assertEquals(false, filas.get(0).isRegistrada());
    }

    @Test
    void guardarAsistencia_fechaFutura() {
        List<FilaAsistencia> filas = service.asistencia(DOCENTE, 10, LocalDate.of(2026, 10, 30));
        assertThrows(BusinessException.class,
                () -> service.guardarAsistencia(DOCENTE, 10, LocalDate.of(2026, 11, 2), filas));
    }

    @Test
    void guardarAsistencia_bimestreCerrado() {
        List<FilaAsistencia> filas = service.asistencia(DOCENTE, 10, LocalDate.of(2026, 9, 1));
        assertThrows(BusinessException.class,
                () -> service.guardarAsistencia(DOCENTE, 10, LocalDate.of(2026, 9, 1), filas));
    }

    @Test
    void guardarAsistencia_fueraDeLosBimestres() {
        List<FilaAsistencia> filas = service.asistencia(DOCENTE, 10, LocalDate.of(2026, 10, 14));
        assertThrows(BusinessException.class,
                () -> service.guardarAsistencia(DOCENTE, 10, LocalDate.of(2026, 10, 14), filas));
    }

    @Test
    void guardarAsistencia_enLote() {
        List<FilaAsistencia> filas = service.asistencia(DOCENTE, 10, LocalDate.of(2026, 10, 30));
        filas.get(0).setEstado(EstadoAsistencia.FALTA_JUSTIFICADA);
        when(asistenciaDAO.guardar(eq(10), eq(LocalDate.of(2026, 10, 30)), any(), eq(3))).thenReturn(1);

        assertEquals(1, service.guardarAsistencia(DOCENTE, 10, LocalDate.of(2026, 10, 30), filas));
    }

    @Test
    void conclusiones_maximo500Caracteres() {
        java.util.Map<String, String> textos = java.util.Map.of("1", "x".repeat(501));
        assertThrows(BusinessException.class, () -> service.guardarConclusiones(DOCENTE, 10, 4, 5, textos));
        verify(conclusionDAO, never()).guardar(anyInt(), anyInt(), anyInt(), any(), anyInt());
    }

    @Test
    void conclusiones_noEnPeriodoCerrado() {
        assertThrows(BusinessException.class,
                () -> service.guardarConclusiones(DOCENTE, 10, 3, 5, java.util.Map.of("1", "Logra lo esperado.")));
    }

    @Test
    void periodoActual_esElQueContieneHoy() {
        assertEquals(ABIERTO, service.periodoActual());
    }
}
