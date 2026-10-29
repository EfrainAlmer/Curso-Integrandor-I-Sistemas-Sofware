package pe.edu.utp.pitagoras.dao;

import pe.edu.utp.pitagoras.model.CargaAcademica;
import pe.edu.utp.pitagoras.model.CurriculoSecundaria;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Implementación en memoria de {@link ICargaAcademicaDAO} (modo demostración, sin SQL Server).
 * Una sola instancia compartida para que Director y Docente vean los mismos datos.
 */
public class CargaAcademicaDAOMemoria implements ICargaAcademicaDAO {

    private static final CargaAcademicaDAOMemoria INSTANCIA = new CargaAcademicaDAOMemoria();
    private static final int ANIO = 2026;

    private final List<CargaAcademica> cargas = new ArrayList<>();

    public static CargaAcademicaDAOMemoria getInstancia() { return INSTANCIA; }

    private CargaAcademicaDAOMemoria() { sembrarDatosDemo(); }

    @Override
    public synchronized List<CargaAcademica> listarTodas() {
        return Collections.unmodifiableList(new ArrayList<>(cargas));
    }

    @Override
    public synchronized List<CargaAcademica> listarPorDocente(String docenteUsername) {
        List<CargaAcademica> r = new ArrayList<>();
        for (CargaAcademica c : cargas) {
            if (c.getDocenteUsername() != null && c.getDocenteUsername().equalsIgnoreCase(docenteUsername)) r.add(c);
        }
        return Collections.unmodifiableList(r);
    }

    @Override
    public synchronized void guardar(CargaAcademica carga) { cargas.add(carga); }

    @Override
    public synchronized boolean eliminar(String codigoCarga) {
        return cargas.removeIf(c -> c.getCodigoCarga().equals(codigoCarga));
    }

    @Override
    public synchronized String siguienteCodigo() {
        int max = 0;
        for (CargaAcademica c : cargas) {
            try { max = Math.max(max, Integer.parseInt(c.getCodigoCarga().substring(c.getCodigoCarga().lastIndexOf('-') + 1))); }
            catch (NumberFormatException ignorada) { /* código con otro formato */ }
        }
        return String.format("CA-%d-%02d", ANIO, max + 1);
    }

    /** Competencias del área, tomadas del currículo (fuente única). */
    public static String[] competenciasDe(String area) {
        return CurriculoSecundaria.AREAS.stream()
                .filter(a -> a.nombre().equals(area)).findFirst()
                .map(a -> a.competencias().toArray(new String[0]))
                .orElse(new String[0]);
    }

    // Misma carga que antes estaba escrita dentro de DocenteController.
    private void sembrarDatosDemo() {
        String[] grados = {"1.°", "2.°", "3.°", "4.°", "5.°"};
        for (String g : grados) {
            sembrar(g, "Matemática", "docente", "Prof. Carlos Mendoza");
            sembrar(g, "Ciencia y Tecnología", "docente", "Prof. Carlos Mendoza");
        }
        for (String g : grados) {
            sembrar(g, "Comunicación", "docente2", "Prof. María Quispe");
            sembrar(g, "Ciencias Sociales", "docente2", "Prof. María Quispe");
        }
    }

    private void sembrar(String grado, String area, String username, String nombre) {
        cargas.add(new CargaAcademica(siguienteCodigo(), ANIO, grado, "A", CurriculoSecundaria.TURNO,
                area, username, nombre, competenciasDe(area)));
    }
}