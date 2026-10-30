package pe.edu.pitagoras.sga.service;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.edu.pitagoras.sga.dao.interfaces.IUsuarioDAO;
import pe.edu.pitagoras.sga.exception.AuthException;
import pe.edu.pitagoras.sga.exception.BusinessException;
import pe.edu.pitagoras.sga.exception.CuentaBloqueadaException;
import pe.edu.pitagoras.sga.model.auth.ResultadoLogin;
import pe.edu.pitagoras.sga.model.auth.Rol;
import pe.edu.pitagoras.sga.model.auth.Sesion;
import pe.edu.pitagoras.sga.model.auth.Usuario;
import pe.edu.pitagoras.sga.util.BCryptUtil;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import pe.edu.pitagoras.sga.service.AuthService;

/** Pruebas de AuthService con el DAO simulado (RF01, RF02). */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private static final int USUARIO_ID = 7;
    private static final String USERNAME = "docente1";
    private static final char[] CLAVE = "Clave#Segura1".toCharArray();

    // Reloj fijo: así el test controla el "ahora" y es repetible.
    private static final Clock RELOJ = Clock.fixed(Instant.parse("2026-10-29T15:00:00Z"), ZoneOffset.UTC);
    private static final LocalDateTime AHORA = LocalDateTime.now(RELOJ);

    private static String hashClave;

    @Mock
    private IUsuarioDAO dao;
    private AuthService service;

    @BeforeAll
    static void prepararHash() {
        hashClave = BCryptUtil.hashear(CLAVE); // costo 12 es lento: se calcula una sola vez
    }

    @BeforeEach
    void crearServicio() {
        service = new AuthService(dao, RELOJ);
    }

    private static Usuario usuario(Rol rol, boolean activo, boolean debeCambiar,
                                   int intentos, LocalDateTime bloqueadoHasta) {
        return new Usuario(USUARIO_ID, USERNAME, hashClave, rol, activo, debeCambiar, intentos, bloqueadoHasta);
    }

    // ---------- iniciarSesion ----------

    @Test
    void usuarioInexistente_lanzaAuthException() {
        when(dao.buscarPorUsername("fantasma")).thenReturn(Optional.empty());

        assertThrows(AuthException.class, () -> service.iniciarSesion("fantasma", CLAVE));

        verify(dao).registrarAcceso("fantasma", "FALLIDO");
        verify(dao, never()).actualizarIntentos(anyInt(), anyInt(), any());
    }

    @Test
    void cuentaDesactivada_lanzaAuthException() {
        when(dao.buscarPorUsername(USERNAME))
                .thenReturn(Optional.of(usuario(Rol.DOCENTE, false, false, 0, null)));

        assertThrows(AuthException.class, () -> service.iniciarSesion(USERNAME, CLAVE));

        verify(dao).registrarAcceso(USERNAME, "FALLIDO");
        verify(dao, never()).reiniciarIntentos(anyInt());
    }

    @Test
    void cuentaBloqueada_lanzaCuentaBloqueadaException_sinProbarLaClave() {
        LocalDateTime hasta = AHORA.plusMinutes(10);
        when(dao.buscarPorUsername(USERNAME))
                .thenReturn(Optional.of(usuario(Rol.DOCENTE, true, false, 0, hasta)));

        CuentaBloqueadaException ex = assertThrows(CuentaBloqueadaException.class,
                () -> service.iniciarSesion(USERNAME, CLAVE));

        assertEquals(hasta, ex.getBloqueadoHasta());
        verify(dao).registrarAcceso(USERNAME, "BLOQUEADO");
        verify(dao, never()).actualizarIntentos(anyInt(), anyInt(), any());
    }

    @Test
    void claveIncorrecta_sumaUnIntento() {
        when(dao.buscarPorUsername(USERNAME))
                .thenReturn(Optional.of(usuario(Rol.DOCENTE, true, false, 0, null)));

        AuthException ex = assertThrows(AuthException.class,
                () -> service.iniciarSesion(USERNAME, "Incorrecta#1".toCharArray()));

        assertFalse(ex instanceof CuentaBloqueadaException);
        verify(dao).actualizarIntentos(USUARIO_ID, 1, null);
        verify(dao).registrarAcceso(USERNAME, "FALLIDO");
    }

    @Test
    void tercerIntentoFallido_bloqueaQuinceMinutos() {
        when(dao.buscarPorUsername(USERNAME))
                .thenReturn(Optional.of(usuario(Rol.DOCENTE, true, false, 2, null)));

        CuentaBloqueadaException ex = assertThrows(CuentaBloqueadaException.class,
                () -> service.iniciarSesion(USERNAME, "Incorrecta#1".toCharArray()));

        assertEquals(AHORA.plusMinutes(15), ex.getBloqueadoHasta());
        verify(dao).actualizarIntentos(USUARIO_ID, 0, AHORA.plusMinutes(15));
        verify(dao).registrarAcceso(USERNAME, "BLOQUEADO");
    }

    @Test
    void bloqueoVencido_permiteIngresar() {
        when(dao.buscarPorUsername(USERNAME))
                .thenReturn(Optional.of(usuario(Rol.DOCENTE, true, false, 0, AHORA.minusMinutes(1))));

        ResultadoLogin resultado = service.iniciarSesion(USERNAME, CLAVE);

        assertEquals(Rol.DOCENTE, resultado.sesion().getRol());
        verify(dao).reiniciarIntentos(USUARIO_ID);
    }

    @Test
    void exito_reiniciaIntentosYDevuelveSesion() {
        when(dao.buscarPorUsername(USERNAME))
                .thenReturn(Optional.of(usuario(Rol.DOCENTE, true, false, 2, null)));

        ResultadoLogin resultado = service.iniciarSesion("  " + USERNAME + " ", CLAVE);

        assertEquals(USUARIO_ID, resultado.sesion().getUsuarioId());
        assertEquals(Rol.DOCENTE, resultado.sesion().getRol());
        assertNull(resultado.sesion().getAlumnoId());
        assertFalse(resultado.debeCambiarPassword());
        verify(dao).reiniciarIntentos(USUARIO_ID);
        verify(dao).registrarAcceso(USERNAME, "EXITOSO");
    }

    @Test
    void primerIngreso_exigeCambioDeClave() {
        when(dao.buscarPorUsername(USERNAME))
                .thenReturn(Optional.of(usuario(Rol.DIRECTOR, true, true, 0, null)));

        assertTrue(service.iniciarSesion(USERNAME, CLAVE).debeCambiarPassword());
    }

    @Test
    void apoderado_recibeSuAlumnoId() {
        when(dao.buscarPorUsername(USERNAME))
                .thenReturn(Optional.of(usuario(Rol.APODERADO, true, false, 0, null)));
        when(dao.buscarAlumnoIdPorUsuario(USUARIO_ID)).thenReturn(Optional.of(5));

        Sesion sesion = service.iniciarSesion(USERNAME, CLAVE).sesion();

        assertEquals(5, sesion.getAlumnoId());
    }

    @Test
    void apoderadoSinAlumno_noPuedeIngresar() {
        when(dao.buscarPorUsername(USERNAME))
                .thenReturn(Optional.of(usuario(Rol.APODERADO, true, false, 0, null)));
        when(dao.buscarAlumnoIdPorUsuario(USUARIO_ID)).thenReturn(Optional.empty());

        assertThrows(AuthException.class, () -> service.iniciarSesion(USERNAME, CLAVE));

        verify(dao).registrarAcceso(USERNAME, "FALLIDO");
        verify(dao, never()).reiniciarIntentos(anyInt());
    }

    @Test
    void datosVacios_noTocanLaBaseDeDatos() {
        assertThrows(AuthException.class, () -> service.iniciarSesion("   ", CLAVE));
        assertThrows(AuthException.class, () -> service.iniciarSesion(USERNAME, new char[0]));
        assertThrows(AuthException.class, () -> service.iniciarSesion(null, null));

        verifyNoInteractions(dao);
    }

    // ---------- cambiarPassword ----------

    @Test
    void cambiarPassword_guardaElNuevoHash() {
        Sesion sesion = new Sesion(USUARIO_ID, USERNAME, Rol.DOCENTE, null);
        when(dao.buscarPorUsername(USERNAME))
                .thenReturn(Optional.of(usuario(Rol.DOCENTE, true, true, 0, null)));
        char[] nueva = "NuevaClave#2026".toCharArray();

        service.cambiarPassword(sesion, CLAVE, nueva);

        ArgumentCaptor<String> hash = ArgumentCaptor.forClass(String.class);
        verify(dao).actualizarPassword(eq(USUARIO_ID), hash.capture());
        assertTrue(BCryptUtil.verificar(nueva, hash.getValue()));
        assertNotEquals("NuevaClave#2026", hash.getValue()); // nunca en texto plano
    }

    @Test
    void cambiarPassword_claveActualIncorrecta() {
        Sesion sesion = new Sesion(USUARIO_ID, USERNAME, Rol.DOCENTE, null);
        when(dao.buscarPorUsername(USERNAME))
                .thenReturn(Optional.of(usuario(Rol.DOCENTE, true, true, 0, null)));

        assertThrows(AuthException.class, () -> service.cambiarPassword(
                sesion, "Otra#Clave99".toCharArray(), "NuevaClave#2026".toCharArray()));

        verify(dao, never()).actualizarPassword(anyInt(), any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"abc", "soloLetrasLargas", "12345678"})
    void cambiarPassword_rechazaClavesDebiles(String debil) {
        Sesion sesion = new Sesion(USUARIO_ID, USERNAME, Rol.DOCENTE, null);

        assertThrows(BusinessException.class,
                () -> service.cambiarPassword(sesion, CLAVE, debil.toCharArray()));

        verifyNoInteractions(dao);
    }

    @Test
    void cambiarPassword_rechazaLaMismaClave() {
        Sesion sesion = new Sesion(USUARIO_ID, USERNAME, Rol.DOCENTE, null);

        assertThrows(BusinessException.class, () -> service.cambiarPassword(sesion, CLAVE, CLAVE.clone()));

        verifyNoInteractions(dao);
    }
}