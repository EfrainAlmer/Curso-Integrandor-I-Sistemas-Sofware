package pe.edu.pitagoras.sga.model.evaluacion;

import java.util.Locale;
import java.util.Optional;

/** Escala literal del MINEDU (RF13). El orden va del logro más alto al más bajo. */
public enum Calificativo {
    AD(4, "Logro destacado"),
    A(3, "Logro esperado"),
    B(2, "En proceso"),
    C(1, "En inicio");

    private final int puntos;
    private final String descripcion;

    Calificativo(int puntos, String descripcion) {
        this.puntos = puntos;
        this.descripcion = descripcion;
    }

    /** @return puntos usados para consolidar el área (AD=4 ... C=1). */
    public int getPuntos() {
        return puntos;
    }

    public String getDescripcion() {
        return descripcion;
    }

    /**
     * @param texto lo que escribió el usuario o lo que vino de la BD ("ad", " A ", ...).
     * @return el calificativo, o vacío si el texto no es AD, A, B ni C.
     */
    public static Optional<Calificativo> desdeTexto(String texto) {
        if (texto == null) {
            return Optional.empty();
        }
        String limpio = texto.trim().toUpperCase(Locale.ROOT);
        for (Calificativo c : values()) {
            if (c.name().equals(limpio)) {
                return Optional.of(c);
            }
        }
        return Optional.empty();
    }

    /** @return el calificativo que corresponde a un puntaje redondeado de 1 a 4. */
    static Calificativo desdePuntos(int puntos) {
        for (Calificativo c : values()) {
            if (c.puntos == puntos) {
                return c;
            }
        }
        return C;
    }
}
