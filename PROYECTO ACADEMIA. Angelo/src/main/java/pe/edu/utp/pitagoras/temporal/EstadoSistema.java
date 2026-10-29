package pe.edu.utp.pitagoras.temporal;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Consumer;

/**
 * Estado compartido en memoria para el prototipo (sin base de datos todavía).
 * Cuando exista la capa DAO/SQL Server, cada sección de esta clase se reemplaza
 * por su DAO y los paneles no necesitan cambiar.
 *
 *  - Sesión: usuario autenticado.
 *  - Periodos: los 4 bimestres (RF07), compartidos entre Periodos, Reportes y el Dashboard.
 *  - Bitácora: eventos de auditoría (RF09) que generan los demás paneles.
 *  - Credenciales: contraseñas con hash + sal (RF02 / RNF02).
 */
/** TEMPORAL: se elimina en la fase 2. */
@Deprecated
public final class EstadoSistema {

    private EstadoSistema() { }

    public static final DateTimeFormatter FMT_FECHA      = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    public static final DateTimeFormatter FMT_FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    // ══ Sesión ════════════════════════════════════════════════════
    private static String usuarioActual = "dir_pitagoras";

    public static String getUsuarioActual()            { return usuarioActual; }
    public static void setUsuarioActual(String usuario) { usuarioActual = usuario; }

    // ══ Periodos ══════════════════════════════════════════════════
    public static final String ABIERTO   = "Abierto";
    public static final String CERRADO   = "Cerrado";
    public static final String PENDIENTE = "Pendiente";

    public static final class Periodo {
        public final int id;
        public final String nombre;
        public final LocalDate inicio;
        public final LocalDate fin;
        public String estado;
        public String fechaCierre = "—";
        public String cerradoPor  = "—";

        Periodo(int id, String nombre, LocalDate inicio, LocalDate fin, String estado) {
            this.id = id; this.nombre = nombre; this.inicio = inicio; this.fin = fin; this.estado = estado;
        }
    }

    private static final List<Periodo> PERIODOS = new ArrayList<>();
    private static final List<Runnable> OYENTES_PERIODOS = new ArrayList<>();

    static {
        PERIODOS.add(new Periodo(1, "Bimestre 1", LocalDate.of(2026, 3, 9),   LocalDate.of(2026, 5, 15),  CERRADO));
        PERIODOS.add(new Periodo(2, "Bimestre 2", LocalDate.of(2026, 5, 18),  LocalDate.of(2026, 7, 24),  ABIERTO));
        PERIODOS.add(new Periodo(3, "Bimestre 3", LocalDate.of(2026, 8, 10),  LocalDate.of(2026, 10, 16), PENDIENTE));
        PERIODOS.add(new Periodo(4, "Bimestre 4", LocalDate.of(2026, 10, 19), LocalDate.of(2026, 12, 18), PENDIENTE));
        PERIODOS.get(0).fechaCierre = "16/05/2026 18:30";
        PERIODOS.get(0).cerradoPor  = "dir_pitagoras";
    }

    public static List<Periodo> getPeriodos() { return Collections.unmodifiableList(PERIODOS); }

    public static Periodo buscarPeriodo(String nombre) {
        for (Periodo p : PERIODOS) if (p.nombre.equalsIgnoreCase(nombre)) return p;
        return null;
    }

    /** Periodo en curso: el abierto de mayor número (si se reabrió uno anterior, prevalece el más reciente). */
    public static Periodo periodoEnCurso() {
        for (int i = PERIODOS.size() - 1; i >= 0; i--) {
            if (ABIERTO.equals(PERIODOS.get(i).estado)) return PERIODOS.get(i);
        }
        return null;
    }

    public static void addOyentePeriodos(Runnable r) { OYENTES_PERIODOS.add(r); }

    /** RF07: cierra el periodo, bloquea notas/asistencia y deja el evento en la bitácora. */
    public static EventoBitacora cerrarPeriodo(Periodo p) {
        String anterior = p.estado;
        p.estado = CERRADO;
        p.fechaCierre = LocalDateTime.now().format(FMT_FECHA_HORA);
        p.cerradoPor = usuarioActual;
        EventoBitacora ev = registrarEvento("Periodo", "CERRAR_PERIODO", p.nombre, anterior, CERRADO, "");
        notificarPeriodos();
        return ev;
    }

    /** RF07: reabre un periodo cerrado; el motivo es obligatorio y queda en la bitácora. */
    public static EventoBitacora reabrirPeriodo(Periodo p, String motivo) {
        String anterior = p.estado;
        p.estado = ABIERTO;
        p.fechaCierre = "—";
        p.cerradoPor = "—";
        EventoBitacora ev = registrarEvento("Periodo", "REABRIR_PERIODO", p.nombre, anterior, ABIERTO, motivo);
        notificarPeriodos();
        return ev;
    }

    private static void notificarPeriodos() {
        for (Runnable r : new ArrayList<>(OYENTES_PERIODOS)) r.run();
    }

    // ══ Bitácora de auditoría ═════════════════════════════════════
    public static final class EventoBitacora {
        public final int id;
        public final String fechaHora, usuario, modulo, accion, registro, anterior, nuevo, motivo;

        EventoBitacora(int id, String fechaHora, String usuario, String modulo, String accion,
                       String registro, String anterior, String nuevo, String motivo) {
            this.id = id; this.fechaHora = fechaHora; this.usuario = usuario; this.modulo = modulo;
            this.accion = accion; this.registro = registro; this.anterior = anterior;
            this.nuevo = nuevo; this.motivo = motivo;
        }

        public Object[] aFila() {
            return new Object[]{ String.valueOf(id), fechaHora, usuario, modulo, accion,
                                 registro, anterior, nuevo, motivo == null || motivo.isEmpty() ? "—" : motivo };
        }
    }

    private static final List<EventoBitacora> BITACORA = new ArrayList<>();       // orden cronológico
    private static final List<Consumer<EventoBitacora>> OYENTES_BITACORA = new ArrayList<>();

    static {
        BITACORA.add(new EventoBitacora(101, "16/05/2026 18:30", "dir_pitagoras",  "Periodo",      "CERRAR_PERIODO",    "Bimestre 1",       "Abierto", "Cerrado", ""));
        BITACORA.add(new EventoBitacora(102, "25/10/2026 09:05", "dir_pitagoras",  "Usuario",      "RESET_PASSWORD",    "Usr:apo_748596",   "Hash***", "HashNuevo***", ""));
        BITACORA.add(new EventoBitacora(103, "28/10/2026 11:15", "doc_ciencias",   "Asistencia",   "UPDATE_ASISTENCIA", "Alum:76321458",    "F",       "J (Justificada)", ""));
        BITACORA.add(new EventoBitacora(104, "28/10/2026 16:40", "doc_matematica", "Calificacion", "UPDATE_NOTA",       "Insc:42 - Bim:2",  "B",       "A", ""));
    }

    public static List<EventoBitacora> getBitacora() { return Collections.unmodifiableList(BITACORA); }

    public static void addOyenteBitacora(Consumer<EventoBitacora> c) { OYENTES_BITACORA.add(c); }

    /** Punto único para auditar. Los módulos de Notas y Asistencia también deben llamarlo (RF09). */
    public static EventoBitacora registrarEvento(String modulo, String accion, String registro,
                                                 String anterior, String nuevo, String motivo) {
        int id = BITACORA.isEmpty() ? 1 : BITACORA.get(BITACORA.size() - 1).id + 1;
        EventoBitacora ev = new EventoBitacora(id, LocalDateTime.now().format(FMT_FECHA_HORA),
                usuarioActual, modulo, accion, registro, anterior, nuevo, motivo);
        BITACORA.add(ev);
        for (Consumer<EventoBitacora> c : new ArrayList<>(OYENTES_BITACORA)) c.accept(ev);
        return ev;
    }

    // ══ Credenciales (RF02 / RNF02) ═══════════════════════════════
    // Se usa PBKDF2-HMAC-SHA256 con sal aleatoria (incluido en el JDK). Si el equipo
    // adopta BCrypt (jBCrypt / spring-security-crypto), solo cambian hash() y verificar().
    private static final int ITERACIONES = 120_000;
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Map<String, byte[][]> CREDENCIALES = new HashMap<>(); // usuario -> {sal, hash}

    public static final int MAX_INTENTOS = 3;
    public static final int MINUTOS_BLOQUEO = 5;
    private static final Map<String, Integer> INTENTOS = new HashMap<>();
    private static final Map<String, LocalDateTime> BLOQUEO_HASTA = new HashMap<>();

    static {
        // Contraseña de DEMOSTRACIÓN; eliminar al conectar con la tabla de usuarios.
        guardarPassword("dir_pitagoras", "Pitagoras2026".toCharArray());
    }

    private static byte[] hash(char[] password, byte[] sal) {
        try {
            PBEKeySpec spec = new PBEKeySpec(password, sal, ITERACIONES, 256);
            byte[] h = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
            spec.clearPassword();
            return h;
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo calcular el hash de la contraseña", e);
        }
    }

    public static void guardarPassword(String usuario, char[] password) {
        byte[] sal = new byte[16];
        RANDOM.nextBytes(sal);
        CREDENCIALES.put(usuario, new byte[][]{ sal, hash(password, sal) });
    }

    public static boolean verificarPassword(String usuario, char[] password) {
        byte[][] c = CREDENCIALES.get(usuario);
        if (c == null) return false;
        return MessageDigest.isEqual(c[1], hash(password, c[0]));   // comparación en tiempo constante
    }

    /** Cambia la contraseña, reinicia intentos y audita el evento. */
    public static EventoBitacora cambiarPassword(String usuario, char[] nueva) {
        guardarPassword(usuario, nueva);
        limpiarIntentos(usuario);
        return registrarEvento("Usuario", "CAMBIAR_PASSWORD", "Usr:" + usuario, "Hash***", "HashNuevo***", "");
    }

    public static boolean estaBloqueado(String usuario) {
        LocalDateTime hasta = BLOQUEO_HASTA.get(usuario);
        if (hasta == null) return false;
        if (LocalDateTime.now().isAfter(hasta)) { BLOQUEO_HASTA.remove(usuario); INTENTOS.remove(usuario); return false; }
        return true;
    }

    public static long minutosBloqueoRestantes(String usuario) {
        LocalDateTime hasta = BLOQUEO_HASTA.get(usuario);
        if (hasta == null) return 0;
        return Math.max(1, java.time.Duration.between(LocalDateTime.now(), hasta).toMinutes() + 1);
    }

    /** Registra un intento fallido y devuelve cuántos intentos quedan (0 = cuenta bloqueada). */
    public static int registrarIntentoFallido(String usuario) {
        int n = INTENTOS.merge(usuario, 1, Integer::sum);
        if (n >= MAX_INTENTOS) {
            BLOQUEO_HASTA.put(usuario, LocalDateTime.now().plusMinutes(MINUTOS_BLOQUEO));
            return 0;
        }
        return MAX_INTENTOS - n;
    }

    public static void limpiarIntentos(String usuario) {
        INTENTOS.remove(usuario);
        BLOQUEO_HASTA.remove(usuario);
    }
}
