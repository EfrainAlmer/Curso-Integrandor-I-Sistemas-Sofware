package pe.edu.pitagoras.sga.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import pe.edu.pitagoras.sga.exception.BusinessException;
import pe.edu.pitagoras.sga.model.academico.Alumno;
import pe.edu.pitagoras.sga.model.academico.CargaAcademica;
import pe.edu.pitagoras.sga.model.academico.Competencia;
import pe.edu.pitagoras.sga.model.academico.Curso;
import pe.edu.pitagoras.sga.model.academico.Periodo;
import pe.edu.pitagoras.sga.model.evaluacion.Calificativo;
import pe.edu.pitagoras.sga.model.evaluacion.Consolidado;
import pe.edu.pitagoras.sga.model.evaluacion.FilaConsolidado;
import pe.edu.pitagoras.sga.model.evaluacion.FilaNotas;

/**
 * Exportación a Excel (.xlsx) con Apache POI (RF19): nómina, acta de notas y consolidado
 * bimestral, en un formato fácil de copiar al SIAGIE.
 */
public final class ExcelReportes {

    private ExcelReportes() {
    }

    public static void nomina(List<Alumno> alumnos, File destino) {
        escribir(destino, (wb, est) -> {
            Sheet hoja = wb.createSheet("Nómina");
            cabecera(hoja, est, "N.°", "DNI", "Apellidos", "Nombres", "Grado", "Sección", "Turno", "Apoderado",
                    "Teléfono", "Estado");
            int f = 1;
            for (Alumno a : alumnos) {
                Row r = hoja.createRow(f);
                valores(r, est.celda, f, a.dni(), a.apellido(), a.nombre(), a.seccion().grado(), a.seccion().letra(),
                        a.seccion().turno(), a.nombreApoderado(), a.telefonoApoderado(), a.estado());
                f++;
            }
            ajustar(hoja, 10);
        });
    }

    public static void actaNotas(CargaAcademica carga, Periodo periodo, List<Competencia> competencias,
                                 List<FilaNotas> filas, File destino) {
        escribir(destino, (wb, est) -> {
            Sheet hoja = wb.createSheet(carga.curso().nombre().replaceAll("[\\\\/?*\\[\\]:]", ""));
            Row titulo = hoja.createRow(0);
            titulo.createCell(0).setCellValue(carga.curso().nombre() + " - " + carga.seccion().etiqueta() + " - "
                    + periodo.etiqueta());
            titulo.getCell(0).setCellStyle(est.titulo);
            String[] cols = new String[competencias.size() + 4];
            cols[0] = "N.°";
            cols[1] = "DNI";
            cols[2] = "Apellidos y nombres";
            for (int i = 0; i < competencias.size(); i++) {
                cols[i + 3] = "C" + competencias.get(i).codigo() + " " + competencias.get(i).nombre();
            }
            cols[cols.length - 1] = "Calificativo del área";
            cabecera(hoja, 2, est, cols);
            int f = 3;
            int n = 1;
            for (FilaNotas fila : filas) {
                Row r = hoja.createRow(f++);
                Object[] v = new Object[cols.length];
                v[0] = n++;
                v[1] = fila.getDni();
                v[2] = fila.getAlumno();
                for (int i = 0; i < competencias.size(); i++) {
                    v[i + 3] = texto(fila.get(competencias.get(i).codigo()));
                }
                v[v.length - 1] = texto(fila.calificativoArea().orElse(null));
                valores(r, est.celda, v);
            }
            ajustar(hoja, cols.length);
        });
    }

    public static void consolidado(Consolidado c, File destino) {
        escribir(destino, (wb, est) -> {
            Sheet hoja = wb.createSheet("Consolidado");
            Row titulo = hoja.createRow(0);
            titulo.createCell(0).setCellValue("Consolidado " + c.seccion().etiqueta() + " - " + c.periodo().etiqueta());
            titulo.getCell(0).setCellStyle(est.titulo);
            List<Curso> cursos = c.cursos();
            String[] cols = new String[cursos.size() + 7];
            cols[0] = "N.°";
            cols[1] = "DNI";
            cols[2] = "Apellidos y nombres";
            for (int i = 0; i < cursos.size(); i++) {
                cols[i + 3] = cursos.get(i).nombre();
            }
            int k = cursos.size() + 3;
            cols[k] = "Asistencias";
            cols[k + 1] = "Tardanzas";
            cols[k + 2] = "Faltas";
            cols[k + 3] = "Faltas justificadas";
            cabecera(hoja, 2, est, cols);
            int f = 3;
            int n = 1;
            for (FilaConsolidado fila : c.filas()) {
                Row r = hoja.createRow(f++);
                Object[] v = new Object[cols.length];
                v[0] = n++;
                v[1] = fila.alumno().dni();
                v[2] = fila.alumno().nombreCompleto();
                for (int i = 0; i < cursos.size(); i++) {
                    v[i + 3] = texto(fila.porCurso().get(cursos.get(i).cursoId()));
                }
                v[k] = fila.asistencia().asistencias();
                v[k + 1] = fila.asistencia().tardanzas();
                v[k + 2] = fila.asistencia().faltas();
                v[k + 3] = fila.asistencia().justificadas();
                valores(r, est.celda, v);
            }
            ajustar(hoja, cols.length);
        });
    }

    // ---------- piezas comunes ----------

    private record Estilos(CellStyle cabecera, CellStyle celda, CellStyle titulo) {
    }

    @FunctionalInterface
    private interface Contenido {
        void escribir(Workbook wb, Estilos estilos);
    }

    private static void escribir(File destino, Contenido contenido) {
        try (Workbook wb = new XSSFWorkbook(); OutputStream out = new FileOutputStream(destino)) {
            contenido.escribir(wb, estilos(wb));
            wb.write(out);
        } catch (IOException e) {
            throw new BusinessException("No se pudo generar el Excel " + destino.getName() + ": " + e.getMessage());
        }
    }

    private static Estilos estilos(Workbook wb) {
        Font negrita = wb.createFont();
        negrita.setBold(true);
        negrita.setColor(IndexedColors.WHITE.getIndex());
        CellStyle cab = wb.createCellStyle();
        cab.setFont(negrita);
        cab.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        cab.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        cab.setAlignment(HorizontalAlignment.CENTER);
        cab.setWrapText(true);
        CellStyle celda = wb.createCellStyle();
        celda.setBorderBottom(BorderStyle.THIN);
        celda.setBottomBorderColor(IndexedColors.GREY_25_PERCENT.getIndex());
        Font grande = wb.createFont();
        grande.setBold(true);
        grande.setFontHeightInPoints((short) 13);
        CellStyle titulo = wb.createCellStyle();
        titulo.setFont(grande);
        return new Estilos(cab, celda, titulo);
    }

    private static void cabecera(Sheet hoja, Estilos est, String... titulos) {
        cabecera(hoja, 0, est, titulos);
    }

    private static void cabecera(Sheet hoja, int fila, Estilos est, String... titulos) {
        Row r = hoja.createRow(fila);
        for (int i = 0; i < titulos.length; i++) {
            r.createCell(i).setCellValue(titulos[i]);
            r.getCell(i).setCellStyle(est.cabecera);
        }
        hoja.createFreezePane(0, fila + 1);
    }

    private static void valores(Row r, CellStyle estilo, Object... valores) {
        for (int i = 0; i < valores.length; i++) {
            Object v = valores[i];
            var celda = r.createCell(i);
            if (v instanceof Number num) {
                celda.setCellValue(num.doubleValue());
            } else {
                celda.setCellValue(v == null ? "" : v.toString());
            }
            celda.setCellStyle(estilo);
        }
    }

    private static void ajustar(Sheet hoja, int columnas) {
        for (int i = 0; i < columnas; i++) {
            hoja.autoSizeColumn(i);
            hoja.setColumnWidth(i, Math.min(hoja.getColumnWidth(i) + 512, 60 * 256));
        }
    }

    private static String texto(Calificativo c) {
        return c == null ? "" : c.name();
    }
}
