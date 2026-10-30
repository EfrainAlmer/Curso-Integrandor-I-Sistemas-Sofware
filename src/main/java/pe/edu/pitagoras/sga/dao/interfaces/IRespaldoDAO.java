package pe.edu.pitagoras.sga.dao.interfaces;

import java.util.List;
import pe.edu.pitagoras.sga.model.gestion.CopiaSeguridad;

/** Copias de seguridad de la base de datos (RF10). */
public interface IRespaldoDAO {

    List<CopiaSeguridad> listar();

    /**
     * Ejecuta BACKUP DATABASE en el servidor y registra el resultado.
     *
     * @param carpeta carpeta del servidor donde se guarda el .bak.
     * @return la copia registrada.
     */
    CopiaSeguridad generar(String carpeta, int usuarioId);

    /** Restaura un .bak del servidor sobre la base actual (desconecta a todos los usuarios). */
    void restaurar(String ruta, int usuarioId);
}
