package pe.edu.pitagoras.sga.service;

import com.google.common.base.Preconditions;
import java.time.LocalDate;
import java.util.List;
import pe.edu.pitagoras.sga.dao.interfaces.IAuditoriaDAO;
import pe.edu.pitagoras.sga.dao.interfaces.IRespaldoDAO;
import pe.edu.pitagoras.sga.exception.BusinessException;
import pe.edu.pitagoras.sga.model.auth.Rol;
import pe.edu.pitagoras.sga.model.auth.Sesion;
import pe.edu.pitagoras.sga.model.gestion.CopiaSeguridad;
import pe.edu.pitagoras.sga.model.gestion.EventoAuditoria;

/** Bitácora de auditoría (RF09) y copias de seguridad (RF10). Solo Dirección. */
public class AuditoriaService {

    private static final int MAX_EVENTOS = 500;

    private final IAuditoriaDAO auditoriaDAO;
    private final IRespaldoDAO respaldoDAO;
    private final String carpetaRespaldo;

    /**
     * @param carpetaRespaldo carpeta del servidor para los .bak; vacía usa la carpeta por defecto de SQL Server.
     */
    public AuditoriaService(IAuditoriaDAO auditoriaDAO, IRespaldoDAO respaldoDAO, String carpetaRespaldo) {
        this.auditoriaDAO = Preconditions.checkNotNull(auditoriaDAO);
        this.respaldoDAO = Preconditions.checkNotNull(respaldoDAO);
        this.carpetaRespaldo = carpetaRespaldo;
    }

    public List<EventoAuditoria> eventos(Sesion sesion, LocalDate desde, LocalDate hasta, String accion) {
        Permisos.exigir(sesion, Rol.DIRECTOR);
        if (desde == null || hasta == null || hasta.isBefore(desde)) {
            throw new BusinessException("Revise el rango de fechas.");
        }
        return auditoriaDAO.listar(desde, hasta, accion, MAX_EVENTOS);
    }

    public List<CopiaSeguridad> copias(Sesion sesion) {
        Permisos.exigir(sesion, Rol.DIRECTOR);
        return respaldoDAO.listar();
    }

    public CopiaSeguridad generarCopia(Sesion sesion) {
        Permisos.exigir(sesion, Rol.DIRECTOR);
        return respaldoDAO.generar(carpetaRespaldo, sesion.getUsuarioId());
    }

    public void restaurar(Sesion sesion, CopiaSeguridad copia) {
        Permisos.exigir(sesion, Rol.DIRECTOR);
        if (copia == null || !"EXITOSO".equals(copia.estado())) {
            throw new BusinessException("Seleccione una copia generada correctamente.");
        }
        respaldoDAO.restaurar(copia.ruta(), sesion.getUsuarioId());
    }
}
