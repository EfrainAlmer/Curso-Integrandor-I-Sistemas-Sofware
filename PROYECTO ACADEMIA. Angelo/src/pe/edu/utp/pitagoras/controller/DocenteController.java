package pe.edu.utp.pitagoras.controller;

import pe.edu.utp.pitagoras.dao.DocenteDAO;
import pe.edu.utp.pitagoras.dao.IDocenteDAO;
import pe.edu.utp.pitagoras.model.*;

import java.util.*;

/**
 * Controlador del módulo Docente (Figura 16, pág. 40: "Un controlador por módulo").
 * <p>
 * Soporta múltiples docentes donde cada uno enseña sus 2 cursos a todos los grados de 1.° A a 5.° A:
 * <ul>
 *   <li><b>{@code docente} (Prof. Carlos Mendoza):</b> Matemática (4 competencias) y Ciencia y Tecnología (3 competencias) en 1.° A, 2.° A, 3.° A, 4.° A y 5.° A.</li>
 *   <li><b>{@code docente2} (Prof. María Quispe):</b> Comunicación (3 competencias) y Ciencias Sociales (3 competencias) en 1.° A, 2.° A, 3.° A, 4.° A y 5.° A.</li>
 * </ul>
 *
 * @author Angelo Giuseppe Paredes Quispe
 * @version 2.1
 */
public class DocenteController {
    private static final DocenteController INSTANCIA = new DocenteController();

    private final IDocenteDAO docenteDAO = new DocenteDAO();
    private final Map<String, List<CargaAcademica>> cargasPorUsuarioDocente = new LinkedHashMap<>();
    private final Map<String, String[]> competenciasPorArea = new LinkedHashMap<>();
    private final Map<String, List<Estudiante>> estudiantesPorSeccion = new LinkedHashMap<>();
    private final Map<String, Boolean> estadoPeriodos = new LinkedHashMap<>();
    // Clave: bimestre|seccion|area -> lista de calificaciones por estudiante
    private final Map<String, List<CalificacionCompetencia>> repositorioNotas = new HashMap<>();
    // Clave: seccion|fecha -> lista de asistencia por estudiante
    private final Map<String, List<RegistroAsistencia>> repositorioAsistencia = new HashMap<>();
    private final List<BitacoraEvento> bitacoraAuditoria = new ArrayList<>();

    private String usernameDocenteActivo = "docente";
    private String nombreDocenteActivo = "Prof. Carlos Mendoza";

    public static DocenteController getInstancia() {
        return INSTANCIA;
    }

    private DocenteController() {
        inicializarDatosDemo();
    }

    public void establecerDocenteActivo(String username, String nombreCompleto) {
        if (username != null && cargasPorUsuarioDocente.containsKey(username.toLowerCase().trim())) {
            this.usernameDocenteActivo = username.toLowerCase().trim();
            this.nombreDocenteActivo = nombreCompleto;
        } else {
            this.usernameDocenteActivo = "docente";
            this.nombreDocenteActivo = "Prof. Carlos Mendoza";
        }
    }

    public String getUsernameDocenteActivo() {
        return usernameDocenteActivo;
    }

    public String getNombreDocenteActivo() {
        return nombreDocenteActivo;
    }

    private void inicializarDatosDemo() {
        // 1. Estado de periodos bimestrales (RF07 / BPMN TO-BE Figura 9)
        estadoPeriodos.put("Bimestre 1", false); // Cerrado (Bloqueado para edición)
        estadoPeriodos.put("Bimestre 2", true);  // Abierto (En curso)
        estadoPeriodos.put("Bimestre 3", false); // Pendiente
        estadoPeriodos.put("Bimestre 4", true);  // Habilitado para probar cálculo anual RF14/CU14

        // 2. Áreas Curriculares y Competencias Oficiales (SIAGIE / MINEDU)
        String[] compMatematica = {
                "Resuelve problemas de cantidad",
                "Resuelve problemas de regularidad, equivalencia y cambio",
                "Resuelve problemas de forma, movimiento y localización",
                "Resuelve problemas de gestión de datos e incertidumbre"
        };
        String[] compCyT = {
                "Indaga mediante métodos científicos para construir conocimientos",
                "Explica el mundo físico basándose en conocimientos sobre los seres vivos",
                "Diseña y construye soluciones tecnológicas para su entorno"
        };
        String[] compComunicacion = {
                "Se comunica oralmente en su lengua materna",
                "Lee diversos tipos de textos escritos en su lengua materna",
                "Escribe diversos tipos de textos en su lengua materna"
        };
        String[] compCienciasSociales = {
                "Construye interpretaciones históricas",
                "Gestiona responsablemente el espacio y el ambiente",
                "Gestiona responsablemente los recursos económicos"
        };

        competenciasPorArea.put("Matemática", compMatematica);
        competenciasPorArea.put("Ciencia y Tecnología", compCyT);
        competenciasPorArea.put("Comunicación", compComunicacion);
        competenciasPorArea.put("Ciencias Sociales", compCienciasSociales);

        String[] grados = {"1.°", "2.°", "3.°", "4.°", "5.°"};

        // 3. Carga académica Docente 1 ("docente" - Prof. Carlos Mendoza):
        // Enseña sus 2 cursos (Matemática y Ciencia y Tecnología) a TODOS los grados de 1.° A a 5.° A
        List<CargaAcademica> cargaDocente1 = new ArrayList<>();
        int contador1 = 1;
        for (String grado : grados) {
            cargaDocente1.add(new CargaAcademica(
                    String.format("CA-2026-%02d", contador1++), 2026, grado, "A", "Mañana",
                    "Matemática", "Prof. Carlos Mendoza", compMatematica));
            cargaDocente1.add(new CargaAcademica(
                    String.format("CA-2026-%02d", contador1++), 2026, grado, "A", "Mañana",
                    "Ciencia y Tecnología", "Prof. Carlos Mendoza", compCyT));
        }
        cargasPorUsuarioDocente.put("docente", cargaDocente1);

        // 4. Carga académica Docente 2 ("docente2" - Prof. María Quispe):
        // Enseña sus 2 cursos diferentes (Comunicación y Ciencias Sociales) a TODOS los grados de 1.° A a 5.° A
        List<CargaAcademica> cargaDocente2 = new ArrayList<>();
        int contador2 = 11;
        for (String grado : grados) {
            cargaDocente2.add(new CargaAcademica(
                    String.format("CA-2026-%02d", contador2++), 2026, grado, "A", "Mañana",
                    "Comunicación", "Prof. María Quispe", compComunicacion));
            cargaDocente2.add(new CargaAcademica(
                    String.format("CA-2026-%02d", contador2++), 2026, grado, "A", "Mañana",
                    "Ciencias Sociales", "Prof. María Quispe", compCienciasSociales));
        }
        cargasPorUsuarioDocente.put("docente2", cargaDocente2);

        // 5. Nómina de estudiantes con NOMBRES COMPLETOS por sección (1.° A a 5.° A)
        estudiantesPorSeccion.put("1.° A", List.of(
                new Estudiante(5, "75123401", "EST-1A-01", "Mendoza Torres, Sebastián", "1.° A", "Sr. Luis Mendoza", "964125874", "Activa"),
                new Estudiante(6, "75123402", "EST-1A-02", "Ureta Salazar, Camila", "1.° A", "Sra. Carmen Salazar", "932145698", "Activa"),
                new Estudiante(7, "75123403", "EST-1A-03", "Agapito Vega, Rodrigo", "1.° A", "Sr. Pedro Agapito", "978451236", "Activa"),
                new Estudiante(8, "75123404", "EST-1A-04", "Chacchi Morales, Ariana", "1.° A", "Sra. Julia Morales", "912365478", "Activa")
        ));

        estudiantesPorSeccion.put("2.° A", List.of(
                new Estudiante(9, "74985201", "EST-2A-01", "Ríos Palomino, Joaquín", "2.° A", "Sr. César Ríos", "954789632", "Activa"),
                new Estudiante(10, "74985202", "EST-2A-02", "Castillo Flores, Fernanda", "2.° A", "Sra. Ana Flores", "985632147", "Activa"),
                new Estudiante(11, "74985203", "EST-2A-03", "Vargas Soto, Gabriel", "2.° A", "Sr. Raúl Vargas", "963258741", "Activa"),
                new Estudiante(12, "74985204", "EST-2A-04", "Aguilar Ramos, Daniela", "2.° A", "Sra. Patricia Ramos", "947852369", "Activa")
        ));

        estudiantesPorSeccion.put("3.° A", List.of(
                new Estudiante(1, "74859601", "EST-3A-01", "Pérez Rojas, Juan Carlos", "3.° A", "Sr. Mateo Pérez", "963852741", "Activa"),
                new Estudiante(2, "74859602", "EST-3A-02", "Almerco Huamán, Lucía", "3.° A", "Sra. Elena Huamán", "951753852", "Activa"),
                new Estudiante(3, "74859603", "EST-3A-03", "Paredes Lozano, Diego", "3.° A", "Sr. Jorge Paredes", "987654321", "Activa"),
                new Estudiante(4, "74859604", "EST-3A-04", "Balvin Espinoza, Valeria", "3.° A", "Sra. Rosa Espinoza", "945612378", "Activa")
        ));

        estudiantesPorSeccion.put("4.° A", List.of(
                new Estudiante(13, "73654101", "EST-4A-01", "Huanca Chávez, Leonardo", "4.° A", "Sr. Víctor Huanca", "914785236", "Activa"),
                new Estudiante(14, "73654102", "EST-4A-02", "Navarro Díaz, Ximena", "4.° A", "Sra. Mónica Díaz", "936985214", "Activa"),
                new Estudiante(15, "73654103", "EST-4A-03", "Zevallos Peña, André", "4.° A", "Sr. Óscar Zevallos", "925874136", "Activa"),
                new Estudiante(16, "73654104", "EST-4A-04", "Cárdenas Silva, Mariana", "4.° A", "Sra. Gloria Silva", "987412365", "Activa")
        ));

        estudiantesPorSeccion.put("5.° A", List.of(
                new Estudiante(17, "72145801", "EST-5A-01", "Villanueva Castro, Matías", "5.° A", "Sr. Héctor Villanueva", "958741236", "Activa"),
                new Estudiante(18, "72145802", "EST-5A-02", "Ortega Gómez, Valentina", "5.° A", "Sra. Silvia Gómez", "965874123", "Activa"),
                new Estudiante(19, "72145803", "EST-5A-03", "Lozano Poma, Nicolás", "5.° A", "Sr. Andrés Lozano", "974125896", "Activa"),
                new Estudiante(20, "72145804", "EST-5A-04", "Bustamante León, Antonella", "5.° A", "Sra. Clara León", "985214763", "Activa")
        ));

        // 6. Calificativos iniciales por área y sección con NOMBRES COMPLETOS de los estudiantes
        String[][] notas4Comp = {
                {"A", "A", "B", "A"},
                {"AD", "A", "A", "AD"},
                {"B", "C", "B", "B"},
                {"A", "B", "A", "A"}
        };
        String[][] notas3Comp = {
                {"A", "A", "A"},
                {"AD", "A", "AD"},
                {"B", "B", "C"},
                {"A", "B", "A"}
        };
        String[] conclusionesDemo = {
                "Mantiene un desempeño esperado y justifica sus resultados con claridad.",
                "Destaca en el logro de las competencias del área y propone soluciones autónomas.",
                "Presenta dificultades en situaciones nuevas. Requiere acompañamiento y práctica guiada.",
                "Superó las dificultades iniciales y aplica los conceptos en contextos cotidianos."
        };

        for (String bim : List.of("Bimestre 1", "Bimestre 2", "Bimestre 3", "Bimestre 4")) {
            for (String seccion : estudiantesPorSeccion.keySet()) {
                List<Estudiante> alumnos = estudiantesPorSeccion.get(seccion);
                for (Map.Entry<String, String[]> entryArea : competenciasPorArea.entrySet()) {
                    String area = entryArea.getKey();
                    int numComp = entryArea.getValue().length;
                    List<CalificacionCompetencia> lista = new ArrayList<>();
                    for (int i = 0; i < alumnos.size(); i++) {
                        Estudiante est = alumnos.get(i);
                        String[] cal = (numComp == 4)
                                ? notas4Comp[i % notas4Comp.length]
                                : notas3Comp[i % notas3Comp.length];
                        lista.add(new CalificacionCompetencia(
                                est.getEstudianteId(),
                                est.getNombresApellidos(),
                                bim,
                                seccion,
                                area,
                                cal,
                                conclusionesDemo[i % conclusionesDemo.length]
                        ));
                    }
                    repositorioNotas.put(claveNotas(bim, seccion, area), lista);
                }
            }
        }

        // 7. Eventos iniciales en bitácora de auditoría (RF09)
        bitacoraAuditoria.add(new BitacoraEvento("2026-10-29 08:15:00", "docente", "Attendance",
                "MODIFICACION", "Estado=F (Almerco Huamán, Lucía)", "Estado=F (Justificada=Sí)"));
        bitacoraAuditoria.add(new BitacoraEvento("2026-10-29 09:30:00", "docente", "Grades",
                "MODIFICACION", "Calificativo=B (Pérez Rojas, Juan Carlos)", "Calificativo=A"));
    }

    public List<CargaAcademica> obtenerCargaDocente() {
        List<CargaAcademica> lista = cargasPorUsuarioDocente.get(usernameDocenteActivo);
        if (lista == null) {
            lista = cargasPorUsuarioDocente.get("docente");
        }
        return Collections.unmodifiableList(lista);
    }

    public String[] obtenerSeccionesDocenteActivo() {
        Set<String> secciones = new LinkedHashSet<>();
        for (CargaAcademica c : obtenerCargaDocente()) {
            secciones.add(c.getGradoSeccionCorto());
        }
        return secciones.toArray(new String[0]);
    }

    public String[] obtenerAreasDocenteActivo() {
        Set<String> areas = new LinkedHashSet<>();
        for (CargaAcademica c : obtenerCargaDocente()) {
            areas.add(c.getAreaCurricular());
        }
        return areas.toArray(new String[0]);
    }

    public List<Estudiante> obtenerEstudiantesPorSeccion(String seccion) {
        return estudiantesPorSeccion.getOrDefault(seccion, Collections.emptyList());
    }

    public boolean esPeriodoAbierto(String bimestre) {
        return estadoPeriodos.getOrDefault(bimestre, false);
    }

    public String[] obtenerCompetenciasArea(String areaCurricular) {
        if (areaCurricular != null && competenciasPorArea.containsKey(areaCurricular)) {
            return competenciasPorArea.get(areaCurricular);
        }
        return competenciasPorArea.get("Matemática");
    }

    public List<CalificacionCompetencia> obtenerCalificaciones(String bimestre, String seccion, String area) {
        String key = claveNotas(bimestre, seccion, area);
        List<CalificacionCompetencia> guardadas = repositorioNotas.get(key);
        if (guardadas == null) {
            List<Estudiante> alumnos = obtenerEstudiantesPorSeccion(seccion);
            String[] comps = obtenerCompetenciasArea(area);
            guardadas = new ArrayList<>();
            for (Estudiante e : alumnos) {
                String[] cal = new String[comps.length];
                Arrays.fill(cal, "A");
                guardadas.add(new CalificacionCompetencia(
                        e.getEstudianteId(), e.getNombresApellidos(), bimestre, seccion, area, cal,
                        "Mantiene un desempeño esperado en las competencias del periodo."));
            }
            repositorioNotas.put(key, guardadas);
        }
        List<CalificacionCompetencia> copia = new ArrayList<>();
        for (CalificacionCompetencia c : guardadas) {
            copia.add(c.copiar());
        }
        return copia;
    }

    public int guardarCalificaciones(String bimestre, String seccion, String area, List<CalificacionCompetencia> nuevas) {
        String key = claveNotas(bimestre, seccion, area);
        List<CalificacionCompetencia> anteriores = repositorioNotas.getOrDefault(key, Collections.emptyList());
        List<BitacoraEvento> nuevosEventos = new ArrayList<>();

        for (int i = 0; i < nuevas.size(); i++) {
            CalificacionCompetencia nueva = nuevas.get(i);
            if (i < anteriores.size()) {
                CalificacionCompetencia ant = anteriores.get(i);
                int totalComp = nueva.getNumeroCompetencias();
                for (int comp = 0; comp < totalComp; comp++) {
                    String vAnt = ant.getCalificativo(comp);
                    String vNvo = nueva.getCalificativo(comp);
                    if (!Objects.equals(vAnt, vNvo)) {
                        BitacoraEvento ev = new BitacoraEvento(
                                usernameDocenteActivo,
                                "Grades (" + seccion + " - " + area + ")",
                                "MODIFICACION_NOTA (" + bimestre + ")",
                                nueva.getEstudianteNombre() + " C" + (comp + 1) + "=" + vAnt,
                                nueva.getEstudianteNombre() + " C" + (comp + 1) + "=" + vNvo
                        );
                        nuevosEventos.add(ev);
                        bitacoraAuditoria.add(0, ev);
                    }
                }
            }
        }

        List<CalificacionCompetencia> persistir = new ArrayList<>();
        for (CalificacionCompetencia c : nuevas) {
            persistir.add(c.copiar());
        }
        repositorioNotas.put(key, persistir);
        docenteDAO.guardarCalificativosSQL(persistir, nuevosEventos);
        return nuevosEventos.size();
    }

    public List<RegistroAsistencia> obtenerAsistencia(String seccion, String fecha) {
        String key = seccion + "|" + fecha;
        List<RegistroAsistencia> guardada = repositorioAsistencia.get(key);
        if (guardada == null) {
            List<Estudiante> alumnos = obtenerEstudiantesPorSeccion(seccion);
            guardada = new ArrayList<>();
            String[] estadosDefecto = {"Asistió", "Falta", "Tardanza", "Asistió"};
            boolean[] justDefecto = {false, true, false, false};
            for (int i = 0; i < alumnos.size(); i++) {
                Estudiante e = alumnos.get(i);
                guardada.add(new RegistroAsistencia(
                        e.getEstudianteId(),
                        e.getNombresApellidos(),
                        seccion,
                        fecha,
                        estadosDefecto[i % estadosDefecto.length],
                        justDefecto[i % justDefecto.length],
                        ""
                ));
            }
            repositorioAsistencia.put(key, guardada);
        }
        List<RegistroAsistencia> copia = new ArrayList<>();
        for (RegistroAsistencia r : guardada) {
            copia.add(r.copiar());
        }
        return copia;
    }

    public int guardarAsistenciaLote(String seccion, String fecha, List<RegistroAsistencia> lote) {
        String key = seccion + "|" + fecha;
        List<RegistroAsistencia> anterior = repositorioAsistencia.getOrDefault(key, Collections.emptyList());
        List<BitacoraEvento> nuevosEventos = new ArrayList<>();

        for (int i = 0; i < lote.size(); i++) {
            RegistroAsistencia nvo = lote.get(i);
            if (i < anterior.size()) {
                RegistroAsistencia ant = anterior.get(i);
                if (!Objects.equals(ant.getEstado(), nvo.getEstado())
                        || ant.isJustificada() != nvo.isJustificada()
                        || !Objects.equals(ant.getObservacion(), nvo.getObservacion())) {
                    BitacoraEvento ev = new BitacoraEvento(
                            usernameDocenteActivo,
                            "Attendance (" + seccion + ")",
                            "GUARDADO_LOTE (" + fecha + ")",
                            nvo.getEstudianteNombre() + ": " + ant.getEstado() + (ant.isJustificada() ? " [Justificada]" : ""),
                            nvo.getEstudianteNombre() + ": " + nvo.getEstado() + (nvo.isJustificada() ? " [Justificada]" : "")
                    );
                    nuevosEventos.add(ev);
                    bitacoraAuditoria.add(0, ev);
                }
            }
        }

        if (nuevosEventos.isEmpty()) {
            BitacoraEvento evLote = new BitacoraEvento(
                    usernameDocenteActivo,
                    "Attendance (" + seccion + ")",
                    "GUARDADO_LOTE (" + fecha + ")",
                    "Asistencia diaria de " + lote.size() + " estudiantes",
                    "Confirmado sin incidencias"
            );
            nuevosEventos.add(evLote);
            bitacoraAuditoria.add(0, evLote);
        }

        List<RegistroAsistencia> persistir = new ArrayList<>();
        for (RegistroAsistencia r : lote) {
            persistir.add(r.copiar());
        }
        repositorioAsistencia.put(key, persistir);
        docenteDAO.guardarLoteAsistenciaSQL(persistir, nuevosEventos);
        return nuevosEventos.size();
    }

    public List<BitacoraEvento> obtenerBitacoraReciente() {
        return Collections.unmodifiableList(bitacoraAuditoria);
    }

    private String claveNotas(String bimestre, String seccion, String area) {
        return bimestre + "|" + seccion + "|" + area;
    }
}
