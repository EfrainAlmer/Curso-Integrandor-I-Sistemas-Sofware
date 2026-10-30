package pe.edu.pitagoras.sga.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import pe.edu.pitagoras.sga.model.academico.CargaAcademica;
import pe.edu.pitagoras.sga.model.academico.Curso;
import java.util.Optional;
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
import pe.edu.pitagoras.sga.dao.interfaces.ICuentaDAO;
import pe.edu.pitagoras.sga.dao.interfaces.IEstructuraDAO;
import pe.edu.pitagoras.sga.dao.interfaces.INotaDAO;
import pe.edu.pitagoras.sga.dao.interfaces.IPeriodoDAO;
import pe.edu.pitagoras.sga.exception.AccesoDenegadoException;
import pe.edu.pitagoras.sga.exception.BusinessException;
import pe.edu.pitagoras.sga.model.academico.Alumno;
import pe.edu.pitagoras.sga.model.academico.Docente;
import pe.edu.pitagoras.sga.model.academico.Periodo;
import pe.edu.pitagoras.sga.model.academico.Seccion;
import pe.edu.pitagoras.sga.model.auth.Rol;
import pe.edu.pitagoras.sga.model.auth.Sesion;
import pe.edu.pitagoras.sga.model.evaluacion.AsistenciaRegistro;
import pe.edu.pitagoras.sga.model.evaluacion.Calificativo;
import pe.edu.pitagoras.sga.model.evaluacion.DetalleRendimiento;
import pe.edu.pitagoras.sga.model.evaluacion.EstadoAsistencia;
import pe.edu.pitagoras.sga.model.evaluacion.NotaRegistro;
import pe.edu.pitagoras.sga.model.gestion.Credencial;
import pe.edu.pitagoras.sga.model.gestion.CuentaUsuario;
import pe.edu.pitagoras.sga.util.BCryptUtil;

/** Services de la Dirección: usuarios (RF03), inscripción (RF04) y periodos (RF05, RF07). */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DireccionServicesTest {

    private static final Sesion DIRECTOR = new Sesion(1, "director", Rol.DIRECTOR, null);
    private static final Sesion DOCENTE = new Sesion(3, "docente01", Rol.DOCENTE, null);
    private static final Seccion SECCION = new Seccion(1, 1, "1.°", "A", "Mañana");
    private static final Periodo B4 = new Periodo(4, 2026, 4, LocalDate.of(2026, 10, 19), LocalDate.of(2026, 12, 18),
            false, null, null);
    private static final Periodo B3 = new Periodo(3, 2026, 3, LocalDate.of(2026, 8, 10), LocalDate.of(2026, 10, 9),
            true, null, "director");

    private static final Curso MATE = new Curso(1, "Matemática");
    private static final Docente ROSA = new Docente(7, 30, "40000002", "Rosa", "Huamán", null);

    @Mock private ICuentaDAO cuentaDAO;
    @Mock private IAlumnoDAO alumnoDAO;
    @Mock private IEstructuraDAO estructuraDAO;
    @Mock private ICargaAcademicaDAO cargaDAO;
    @Mock private IPeriodoDAO periodoDAO;
    @Mock private INotaDAO notaDAO;
    @Mock private IAsistenciaDAO asistenciaDAO;

    // ---------- UsuarioService (RF03) ----------

    @Test
    void crearDocente_guardaSoloElHashYDevuelveClaveTemporal() {
        UsuarioService s = new UsuarioService(cuentaDAO, cargaDAO);
        Credencial c = s.crearDocente(DIRECTOR, "Docente11", "40000099", "Ana", "Pérez", "", List.of(MATE));

        ArgumentCaptor<String> hash = ArgumentCaptor.forClass(String.class);
        verify(cuentaDAO).crearDocente(eq("docente11"), hash.capture(), any(Docente.class), eq(List.of(1)), eq(1));
        assertNotEquals(c.claveTemporal(), hash.getValue());
        assertTrue(BCryptUtil.verificar(c.claveTemporal().toCharArray(), hash.getValue()));
    }

    @Test
    void crearDocente_usuarioRepetido() {
        when(cuentaDAO.existeUsername("docente01")).thenReturn(true);
        UsuarioService s = new UsuarioService(cuentaDAO, cargaDAO);
        assertThrows(BusinessException.class, () -> s.crearDocente(DIRECTOR, "docente01", "40000099", "Ana", "Pérez", null, List.of(MATE)));
    }

    @Test
    void crearDocente_soloDireccion() {
        UsuarioService s = new UsuarioService(cuentaDAO, cargaDAO);
        assertThrows(AccesoDenegadoException.class, () -> s.crearDocente(DOCENTE, "x1234", "40000099", "A", "B", null, List.of(MATE)));
    }

    @Test
    void noPuedeDesactivarseASiMismo() {
        UsuarioService s = new UsuarioService(cuentaDAO, cargaDAO);
        CuentaUsuario propia = new CuentaUsuario(1, "director", Rol.DIRECTOR, "", true, false, null);
        assertThrows(BusinessException.class, () -> s.cambiarEstado(DIRECTOR, propia, false));
        verify(cuentaDAO, never()).cambiarEstado(anyInt(), any(Boolean.class), anyInt());
    }

    @Test
    void docente_tieneDe1a3Areas() {
        UsuarioService s = new UsuarioService(cuentaDAO, cargaDAO);
        List<Curso> cuatro = List.of(MATE, new Curso(2, "Com"), new Curso(3, "CyT"), new Curso(4, "CCSS"));
        assertThrows(BusinessException.class, () -> s.crearDocente(DIRECTOR, "docente12", "40000098", "Ana", "Paz",
                null, cuatro));
        assertThrows(BusinessException.class, () -> s.crearDocente(DIRECTOR, "docente12", "40000098", "Ana", "Paz",
                null, List.of()));
    }

    @Test
    void noSeQuitaUnAreaQueElDocenteDicta() {
        when(cargaDAO.listarPorDocente(7)).thenReturn(List.of(new CargaAcademica(10, MATE, SECCION, 7, "Huamán")));
        UsuarioService s = new UsuarioService(cuentaDAO, cargaDAO);
        assertThrows(BusinessException.class, () -> s.actualizarDocente(DIRECTOR, ROSA, "40000002", "Rosa", "Huamán",
                null, List.of(new Curso(2, "Comunicación"))));
    }

    @Test
    void cargaAcademica_soloConDocentesDeEsaArea() {
        when(cuentaDAO.areasPorUsuario()).thenReturn(Map.of(30, List.of(new Curso(2, "Comunicación"))));
        assertThrows(BusinessException.class, () -> academico().asignarCarga(DIRECTOR, MATE, SECCION, ROSA));
        verify(cargaDAO, never()).asignar(anyInt(), anyInt(), anyInt(), anyInt());

        when(cuentaDAO.areasPorUsuario()).thenReturn(Map.of(30, List.of(MATE)));
        academico().asignarCarga(DIRECTOR, MATE, SECCION, ROSA);
        verify(cargaDAO).asignar(1, 1, 7, 1);
    }

    // ---------- AlumnoService (RF04) ----------

    @Test
    void inscribir_creaCuentaApoMasDni() {
        AlumnoService s = new AlumnoService(alumnoDAO, cuentaDAO);
        Credencial c = s.inscribir(DIRECTOR, SECCION, "71234567", "Pedro", "Gómez", "María Luna", "987654321");
        assertEquals("apo71234567", c.username());
        verify(alumnoDAO).inscribir(any(Alumno.class), eq("apo71234567"), anyString(), eq(1));
    }

    @Test
    void inscribir_dniRepetidoODniInvalido() {
        when(alumnoDAO.existeDni("71234567", 0)).thenReturn(true);
        AlumnoService s = new AlumnoService(alumnoDAO, cuentaDAO);
        assertThrows(BusinessException.class,
                () -> s.inscribir(DIRECTOR, SECCION, "71234567", "Pedro", "Gómez", "María", null));
        assertThrows(BusinessException.class,
                () -> s.inscribir(DIRECTOR, SECCION, "123", "Pedro", "Gómez", "María", null));
    }

    // ---------- AcademicoService (RF05, RF07) ----------

    private AcademicoService academico() {
        return new AcademicoService(estructuraDAO, cargaDAO, periodoDAO, notaDAO, asistenciaDAO, cuentaDAO);
    }

    @Test
    void cerrarPeriodo_guardaConsolidadoPorAlumnoYArea() {
        when(periodoDAO.buscar(4)).thenReturn(Optional.of(B4));
        when(notaDAO.listar(any())).thenReturn(List.of(
                new NotaRegistro(5, 10, 1, "Matemática", "1", 4, 4, Calificativo.A),
                new NotaRegistro(5, 10, 1, "Matemática", "2", 4, 4, Calificativo.AD)));
        when(asistenciaDAO.listar(any())).thenReturn(List.of(
                new AsistenciaRegistro(5, 10, "Matemática", LocalDate.of(2026, 10, 20), EstadoAsistencia.FALTA, null)));

        assertEquals(1, academico().cerrarPeriodo(DIRECTOR, B4));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<DetalleRendimiento>> c = ArgumentCaptor.forClass(List.class);
        verify(periodoDAO).cerrar(eq(4), eq(1), c.capture());
        DetalleRendimiento d = c.getValue().get(0);
        assertEquals(Calificativo.AD, d.calificativo()); // (3 + 4) / 2 = 3.5 → 4
        assertEquals(1, d.asistencia().faltas());
        assertEquals("LOGRADO", d.estadoAprobacion());
    }

    @Test
    void cerrarPeriodo_yaCerrado() {
        when(periodoDAO.buscar(3)).thenReturn(Optional.of(B3));
        assertThrows(BusinessException.class, () -> academico().cerrarPeriodo(DIRECTOR, B3));
    }

    @Test
    void reabrir_exigeMotivo() {
        when(periodoDAO.buscar(3)).thenReturn(Optional.of(B3));
        assertThrows(BusinessException.class, () -> academico().reabrirPeriodo(DIRECTOR, B3, "  "));
        academico().reabrirPeriodo(DIRECTOR, B3, "Corrección de notas");
        verify(periodoDAO).reabrir(3, 1, "Corrección de notas");
    }

    @Test
    void fechasDeBimestre_noSePuedenCruzar() {
        when(periodoDAO.listar()).thenReturn(List.of(B3, B4));
        assertThrows(BusinessException.class, () -> academico().actualizarFechas(DIRECTOR, B4,
                LocalDate.of(2026, 10, 1), LocalDate.of(2026, 12, 18)));
    }

    @Test
    void crearSeccion_letraInvalidaORepetida() {
        when(estructuraDAO.listarSecciones()).thenReturn(List.of(SECCION));
        var grado = new pe.edu.pitagoras.sga.model.academico.Grado(1, "1.°");
        assertThrows(BusinessException.class, () -> academico().crearSeccion(DIRECTOR, grado, "AB", "Mañana"));
        assertThrows(BusinessException.class, () -> academico().crearSeccion(DIRECTOR, grado, "a", "Mañana"));
    }
}
