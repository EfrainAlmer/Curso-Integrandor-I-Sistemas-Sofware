package pe.edu.pitagoras.sga.controller;

import com.google.common.base.Preconditions;
import java.io.File;
import java.util.List;
import pe.edu.pitagoras.sga.model.academico.Alumno;
import pe.edu.pitagoras.sga.model.academico.Periodo;
import pe.edu.pitagoras.sga.model.auth.Sesion;
import pe.edu.pitagoras.sga.model.evaluacion.AsistenciaRegistro;
import pe.edu.pitagoras.sga.model.evaluacion.FilaConsultaNota;
import pe.edu.pitagoras.sga.model.evaluacion.Libreta;
import pe.edu.pitagoras.sga.service.ApoderadoService;

/** Controlador del módulo Alumno/Apoderado (solo lectura). */
public class ApoderadoController {

    private final ApoderadoService apoderadoService;
    private final Sesion sesion;

    public ApoderadoController(ApoderadoService apoderadoService, Sesion sesion) {
        this.apoderadoService = Preconditions.checkNotNull(apoderadoService);
        this.sesion = Preconditions.checkNotNull(sesion);
    }

    public Alumno miAlumno() {
        return apoderadoService.miAlumno(sesion);
    }

    public List<Periodo> periodos() {
        return apoderadoService.periodos(sesion);
    }

    public List<FilaConsultaNota> notas(Periodo periodo) {
        return apoderadoService.notas(sesion, periodo.periodoId());
    }

    /** @param periodo bimestre, o null para todo el año. */
    public List<AsistenciaRegistro> asistencia(Periodo periodo) {
        return apoderadoService.asistencia(sesion, periodo == null ? null : periodo.periodoId());
    }

    public Libreta libreta() {
        return apoderadoService.libreta(sesion);
    }

    public File exportarLibreta(File destino) {
        return apoderadoService.exportarLibreta(sesion, destino);
    }
}
