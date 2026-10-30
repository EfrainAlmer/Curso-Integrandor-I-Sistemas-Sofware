package pe.edu.pitagoras.sga.service;

import com.google.common.base.Preconditions;
import java.util.List;
import pe.edu.pitagoras.sga.dao.interfaces.IAlumnoDAO;
import pe.edu.pitagoras.sga.dao.interfaces.ICuentaDAO;
import pe.edu.pitagoras.sga.exception.BusinessException;
import pe.edu.pitagoras.sga.model.academico.Alumno;
import pe.edu.pitagoras.sga.model.academico.Seccion;
import pe.edu.pitagoras.sga.model.auth.Rol;
import pe.edu.pitagoras.sga.model.auth.Sesion;
import pe.edu.pitagoras.sga.model.gestion.Credencial;
import pe.edu.pitagoras.sga.util.BCryptUtil;
import pe.edu.pitagoras.sga.util.GeneradorClave;
import pe.edu.pitagoras.sga.util.Validaciones;

/**
 * Registro de estudiantes e inscripción en el año (RF04). Al inscribir se genera la cuenta
 * del apoderado (RF03) con el usuario "apo" + DNI del alumno.
 */
public class AlumnoService {

    static final String PREFIJO_APODERADO = "apo";

    private final IAlumnoDAO alumnoDAO;
    private final ICuentaDAO cuentaDAO;

    public AlumnoService(IAlumnoDAO alumnoDAO, ICuentaDAO cuentaDAO) {
        this.alumnoDAO = Preconditions.checkNotNull(alumnoDAO);
        this.cuentaDAO = Preconditions.checkNotNull(cuentaDAO);
    }

    public List<Alumno> listar(Sesion sesion, Integer seccionId, boolean soloActivos) {
        Permisos.exigir(sesion, Rol.DIRECTOR);
        return alumnoDAO.listar(seccionId, soloActivos);
    }

    /** @return la cuenta del apoderado con su clave temporal. */
    public Credencial inscribir(Sesion sesion, Seccion seccion, String dni, String nombre, String apellido,
                                String apoderado, String telefono) {
        Permisos.exigir(sesion, Rol.DIRECTOR);
        Alumno a = validar(0, seccion, dni, nombre, apellido, apoderado, telefono);
        String usuario = PREFIJO_APODERADO + a.dni();
        if (cuentaDAO.existeUsername(usuario)) {
            throw new BusinessException("Ya existe la cuenta " + usuario + ".");
        }
        String clave = GeneradorClave.generar();
        alumnoDAO.inscribir(a, usuario, BCryptUtil.hashear(clave.toCharArray()), sesion.getUsuarioId());
        return new Credencial(usuario, clave);
    }

    public void actualizar(Sesion sesion, int alumnoId, Seccion seccion, String dni, String nombre, String apellido,
                           String apoderado, String telefono) {
        Permisos.exigir(sesion, Rol.DIRECTOR);
        alumnoDAO.actualizar(validar(alumnoId, seccion, dni, nombre, apellido, apoderado, telefono),
                sesion.getUsuarioId());
    }

    /** Baja lógica (RF04): el alumno queda RETIRADO y su apoderado ya no puede ingresar. */
    public void cambiarEstado(Sesion sesion, Alumno alumno, boolean activo) {
        Permisos.exigir(sesion, Rol.DIRECTOR);
        alumnoDAO.cambiarEstado(alumno.alumnoId(), activo ? Alumno.ACTIVO : Alumno.RETIRADO, sesion.getUsuarioId());
    }

    private Alumno validar(int alumnoId, Seccion seccion, String dni, String nombre, String apellido,
                           String apoderado, String telefono) {
        if (seccion == null) {
            throw new BusinessException("Seleccione el grado y la sección.");
        }
        String dniLimpio = Validaciones.dni(dni);
        if (alumnoDAO.existeDni(dniLimpio, alumnoId)) {
            throw new BusinessException("Ya existe un alumno con el DNI " + dniLimpio + ".");
        }
        return new Alumno(alumnoId, seccion, dniLimpio, Validaciones.nombre(nombre, "los nombres", 50),
                Validaciones.nombre(apellido, "los apellidos", 50),
                Validaciones.nombre(apoderado, "el nombre del apoderado", 100),
                Validaciones.telefonoOpcional(telefono), Alumno.ACTIVO, null);
    }
}
