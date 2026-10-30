package pe.edu.pitagoras.sga.service;

import com.google.common.base.Preconditions;
import java.util.List;
import java.util.Map;
import pe.edu.pitagoras.sga.dao.interfaces.ICargaAcademicaDAO;
import pe.edu.pitagoras.sga.model.academico.Curso;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pe.edu.pitagoras.sga.dao.interfaces.ICuentaDAO;
import pe.edu.pitagoras.sga.exception.BusinessException;
import pe.edu.pitagoras.sga.model.academico.Docente;
import pe.edu.pitagoras.sga.model.auth.Rol;
import pe.edu.pitagoras.sga.model.auth.Sesion;
import pe.edu.pitagoras.sga.model.gestion.Credencial;
import pe.edu.pitagoras.sga.model.gestion.CuentaUsuario;
import pe.edu.pitagoras.sga.util.BCryptUtil;
import pe.edu.pitagoras.sga.util.GeneradorClave;
import pe.edu.pitagoras.sga.util.Validaciones;

/**
 * Gestión de usuarios por la Dirección (RF03): crear, desactivar y restablecer claves.
 * Las claves nuevas son temporales, se guardan solo como hash BCrypt (RNF02) y se exige
 * cambiarlas en el primer ingreso (RF02).
 */
public class UsuarioService {

    private static final Logger LOG = LoggerFactory.getLogger(UsuarioService.class);

    /** Un docente puede tener como máximo 3 áreas de especialidad. */
    static final int MAX_AREAS = 3;

    private final ICuentaDAO cuentaDAO;
    private final ICargaAcademicaDAO cargaDAO;

    public UsuarioService(ICuentaDAO cuentaDAO, ICargaAcademicaDAO cargaDAO) {
        this.cuentaDAO = Preconditions.checkNotNull(cuentaDAO);
        this.cargaDAO = Preconditions.checkNotNull(cargaDAO);
    }

    /** @return usuarioId del docente → áreas que puede dictar. */
    public Map<Integer, List<Curso>> areasDocentes(Sesion sesion) {
        Permisos.exigir(sesion, Rol.DIRECTOR);
        return cuentaDAO.areasPorUsuario();
    }

    public List<CuentaUsuario> listar(Sesion sesion) {
        Permisos.exigir(sesion, Rol.DIRECTOR);
        return cuentaDAO.listar();
    }

    public List<Docente> docentes(Sesion sesion) {
        Permisos.exigir(sesion, Rol.DIRECTOR);
        return cuentaDAO.listarDocentes();
    }

    /** @return nombre de la persona para la cabecera; el username si no tiene ficha. */
    public String nombreParaMostrar(Sesion sesion) {
        Preconditions.checkNotNull(sesion);
        return cuentaDAO.nombreParaMostrar(sesion.getUsuarioId()).orElse(sesion.getUsername());
    }

    public Credencial crearDocente(Sesion sesion, String username, String dni, String nombre, String apellido,
                                   String email, List<Curso> areas) {
        Permisos.exigir(sesion, Rol.DIRECTOR);
        String user = usuarioDisponible(username);
        Docente datos = new Docente(0, 0, Validaciones.dni(dni), Validaciones.nombre(nombre, "el nombre", 50),
                Validaciones.nombre(apellido, "el apellido", 50), Validaciones.emailOpcional(email));
        String clave = GeneradorClave.generar();
        cuentaDAO.crearDocente(user, BCryptUtil.hashear(clave.toCharArray()), datos, validarAreas(areas),
                sesion.getUsuarioId());
        LOG.info("Cuenta de docente creada: {}", user);
        return new Credencial(user, clave);
    }

    /** @return la ficha del docente de la cuenta (para editarla). */
    public Docente docenteDeCuenta(Sesion sesion, CuentaUsuario cuenta) {
        Permisos.exigir(sesion, Rol.DIRECTOR);
        return cuentaDAO.buscarDocentePorUsuario(cuenta.usuarioId())
                .orElseThrow(() -> new BusinessException("Solo se pueden editar aquí los datos de los docentes. "
                        + "Los datos del apoderado se editan en Inscripción de alumnos."));
    }

    /** RF03: modifica los datos del docente. */
    public void actualizarDocente(Sesion sesion, Docente actual, String dni, String nombre, String apellido,
                                  String email, List<Curso> areas) {
        Permisos.exigir(sesion, Rol.DIRECTOR);
        Docente nuevo = new Docente(actual.docenteId(), actual.usuarioId(), Validaciones.dni(dni),
                Validaciones.nombre(nombre, "el nombre", 50), Validaciones.nombre(apellido, "el apellido", 50),
                Validaciones.emailOpcional(email));
        boolean dniOcupado = cuentaDAO.listarDocentes().stream()
                .anyMatch(d -> d.docenteId() != actual.docenteId() && d.dni().equals(nuevo.dni()));
        if (dniOcupado) {
            throw new BusinessException("Otro docente ya tiene el DNI " + nuevo.dni() + ".");
        }
        List<Integer> ids = validarAreas(areas);
        // No se puede quitar un área que el docente dicta actualmente.
        for (var carga : cargaDAO.listarPorDocente(actual.docenteId())) {
            if (!ids.contains(carga.curso().cursoId())) {
                throw new BusinessException("El docente dicta " + carga.curso().nombre() + " en "
                        + carga.seccion().etiqueta() + ". Reasigne esa carga antes de quitarle el área.");
            }
        }
        cuentaDAO.actualizarDocente(nuevo, ids, sesion.getUsuarioId());
    }

    public Credencial crearDirector(Sesion sesion, String username) {
        Permisos.exigir(sesion, Rol.DIRECTOR);
        String user = usuarioDisponible(username);
        String clave = GeneradorClave.generar();
        cuentaDAO.crearDirector(user, BCryptUtil.hashear(clave.toCharArray()), sesion.getUsuarioId());
        LOG.info("Cuenta de dirección creada: {}", user);
        return new Credencial(user, clave);
    }

    /** RF03: activa o desactiva una cuenta. Nadie puede desactivarse a sí mismo. */
    public void cambiarEstado(Sesion sesion, CuentaUsuario cuenta, boolean activo) {
        Permisos.exigir(sesion, Rol.DIRECTOR);
        if (!activo && cuenta.usuarioId() == sesion.getUsuarioId()) {
            throw new BusinessException("No puede desactivar su propia cuenta.");
        }
        cuentaDAO.cambiarEstado(cuenta.usuarioId(), activo, sesion.getUsuarioId());
    }

    /** @return la nueva clave temporal, para entregarla en persona. */
    public Credencial restablecerClave(Sesion sesion, CuentaUsuario cuenta) {
        Permisos.exigir(sesion, Rol.DIRECTOR);
        String clave = GeneradorClave.generar();
        cuentaDAO.restablecerClave(cuenta.usuarioId(), BCryptUtil.hashear(clave.toCharArray()), sesion.getUsuarioId());
        LOG.info("Clave restablecida: usuarioId={}", cuenta.usuarioId());
        return new Credencial(cuenta.username(), clave);
    }

    /** Especialidad: de 1 a 3 áreas distintas. */
    private static List<Integer> validarAreas(List<Curso> areas) {
        List<Integer> ids = areas == null ? List.of()
                : areas.stream().map(Curso::cursoId).distinct().toList();
        if (ids.isEmpty() || ids.size() > MAX_AREAS) {
            throw new BusinessException("Marque de 1 a " + MAX_AREAS + " áreas que el docente puede dictar.");
        }
        return ids;
    }

    private String usuarioDisponible(String username) {
        String user = Validaciones.usuario(username);
        if (cuentaDAO.existeUsername(user)) {
            throw new BusinessException("El usuario '" + user + "' ya existe.");
        }
        return user;
    }
}
