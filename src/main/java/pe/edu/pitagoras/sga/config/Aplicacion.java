package pe.edu.pitagoras.sga.config;

import java.time.Clock;
import pe.edu.pitagoras.sga.controller.ApoderadoController;
import pe.edu.pitagoras.sga.controller.AuthController;
import pe.edu.pitagoras.sga.controller.DirectorController;
import pe.edu.pitagoras.sga.controller.DocenteController;
import pe.edu.pitagoras.sga.dao.impl.AlumnoDAOImpl;
import pe.edu.pitagoras.sga.dao.impl.AsistenciaDAOImpl;
import pe.edu.pitagoras.sga.dao.impl.AuditoriaDAOImpl;
import pe.edu.pitagoras.sga.dao.impl.CargaAcademicaDAOImpl;
import pe.edu.pitagoras.sga.dao.impl.ConclusionDAOImpl;
import pe.edu.pitagoras.sga.dao.interfaces.IConclusionDAO;
import pe.edu.pitagoras.sga.model.gestion.DatosColegio;
import pe.edu.pitagoras.sga.dao.impl.CuentaDAOImpl;
import pe.edu.pitagoras.sga.dao.impl.EstructuraDAOImpl;
import pe.edu.pitagoras.sga.dao.impl.NotaDAOImpl;
import pe.edu.pitagoras.sga.dao.impl.PeriodoDAOImpl;
import pe.edu.pitagoras.sga.dao.impl.RespaldoDAOImpl;
import pe.edu.pitagoras.sga.dao.impl.UsuarioDAOImpl;
import pe.edu.pitagoras.sga.dao.interfaces.IAlumnoDAO;
import pe.edu.pitagoras.sga.dao.interfaces.IAsistenciaDAO;
import pe.edu.pitagoras.sga.dao.interfaces.ICargaAcademicaDAO;
import pe.edu.pitagoras.sga.dao.interfaces.ICuentaDAO;
import pe.edu.pitagoras.sga.dao.interfaces.IEstructuraDAO;
import pe.edu.pitagoras.sga.dao.interfaces.INotaDAO;
import pe.edu.pitagoras.sga.dao.interfaces.IPeriodoDAO;
import pe.edu.pitagoras.sga.model.auth.Sesion;
import pe.edu.pitagoras.sga.service.AcademicoService;
import pe.edu.pitagoras.sga.service.AlumnoService;
import pe.edu.pitagoras.sga.service.ApoderadoService;
import pe.edu.pitagoras.sga.service.AuditoriaService;
import pe.edu.pitagoras.sga.service.AuthService;
import pe.edu.pitagoras.sga.service.DocenteService;
import pe.edu.pitagoras.sga.service.ReporteService;
import pe.edu.pitagoras.sga.service.UsuarioService;

/**
 * Raíz de composición: aquí, y solo aquí, se crean los DAO concretos y se inyectan
 * por constructor en los services (inversión de dependencias, SOLID). Las vistas reciben
 * controladores ya armados.
 */
public final class Aplicacion {

    private final AuthController authController;
    private final UsuarioService usuarioService;
    private final DocenteService docenteService;
    private final ApoderadoService apoderadoService;
    private final AlumnoService alumnoService;
    private final AcademicoService academicoService;
    private final ReporteService reporteService;
    private final AuditoriaService auditoriaService;

    public Aplicacion(AppConfig config) {
        Clock reloj = Clock.systemDefaultZone();
        IEstructuraDAO estructura = new EstructuraDAOImpl();
        IPeriodoDAO periodos = new PeriodoDAOImpl();
        ICargaAcademicaDAO cargas = new CargaAcademicaDAOImpl();
        IAlumnoDAO alumnos = new AlumnoDAOImpl();
        ICuentaDAO cuentas = new CuentaDAOImpl();
        INotaDAO notas = new NotaDAOImpl();
        IAsistenciaDAO asistencia = new AsistenciaDAOImpl();
        IConclusionDAO conclusiones = new ConclusionDAOImpl();
        DatosColegio colegio = new DatosColegio(
                config.obtenerOpcional("colegio.nombre", DatosColegio.POR_DEFECTO.nombre()),
                config.obtenerOpcional("colegio.dre", DatosColegio.POR_DEFECTO.dre()),
                config.obtenerOpcional("colegio.ugel", DatosColegio.POR_DEFECTO.ugel()),
                config.obtenerOpcional("colegio.codigoModular", DatosColegio.POR_DEFECTO.codigoModular()),
                config.obtenerOpcional("colegio.lugar", DatosColegio.POR_DEFECTO.lugar()));

        this.authController = new AuthController(new AuthService(new UsuarioDAOImpl(), reloj));
        this.usuarioService = new UsuarioService(cuentas, cargas);
        this.reporteService = new ReporteService(alumnos, notas, asistencia, periodos, cargas, estructura,
                conclusiones, colegio, reloj);
        this.docenteService = new DocenteService(cargas, alumnos, periodos, estructura, notas, asistencia,
                conclusiones, reloj);
        this.apoderadoService = new ApoderadoService(alumnos, notas, asistencia, periodos, cargas, estructura,
                conclusiones, reporteService);
        this.alumnoService = new AlumnoService(alumnos, cuentas);
        this.academicoService = new AcademicoService(estructura, cargas, periodos, notas, asistencia, cuentas);
        this.auditoriaService = new AuditoriaService(new AuditoriaDAOImpl(), new RespaldoDAOImpl(),
                config.obtenerOpcional("backup.carpeta", ""));
    }

    public AuthController authController() {
        return authController;
    }

    /** @return nombre de la persona de la sesión para la cabecera. */
    public String nombreParaMostrar(Sesion sesion) {
        return usuarioService.nombreParaMostrar(sesion);
    }

    public DocenteController docenteController(Sesion sesion) {
        return new DocenteController(docenteService, reporteService, sesion);
    }

    public ApoderadoController apoderadoController(Sesion sesion) {
        return new ApoderadoController(apoderadoService, sesion);
    }

    public DirectorController directorController(Sesion sesion) {
        return new DirectorController(usuarioService, alumnoService, academicoService, reporteService,
                auditoriaService, sesion);
    }
}
