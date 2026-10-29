package pe.edu.utp.pitagoras.temporal;

import javax.swing.table.TableModel;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Escritor mínimo de archivos .xlsx sin dependencias externas (un .xlsx es un ZIP de XML).
 * Todas las celdas se guardan como texto, de modo que el DNI conserva sus ceros iniciales.
 * Si más adelante el equipo incorpora Apache POI, solo hay que reemplazar el cuerpo de escribir().
 */
/** TEMPORAL: se elimina en la fase 3. */
@Deprecated
public final class ExportadorXlsx {

    private ExportadorXlsx() { }

    public static void escribir(File destino, String hoja, TableModel modelo) throws IOException {
        try (ZipOutputStream zip = new ZipOutputStream(new FileOutputStream(destino))) {
            agregar(zip, "[Content_Types].xml",
                "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
              + "<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\">"
              + "<Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/>"
              + "<Default Extension=\"xml\" ContentType=\"application/xml\"/>"
              + "<Override PartName=\"/xl/workbook.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml\"/>"
              + "<Override PartName=\"/xl/worksheets/sheet1.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>"
              + "<Override PartName=\"/xl/styles.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml\"/>"
              + "</Types>");

            agregar(zip, "_rels/.rels",
                "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
              + "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">"
              + "<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\"xl/workbook.xml\"/>"
              + "</Relationships>");

            agregar(zip, "xl/workbook.xml",
                "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
              + "<workbook xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" "
              + "xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\">"
              + "<sheets><sheet name=\"" + xml(nombreHoja(hoja)) + "\" sheetId=\"1\" r:id=\"rId1\"/></sheets></workbook>");

            agregar(zip, "xl/_rels/workbook.xml.rels",
                "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
              + "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">"
              + "<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet1.xml\"/>"
              + "<Relationship Id=\"rId2\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles\" Target=\"styles.xml\"/>"
              + "</Relationships>");

            // Estilo 0 = normal, estilo 1 = encabezado (negrita, texto blanco, fondo azul institucional)
            agregar(zip, "xl/styles.xml",
                "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
              + "<styleSheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\">"
              + "<fonts count=\"2\"><font><sz val=\"11\"/><name val=\"Calibri\"/></font>"
              + "<font><b/><sz val=\"11\"/><color rgb=\"FFFFFFFF\"/><name val=\"Calibri\"/></font></fonts>"
              + "<fills count=\"3\"><fill><patternFill patternType=\"none\"/></fill>"
              + "<fill><patternFill patternType=\"gray125\"/></fill>"
              + "<fill><patternFill patternType=\"solid\"><fgColor rgb=\"FF1C355E\"/><bgColor indexed=\"64\"/></patternFill></fill></fills>"
              + "<borders count=\"1\"><border><left/><right/><top/><bottom/><diagonal/></border></borders>"
              + "<cellStyleXfs count=\"1\"><xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"0\"/></cellStyleXfs>"
              + "<cellXfs count=\"2\"><xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"0\" xfId=\"0\"/>"
              + "<xf numFmtId=\"0\" fontId=\"1\" fillId=\"2\" borderId=\"0\" xfId=\"0\" applyFont=\"1\" applyFill=\"1\"/></cellXfs>"
              + "<cellStyles count=\"1\"><cellStyle name=\"Normal\" xfId=\"0\" builtinId=\"0\"/></cellStyles>"
              + "</styleSheet>");

            agregar(zip, "xl/worksheets/sheet1.xml", hojaXml(modelo));
        }
    }

    private static String hojaXml(TableModel m) {
        int cols = m.getColumnCount();
        int[] ancho = new int[cols];
        for (int c = 0; c < cols; c++) ancho[c] = m.getColumnName(c).length();
        for (int r = 0; r < m.getRowCount(); r++) {
            for (int c = 0; c < cols; c++) {
                Object v = m.getValueAt(r, c);
                if (v != null) ancho[c] = Math.max(ancho[c], v.toString().length());
            }
        }

        StringBuilder sb = new StringBuilder(4096);
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>")
          .append("<worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\">")
          .append("<sheetViews><sheetView workbookViewId=\"0\"><pane ySplit=\"1\" topLeftCell=\"A2\" activePane=\"bottomLeft\" state=\"frozen\"/></sheetView></sheetViews>")
          .append("<cols>");
        for (int c = 0; c < cols; c++) {
            sb.append("<col min=\"").append(c + 1).append("\" max=\"").append(c + 1)
              .append("\" width=\"").append(Math.min(60, ancho[c] + 4)).append("\" customWidth=\"1\"/>");
        }
        sb.append("</cols><sheetData>");

        sb.append("<row r=\"1\">");
        for (int c = 0; c < cols; c++) celda(sb, c, 1, m.getColumnName(c), 1);
        sb.append("</row>");
        for (int r = 0; r < m.getRowCount(); r++) {
            sb.append("<row r=\"").append(r + 2).append("\">");
            for (int c = 0; c < cols; c++) {
                Object v = m.getValueAt(r, c);
                celda(sb, c, r + 2, v == null ? "" : v.toString(), 0);
            }
            sb.append("</row>");
        }
        sb.append("</sheetData></worksheet>");
        return sb.toString();
    }

    private static void celda(StringBuilder sb, int col, int fila, String texto, int estilo) {
        sb.append("<c r=\"").append(letra(col)).append(fila).append("\" t=\"inlineStr\" s=\"").append(estilo)
          .append("\"><is><t xml:space=\"preserve\">").append(xml(texto)).append("</t></is></c>");
    }

    private static String letra(int col) {
        StringBuilder s = new StringBuilder();
        for (int n = col; n >= 0; n = n / 26 - 1) s.insert(0, (char) ('A' + n % 26));
        return s.toString();
    }

    private static String nombreHoja(String n) {
        String limpio = n.replaceAll("[\\[\\]:*?/\\\\]", " ").trim();
        if (limpio.isEmpty()) limpio = "Hoja1";
        return limpio.length() > 31 ? limpio.substring(0, 31) : limpio;
    }

    private static String xml(String s) {
        StringBuilder sb = new StringBuilder(s.length() + 8);
        for (char ch : s.toCharArray()) {
            switch (ch) {
                case '&' -> sb.append("&amp;");
                case '<' -> sb.append("&lt;");
                case '>' -> sb.append("&gt;");
                case '"' -> sb.append("&quot;");
                default -> { if (ch >= 0x20 || ch == '\t' || ch == '\n' || ch == '\r') sb.append(ch); }
            }
        }
        return sb.toString();
    }

    private static void agregar(ZipOutputStream zip, String nombre, String contenido) throws IOException {
        zip.putNextEntry(new ZipEntry(nombre));
        zip.write(contenido.getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
    }
}
