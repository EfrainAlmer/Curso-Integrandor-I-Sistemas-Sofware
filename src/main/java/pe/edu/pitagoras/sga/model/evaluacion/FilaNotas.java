package pe.edu.pitagoras.sga.model.evaluacion;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Una fila de la grilla de notas: un alumno y su calificativo en cada competencia del área.
 * Es mutable porque la vista la edita antes de guardar.
 */
public class FilaNotas {

    private final int alumnoId;
    private final String dni;
    private final String alumno;
    private final Map<String, Calificativo> valores = new LinkedHashMap<>();
    private final Map<String, Calificativo> originales = new LinkedHashMap<>();

    public FilaNotas(int alumnoId, String dni, String alumno) {
        this.alumnoId = alumnoId;
        this.dni = dni;
        this.alumno = alumno;
    }

    /** Carga el valor que ya estaba en la BD (no cuenta como cambio). */
    public void cargar(String codigo, Calificativo valor) {
        valores.put(codigo, valor);
        originales.put(codigo, valor);
    }

    /** Cambia el valor desde la vista; null borra lo escrito (no se guarda). */
    public void poner(String codigo, Calificativo valor) {
        if (valor == null) {
            valores.remove(codigo);
        } else {
            valores.put(codigo, valor);
        }
    }

    public Calificativo get(String codigo) {
        return valores.get(codigo);
    }

    /** @return el valor guardado en la BD para la competencia (null si no tenía). */
    public Calificativo getOriginal(String codigo) {
        return originales.get(codigo);
    }

    /** @return true si algún valor difiere de lo guardado. */
    public boolean tieneCambios() {
        return !valores.equals(originales);
    }

    /** Descarta lo editado y vuelve a lo guardado. */
    public void descartar() {
        valores.clear();
        valores.putAll(originales);
    }

    /** Marca lo editado como guardado. */
    public void confirmar() {
        originales.clear();
        originales.putAll(valores);
    }

    /** @return calificativo del área según las competencias llenas, o vacío. */
    public Optional<Calificativo> calificativoArea() {
        return ReglaCalificativo.calificativoArea(valores.values());
    }

    public int getAlumnoId() {
        return alumnoId;
    }

    public String getDni() {
        return dni;
    }

    public String getAlumno() {
        return alumno;
    }

    public Map<String, Calificativo> getValores() {
        return Collections.unmodifiableMap(valores);
    }
}
