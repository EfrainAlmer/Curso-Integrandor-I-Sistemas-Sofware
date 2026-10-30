package pe.edu.pitagoras.sga.service;

import com.google.common.base.Preconditions;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pe.edu.pitagoras.sga.dao.interfaces.IUsuarioDAO;
import pe.edu.pitagoras.sga.exception.AuthException;
import pe.edu.pitagoras.sga.exception.BusinessException;
import pe.edu.pitagoras.sga.exception.CuentaBloqueadaException;
import pe.edu.pitagoras.sga.model.auth.ResultadoLogin;
import pe.edu.pitagoras.sga.model.auth.Rol;
import pe.edu.pitagoras.sga.model.auth.Sesion;
import pe.edu.pitagoras.sga.model.auth.Usuario;
import pe.edu.pitagoras.sga.util.BCryptUtil;

/**
 * Reglas de autenticación (RF01) y de cambio de contraseña y bloqueo (RF02).
 * Recibe el DAO por constructor (DIP) y el reloj para poder probarlo con tiempo fijo.
 */
public class AuthService {

    private static final Logger LOG = LoggerFactory.getLogger(AuthService.class);

    static final int MAX_INTENTOS = 3;
    static final int MINUTOS_BLOQUEO = 15;
    private static final int LARGO_MINIMO_CLAVE = 8;

    private static final String EXITOSO = "EXITOSO";
    private static final String FALLIDO = "FALLIDO";
    private static final String BLOQUEADO = "BLOQUEADO";

    // Mismo mensaje para "usuario inexistente" y "clave incorrecta": no revela qué falló.
    private static final String MENSAJE_CREDENCIALES = "Usuario o contraseña incorrectos.";

    private final IUsuarioDAO usuarioDAO;
    private final Clock reloj;

    public AuthService(IUsuarioDAO usuarioDAO, Clock reloj) {
        this.usuarioDAO = Preconditions.checkNotNull(usuarioDAO, "usuarioDAO");
        this.reloj = Preconditions.checkNotNull(reloj, "reloj");
    }

    /**
     * @param username usuario escrito en el login.
     * @param clave    contraseña; quien llama debe borrarla de memoria después.
     * @return la sesión y si debe cambiar su clave.
     * @throws AuthException             credenciales inválidas, cuenta desactivada o apoderado sin alumno.
     * @throws CuentaBloqueadaException  cuenta bloqueada temporalmente.
     */
    public ResultadoLogin iniciarSesion(String username, char[] clave) {
        if (StringUtils.isBlank(username) || clave == null || clave.length == 0) {
            throw new AuthException("Ingrese su usuario y contraseña.");
        }
        String usuarioLimpio = username.trim();

        Optional<Usuario> encontrado = usuarioDAO.buscarPorUsername(usuarioLimpio);
        if (encontrado.isEmpty()) {
            usuarioDAO.registrarAcceso(usuarioLimpio, FALLIDO);
            LOG.warn("Intento de acceso con un usuario inexistente");
            throw new AuthException(MENSAJE_CREDENCIALES);
        }

        Usuario usuario = encontrado.get();
        LocalDateTime ahora = LocalDateTime.now(reloj);

        if (!usuario.isActivo()) {
            usuarioDAO.registrarAcceso(usuario.getUsername(), FALLIDO);
            throw new AuthException("La cuenta está desactivada. Comuníquese con Dirección.");
        }
        if (usuario.estaBloqueado(ahora)) {
            usuarioDAO.registrarAcceso(usuario.getUsername(), BLOQUEADO);
            throw new CuentaBloqueadaException(usuario.getBloqueadoHasta());
        }
        if (!BCryptUtil.verificar(clave, usuario.getPasswordHash())) {
            throw registrarFalloDeClave(usuario, ahora);
        }

        Integer alumnoId = null;
        if (usuario.getRol() == Rol.APODERADO) {
            alumnoId = usuarioDAO.buscarAlumnoIdPorUsuario(usuario.getUsuarioId()).orElse(null);
            if (alumnoId == null) {
                usuarioDAO.registrarAcceso(usuario.getUsername(), FALLIDO);
                LOG.error("Apoderado sin alumno asociado: usuarioId={}", usuario.getUsuarioId());
                throw new AuthException("La cuenta no tiene un estudiante asociado. Comuníquese con Dirección.");
            }
        }

        usuarioDAO.reiniciarIntentos(usuario.getUsuarioId());
        usuarioDAO.registrarAcceso(usuario.getUsername(), EXITOSO);
        LOG.info("Inicio de sesión: usuarioId={}, rol={}", usuario.getUsuarioId(), usuario.getRol());

        Sesion sesion = new Sesion(usuario.getUsuarioId(), usuario.getUsername(), usuario.getRol(), alumnoId);
        return new ResultadoLogin(sesion, usuario.isDebeCambiarPassword());
    }

    /**
     * @param sesion usuario que ya inició sesión.
     * @param actual contraseña vigente (o la temporal, en el primer ingreso).
     * @param nueva  contraseña nueva: 8 o más caracteres con letras y números, distinta de la actual.
     * @throws BusinessException si la nueva no cumple la política.
     * @throws AuthException     si la actual es incorrecta.
     */
    public void cambiarPassword(Sesion sesion, char[] actual, char[] nueva) {
        Preconditions.checkNotNull(sesion, "sesion");
        validarClaveNueva(nueva);
        if (actual == null || actual.length == 0) {
            throw new AuthException("Ingrese su contraseña actual.");
        }
        if (Arrays.equals(actual, nueva)) {
            throw new BusinessException("La nueva contraseña debe ser distinta de la actual.");
        }

        Usuario usuario = usuarioDAO.buscarPorUsername(sesion.getUsername())
                .orElseThrow(() -> new AuthException("No se encontró la cuenta."));
        if (!BCryptUtil.verificar(actual, usuario.getPasswordHash())) {
            throw new AuthException("La contraseña actual es incorrecta.");
        }

        usuarioDAO.actualizarPassword(usuario.getUsuarioId(), BCryptUtil.hashear(nueva));
        LOG.info("Contraseña cambiada: usuarioId={}", usuario.getUsuarioId());
    }

    /** Suma un fallo; al tercero bloquea la cuenta y reinicia el contador. */
    private AuthException registrarFalloDeClave(Usuario usuario, LocalDateTime ahora) {
        int intentos = usuario.getIntentosFallidos() + 1;
        if (intentos >= MAX_INTENTOS) {
            LocalDateTime hasta = ahora.plusMinutes(MINUTOS_BLOQUEO);
            usuarioDAO.actualizarIntentos(usuario.getUsuarioId(), 0, hasta);
            usuarioDAO.registrarAcceso(usuario.getUsername(), BLOQUEADO);
            LOG.warn("Cuenta bloqueada por intentos fallidos: usuarioId={}", usuario.getUsuarioId());
            return new CuentaBloqueadaException(hasta);
        }
        usuarioDAO.actualizarIntentos(usuario.getUsuarioId(), intentos, null);
        usuarioDAO.registrarAcceso(usuario.getUsername(), FALLIDO);
        return new AuthException(MENSAJE_CREDENCIALES);
    }

    private static void validarClaveNueva(char[] clave) {
        boolean valida = clave != null && clave.length >= LARGO_MINIMO_CLAVE;
        if (valida) {
            boolean hayLetra = false;
            boolean hayNumero = false;
            for (char c : clave) {
                hayLetra |= Character.isLetter(c);
                hayNumero |= Character.isDigit(c);
            }
            valida = hayLetra && hayNumero;
        }
        if (!valida) {
            throw new BusinessException("La nueva contraseña debe tener al menos "
                    + LARGO_MINIMO_CLAVE + " caracteres, con letras y números.");
        }
    }
}