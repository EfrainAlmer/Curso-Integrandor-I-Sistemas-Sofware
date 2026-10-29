package pe.edu.utp.pitagoras.util;

import pe.edu.utp.pitagoras.model.CalificacionCompetencia;
import pe.edu.utp.pitagoras.model.RegistroAsistencia;
import pe.edu.utp.pitagoras.model.ReglaCalificativo;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Utilitario de exportación de hojas de cálculo (compatible con Microsoft Excel y Apache POI)
 * para las planillas de calificaciones y asistencia del Docente y soporte SIAGIE (RF18, RF19).
 *
 * @author Angelo Giuseppe Paredes Quispe
 * @version 2.0
 */
public final class ExportadorExcelUtil {

    private ExportadorExcelUtil() {
    }

    /**
     * Exporta la planilla de calificaciones del docente (con 3 o 4 competencias según el área CNEB)
     * a un archivo compatible con Microsoft Excel (.csv con BOM UTF-8).
     */
    public static void exportarPlanillaNotasExcel(File destino, String bimestre, String seccion,
                                                  String area, String[] competencias,
                                                  List<CalificacionCompetencia> filas) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(destino), StandardCharsets.UTF_8))) {
            writer.write('\uFEFF');
            writer.write("COLEGIO PARTICULAR PITÁGORAS - PLANILLA DE CALIFICACIONES (" + bimestre + " - " + seccion + " - " + area + ")");
            writer.newLine();

            List<String> cabeceras = new ArrayList<>();
            cabeceras.add("ID Estudiante");
            cabeceras.add("Estudiante");
            for (String comp : competencias) {
                cabeceras.add(limpiarCelda(comp));
            }
            cabeceras.add("Logro del Área");
            cabeceras.add("Calificativo Anual (RF14)");
            cabeceras.add("Conclusión Descriptiva del Periodo");
            writer.write(String.join(";", cabeceras));
            writer.newLine();

            boolean esCuartoBim = "Bimestre 4".equalsIgnoreCase(bimestre);
            for (CalificacionCompetencia c : filas) {
                String logro = ReglaCalificativo.calcularCalificativoArea(c.getCalificativos());
                String anual = ReglaCalificativo.determinarCalificativoAnual(logro, esCuartoBim);

                List<String> celdas = new ArrayList<>();
                celdas.add(String.valueOf(c.getEstudianteId()));
                celdas.add(limpiarCelda(c.getEstudianteNombre()));
                for (int i = 0; i < competencias.length; i++) {
                    celdas.add(c.getCalificativo(i));
                }
                celdas.add(logro);
                celdas.add(anual);
                celdas.add(limpiarCelda(c.getConclusionDescriptiva()));

                writer.write(String.join(";", celdas));
                writer.newLine();
            }
        }
        SeguridadUtil.registrarLogInfo("Reports-Excel", "Planilla exportada exitosamente en: " + destino.getAbsolutePath());
    }

    /**
     * Exporta el registro de asistencia en lote a un archivo compatible con Microsoft Excel.
     */
    public static void exportarAsistenciaExcel(File destino, String seccion, String fecha,
                                               List<RegistroAsistencia> asistencias) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(destino), StandardCharsets.UTF_8))) {
            writer.write('\uFEFF');
            writer.write("COLEGIO PARTICULAR PITÁGORAS - CONSOLIDADO DE ASISTENCIA (" + seccion + " - " + fecha + ")");
            writer.newLine();
            writer.write("ID Estudiante;Estudiante;Sección;Fecha;Estado;Falta Justificada;Observación");
            writer.newLine();

            for (RegistroAsistencia r : asistencias) {
                writer.write(String.join(";",
                        String.valueOf(r.getEstudianteId()),
                        limpiarCelda(r.getEstudianteNombre()),
                        r.getSeccion(),
                        r.getFecha(),
                        r.getEstado(),
                        r.isJustificada() ? "Sí" : "No",
                        limpiarCelda(r.getObservacion())
                ));
                writer.newLine();
            }
        }
        SeguridadUtil.registrarLogInfo("Attendance-Excel", "Asistencia exportada exitosamente en: " + destino.getAbsolutePath());
    }

    private static String limpiarCelda(String valor) {
        if (valor == null) {
            return "";
        }
        return valor.replace(";", ",").trim();
    }
}
