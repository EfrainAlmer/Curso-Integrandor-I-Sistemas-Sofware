package pe.edu.pitagoras.sga.view.comun;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import javax.swing.JSpinner;
import javax.swing.SpinnerDateModel;

/** Selector de fecha (JSpinner con formato dd/MM/yyyy) y formatos de fecha comunes. */
public final class Fechas {

    public static final DateTimeFormatter DIA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    public static final DateTimeFormatter DIA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    public static final DateTimeFormatter DIA_SEMANA =
            DateTimeFormatter.ofPattern("EEE dd/MM/yyyy", java.util.Locale.forLanguageTag("es-PE"));

    private Fechas() {
    }

    public static JSpinner selector(LocalDate inicial) {
        JSpinner s = new JSpinner(new SpinnerDateModel());
        s.setEditor(new JSpinner.DateEditor(s, "dd/MM/yyyy"));
        poner(s, inicial);
        return s;
    }

    public static LocalDate leer(JSpinner s) {
        return ((Date) s.getValue()).toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }

    public static void poner(JSpinner s, LocalDate fecha) {
        s.setValue(Date.from(fecha.atStartOfDay(ZoneId.systemDefault()).toInstant()));
    }

    public static String texto(LocalDate f) {
        return f == null ? "" : f.format(DIA);
    }

    public static String texto(LocalDateTime f) {
        return f == null ? "" : f.format(DIA_HORA);
    }
}
