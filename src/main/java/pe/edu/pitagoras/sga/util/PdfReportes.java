package pe.edu.pitagoras.sga.util;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import java.awt.Color;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import pe.edu.pitagoras.sga.exception.BusinessException;
import pe.edu.pitagoras.sga.model.academico.Alumno;
import pe.edu.pitagoras.sga.model.academico.CargaAcademica;
import pe.edu.pitagoras.sga.model.academico.Competencia;
import pe.edu.pitagoras.sga.model.academico.Curso;
import pe.edu.pitagoras.sga.model.academico.Periodo;
import pe.edu.pitagoras.sga.model.academico.Seccion;
import pe.edu.pitagoras.sga.model.evaluacion.Calificativo;
import pe.edu.pitagoras.sga.model.evaluacion.Consolidado;
import pe.edu.pitagoras.sga.model.evaluacion.FilaConsolidado;
import pe.edu.pitagoras.sga.model.evaluacion.FilaNotas;
import pe.edu.pitagoras.sga.model.evaluacion.Libreta;
import pe.edu.pitagoras.sga.model.evaluacion.ConclusionLibreta;
import pe.edu.pitagoras.sga.model.evaluacion.LineaLibreta;
import pe.edu.pitagoras.sga.model.evaluacion.ResumenArea;
import pe.edu.pitagoras.sga.model.evaluacion.ResumenAlumno;
import pe.edu.pitagoras.sga.model.evaluacion.ResumenAsistencia;

/** Reportes en PDF con OpenPDF: libreta (RF17), nómina, acta de notas, asistencia y consolidado (RF18, RF19). */
public final class PdfReportes {

    private static final String COLEGIO = "Colegio Particular Pitágoras - Cerro de Pasco";
    private static final Color MARINO = new Color(0x1F, 0x3A, 0x5F);
    private static final Color ACENTO = MARINO;
    private static final Color DORADO = new Color(0xC9, 0xA2, 0x27);
    private static final Color DORADO_SUAVE = new Color(0xFB, 0xF4, 0xDF);
    private static final Color BORDE = new Color(0xD5, 0xDB, 0xE3);
    private static final Color GRIS = new Color(0xF2, 0xF5, 0xF9);
    private static final Font TITULO = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 15, new Color(0x1F, 0x3A, 0x5F));
    private static final Font SUBTITULO = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.DARK_GRAY);
    private static final Font CABECERA = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.WHITE);
    private static final Font CELDA = FontFactory.getFont(FontFactory.HELVETICA, 9, Color.BLACK);
    private static final Font NEGRITA = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.BLACK);
    private static final Font PIE = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 8, Color.GRAY);
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", Locale.forLanguageTag("es-PE"));

    private PdfReportes() {
    }

    /**
     * RF17: libreta con la estructura del Informe de progreso del aprendizaje (MINEDU): datos de la
     * institución, competencias por bimestre, nivel final, conclusiones descriptivas, asistencia por
     * bimestre y situación final.
     */
    public static void libreta(Libreta l, File destino) {
        escribir(destino, PageSize.A4.rotate(), doc -> {
            PdfPTable cab = new PdfPTable(new float[]{1, 8, 1});
            cab.setWidthPercentage(100);
            cab.addCell(logo(46));
            PdfPCell titulo = new PdfPCell();
            titulo.setBorder(Rectangle.NO_BORDER);
            Paragraph t1 = new Paragraph("INFORME DE PROGRESO DEL APRENDIZAJE DEL ESTUDIANTE - " + l.anio(),
                    FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, MARINO));
            t1.setAlignment(Element.ALIGN_CENTER);
            Paragraph t2 = new Paragraph("Educación Básica Regular - Nivel Secundaria", SUBTITULO);
            t2.setAlignment(Element.ALIGN_CENTER);
            titulo.addElement(t1);
            titulo.addElement(t2);
            cab.addCell(titulo);
            PdfPCell vacia = new PdfPCell(new Phrase(""));
            vacia.setBorder(Rectangle.NO_BORDER);
            cab.addCell(vacia);
            doc.add(cab);
            doc.add(lineaDorada());

            var c = l.colegio();
            var a = l.alumno();
            PdfPTable datos = new PdfPTable(4);
            datos.setWidthPercentage(100);
            datos.setSpacingBefore(6);
            dato(datos, "DRE: ", c.dre(), 1);
            dato(datos, "UGEL: ", c.ugel(), 1);
            dato(datos, "Nivel: ", "Secundaria", 1);
            dato(datos, "Código modular: ", c.codigoModular(), 1);
            dato(datos, "Institución Educativa: ", c.nombre(), 1);
            dato(datos, "Grado y sección: ", a.seccion().etiqueta() + " (" + a.seccion().turno() + ")", 1);
            dato(datos, "Periodos: ", "4 bimestres", 1);
            dato(datos, "Escala: ", "AD, A, B, C", 1);
            dato(datos, "Apellidos y nombres: ", a.apellido().toUpperCase() + ", " + a.nombre(), 2);
            dato(datos, "DNI / Cód. estudiante: ", a.dni(), 2);
            datos.setSpacingAfter(8);
            doc.add(datos);

            PdfPTable t = tabla(new float[]{2.2f, 6.2f, 1.1f, 1.1f, 1.1f, 1.1f, 1.8f}, "ÁREA CURRICULAR", "COMPETENCIAS",
                    "1.er\nBIMESTRE", "2.°\nBIMESTRE", "3.er\nBIMESTRE", "4.°\nBIMESTRE",
                    "NIVEL DE LOGRO AL\nFINALIZAR EL\nPERIODO LECTIVO");
            java.util.Map<String, ResumenArea> resumen = new java.util.HashMap<>();
            l.areas().forEach(r -> resumen.put(r.area(), r));
            int i2 = 0;
            List<LineaLibreta> lineas = l.lineas();
            while (i2 < lineas.size()) {
                String area = lineas.get(i2).area();
                int fin = i2;
                while (fin < lineas.size() && lineas.get(fin).area().equals(area)) {
                    fin++;
                }
                PdfPCell celdaArea = new PdfPCell();
                celdaArea.setRowspan(fin - i2);
                celdaArea.setVerticalAlignment(Element.ALIGN_MIDDLE);
                celdaArea.setBorderColor(BORDE);
                celdaArea.setPadding(5);
                celdaArea.addElement(new Paragraph(area, NEGRITA));
                ResumenArea r = resumen.get(area);
                if (r != null && r.anual().isPresent()) {
                    celdaArea.addElement(new Paragraph("Calificativo anual del área: " + r.anual().get().name(), PIE));
                }
                t.addCell(celdaArea);
                for (int k = i2; k < fin; k++) {
                    LineaLibreta ln = lineas.get(k);
                    t.addCell(celda(ln.competencia(), CELDA, Element.ALIGN_LEFT));
                    for (int b = 1; b <= 4; b++) {
                        t.addCell(calificativo(ln.bimestre(b)));
                    }
                    PdfPCell finalCelda = ln.nivelFinal().map(PdfReportes::calificativo)
                            .orElse(celda("Pendiente", PIE, Element.ALIGN_CENTER));
                    finalCelda.setBackgroundColor(DORADO_SUAVE);
                    t.addCell(finalCelda);
                }
                i2 = fin;
            }
            doc.add(t);
            doc.add(new Paragraph("Escala: AD = logro destacado | A = logro esperado | B = en proceso | C = en inicio. "
                    + "El nivel de logro al finalizar el periodo lectivo corresponde al último nivel alcanzado en la "
                    + "competencia (bimestre 4).", PIE));

            // Conclusiones descriptivas
            doc.add(seccion("CONCLUSIONES DESCRIPTIVAS POR PERIODO"));
            if (l.conclusiones().isEmpty()) {
                doc.add(new Paragraph("Aún no se registran conclusiones descriptivas.", PIE));
            } else {
                PdfPTable tc = tabla(new float[]{2f, 3.6f, 1.1f, 0.8f, 7f}, "ÁREA", "COMPETENCIA", "PERIODO", "NIVEL",
                        "CONCLUSIÓN DESCRIPTIVA (logros, dificultades y recomendaciones)");
                for (ConclusionLibreta cd : l.conclusiones()) {
                    tc.addCell(celda(cd.area(), CELDA, Element.ALIGN_LEFT));
                    tc.addCell(celda(cd.competencia(), CELDA, Element.ALIGN_LEFT));
                    tc.addCell(celda(ordinal(cd.bimestre()) + " bimestre", CELDA, Element.ALIGN_CENTER));
                    tc.addCell(calificativo(cd.nivel()));
                    tc.addCell(celda(cd.texto(), CELDA, Element.ALIGN_LEFT));
                }
                doc.add(tc);
            }

            // Asistencia y situación final
            doc.add(seccion("ASISTENCIA Y SITUACIÓN FINAL"));
            PdfPTable ta = tabla(new float[]{4, 1.2f, 1.2f, 1.2f, 1.2f, 1.2f}, "", "1.er Bim.", "2.° Bim.", "3.er Bim.",
                    "4.° Bim.", "Total");
            filaAsistencia(ta, "Inasistencias justificadas", l, ResumenAsistencia::justificadas);
            filaAsistencia(ta, "Inasistencias injustificadas", l, ResumenAsistencia::faltas);
            filaAsistencia(ta, "Tardanzas", l, ResumenAsistencia::tardanzas);
            filaAsistencia(ta, "Asistencias", l, ResumenAsistencia::asistencias);
            ta.setKeepTogether(true);
            doc.add(ta);
            Paragraph sit = new Paragraph();
            sit.add(new Phrase("Situación final: ", NEGRITA));
            sit.add(new Phrase(l.situacionFinal(), CELDA));
            sit.setSpacingBefore(6);
            doc.add(sit);
            firmas(doc, "Director(a)", "Tutor(a)", "Padre / madre / apoderado");
        });
    }

    private static void dato(PdfPTable t, String etiqueta, String valor, int columnas) {
        Phrase f = new Phrase();
        f.add(new Phrase(etiqueta, NEGRITA));
        f.add(new Phrase(valor == null ? "" : valor, CELDA));
        PdfPCell c = new PdfPCell(f);
        c.setColspan(columnas);
        c.setPadding(4);
        c.setBorderColor(BORDE);
        c.setBackgroundColor(GRIS);
        t.addCell(c);
    }

    private static void filaAsistencia(PdfPTable t, String titulo, Libreta l,
                                       java.util.function.ToIntFunction<ResumenAsistencia> valor) {
        t.addCell(celda(titulo, NEGRITA, Element.ALIGN_LEFT));
        for (int b = 1; b <= 4; b++) {
            ResumenAsistencia r = l.asistenciaBimestre().get(b);
            t.addCell(celda(r == null ? "-" : String.valueOf(valor.applyAsInt(r)), CELDA, Element.ALIGN_CENTER));
        }
        PdfPCell total = celda(String.valueOf(valor.applyAsInt(l.asistencia())), NEGRITA, Element.ALIGN_CENTER);
        total.setBackgroundColor(DORADO_SUAVE);
        t.addCell(total);
    }

    private static String ordinal(int b) {
        return switch (b) {
            case 1 -> "1.er";
            case 2 -> "2.°";
            case 3 -> "3.er";
            default -> "4.°";
        };
    }

    /** RF18: nómina de alumnos inscritos en una sección. */
    public static void nomina(Seccion seccion, int anio, List<Alumno> alumnos, File destino) {
        escribir(destino, PageSize.A4, doc -> {
            encabezado(doc, "Nómina de matrícula " + anio, seccion + "  |  " + alumnos.size() + " alumnos");
            PdfPTable t = tabla(new float[]{0.6f, 1.4f, 4, 4, 1.8f}, "N.°", "DNI", "Apellidos y nombres",
                    "Apoderado", "Teléfono");
            int n = 1;
            for (Alumno a : alumnos) {
                t.addCell(celda(String.valueOf(n++), CELDA, Element.ALIGN_CENTER));
                t.addCell(celda(a.dni(), CELDA, Element.ALIGN_CENTER));
                t.addCell(celda(a.nombreCompleto(), CELDA, Element.ALIGN_LEFT));
                t.addCell(celda(a.nombreApoderado(), CELDA, Element.ALIGN_LEFT));
                t.addCell(celda(a.telefonoApoderado() == null ? "-" : a.telefonoApoderado(), CELDA, Element.ALIGN_CENTER));
            }
            doc.add(t);
            firmas(doc);
        });
    }

    /** RF18: acta de calificativos de un área en una sección y periodo. */
    public static void actaNotas(CargaAcademica carga, Periodo periodo, List<Competencia> competencias,
                                 List<FilaNotas> filas, File destino) {
        escribir(destino, PageSize.A4.rotate(), doc -> {
            encabezado(doc, "Acta de calificativos - " + carga.curso().nombre(),
                    carga.seccion() + "  |  " + periodo.etiqueta() + "  |  Docente: " + carga.docenteNombre());
            float[] anchos = new float[competencias.size() + 3];
            String[] titulos = new String[competencias.size() + 3];
            anchos[0] = 0.5f;
            titulos[0] = "N.°";
            anchos[1] = 3.5f;
            titulos[1] = "Apellidos y nombres";
            for (int i = 0; i < competencias.size(); i++) {
                anchos[i + 2] = 1.2f;
                titulos[i + 2] = "C" + competencias.get(i).codigo();
            }
            anchos[anchos.length - 1] = 1.2f;
            titulos[titulos.length - 1] = "Área";
            PdfPTable t = tabla(anchos, titulos);
            int n = 1;
            for (FilaNotas f : filas) {
                t.addCell(celda(String.valueOf(n++), CELDA, Element.ALIGN_CENTER));
                t.addCell(celda(f.getAlumno(), CELDA, Element.ALIGN_LEFT));
                for (Competencia c : competencias) {
                    t.addCell(calificativo(f.get(c.codigo())));
                }
                t.addCell(calificativo(f.calificativoArea().orElse(null)));
            }
            doc.add(t);
            doc.add(new Paragraph(" "));
            for (Competencia c : competencias) {
                doc.add(new Paragraph("C" + c.codigo() + ": " + c.nombre(), PIE));
            }
            firmas(doc);
        });
    }

    /** RF18: resumen de asistencia de una sección en un área y periodo. */
    public static void asistencia(CargaAcademica carga, Periodo periodo, List<ResumenAlumno> filas, File destino) {
        escribir(destino, PageSize.A4, doc -> {
            encabezado(doc, "Consolidado de asistencia - " + carga.curso().nombre(),
                    carga.seccion() + "  |  " + periodo.etiqueta() + "  |  Docente: " + carga.docenteNombre());
            PdfPTable t = tabla(new float[]{0.6f, 4, 1.2f, 1.2f, 1.2f, 1.4f, 1.2f}, "N.°", "Apellidos y nombres",
                    "Asistió", "Tardanza", "Falta", "F. justif.", "% Asist.");
            int n = 1;
            for (ResumenAlumno r : filas) {
                ResumenAsistencia s = r.resumen();
                t.addCell(celda(String.valueOf(n++), CELDA, Element.ALIGN_CENTER));
                t.addCell(celda(r.alumno().nombreCompleto(), CELDA, Element.ALIGN_LEFT));
                t.addCell(celda(String.valueOf(s.asistencias()), CELDA, Element.ALIGN_CENTER));
                t.addCell(celda(String.valueOf(s.tardanzas()), CELDA, Element.ALIGN_CENTER));
                t.addCell(celda(String.valueOf(s.faltas()), CELDA, Element.ALIGN_CENTER));
                t.addCell(celda(String.valueOf(s.justificadas()), CELDA, Element.ALIGN_CENTER));
                t.addCell(celda(String.format(Locale.ROOT, "%.0f%%", s.porcentajeAsistencia()), NEGRITA,
                        Element.ALIGN_CENTER));
            }
            doc.add(t);
            firmas(doc);
        });
    }

    /** RF19: consolidado bimestral por estudiante (calificativos de área y asistencia). */
    public static void consolidado(Consolidado c, File destino) {
        escribir(destino, PageSize.A4.rotate(), doc -> {
            encabezado(doc, "Consolidado bimestral - " + c.periodo().etiqueta(),
                    c.seccion() + "  |  " + c.filas().size() + " alumnos");
            List<Curso> cursos = c.cursos();
            float[] anchos = new float[cursos.size() + 5];
            String[] titulos = new String[cursos.size() + 5];
            anchos[0] = 0.5f;
            titulos[0] = "N.°";
            anchos[1] = 3.6f;
            titulos[1] = "Apellidos y nombres";
            for (int i = 0; i < cursos.size(); i++) {
                anchos[i + 2] = 1.4f;
                titulos[i + 2] = cursos.get(i).nombre();
            }
            int k = cursos.size() + 2;
            anchos[k] = 0.9f;
            titulos[k] = "Faltas";
            anchos[k + 1] = 0.9f;
            titulos[k + 1] = "Tard.";
            anchos[k + 2] = 1f;
            titulos[k + 2] = "% Asist.";
            PdfPTable t = tabla(anchos, titulos);
            int n = 1;
            for (FilaConsolidado f : c.filas()) {
                t.addCell(celda(String.valueOf(n++), CELDA, Element.ALIGN_CENTER));
                t.addCell(celda(f.alumno().nombreCompleto(), CELDA, Element.ALIGN_LEFT));
                for (Curso curso : cursos) {
                    t.addCell(calificativo(f.porCurso().get(curso.cursoId())));
                }
                t.addCell(celda(String.valueOf(f.asistencia().faltas() + f.asistencia().justificadas()), CELDA,
                        Element.ALIGN_CENTER));
                t.addCell(celda(String.valueOf(f.asistencia().tardanzas()), CELDA, Element.ALIGN_CENTER));
                t.addCell(celda(String.format(Locale.ROOT, "%.0f%%", f.asistencia().porcentajeAsistencia()), CELDA,
                        Element.ALIGN_CENTER));
            }
            doc.add(t);
            firmas(doc);
        });
    }

    // ---------- piezas comunes ----------

    @FunctionalInterface
    private interface Contenido {
        void escribir(Document doc) throws DocumentException;
    }

    private static void escribir(File destino, Rectangle tamano, Contenido contenido) {
        Document doc = new Document(tamano, 36, 36, 36, 36);
        try (OutputStream out = new FileOutputStream(destino)) {
            PdfWriter.getInstance(doc, out);
            doc.addTitle("Reporte - " + COLEGIO);
            doc.addCreator("SGA Pitágoras");
            doc.open();
            contenido.escribir(doc);
            doc.close();
        } catch (IOException | DocumentException e) {
            throw new BusinessException("No se pudo generar el PDF en " + destino.getName() + ": " + e.getMessage());
        }
    }

    private static void encabezado(Document doc, String titulo, String subtitulo) throws DocumentException {
        PdfPTable cab = new PdfPTable(new float[]{1, 9});
        cab.setWidthPercentage(100);
        cab.addCell(logo(40));
        PdfPCell textos = new PdfPCell();
        textos.setBorder(Rectangle.NO_BORDER);
        textos.addElement(new Paragraph(COLEGIO, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, DORADO)));
        textos.addElement(new Paragraph(titulo, TITULO));
        textos.addElement(new Paragraph(subtitulo, SUBTITULO));
        cab.addCell(textos);
        doc.add(cab);
        doc.add(lineaDorada());
        Paragraph emitido = new Paragraph("Emitido el " + LocalDateTime.now().format(FECHA), PIE);
        emitido.setSpacingAfter(8);
        doc.add(emitido);
    }

    /** Escudo del colegio (/app/img/escudo.png); si no está, una celda vacía. */
    private static PdfPCell logo(float alto) {
        PdfPCell c;
        try {
            java.net.URL url = PdfReportes.class.getResource("/app/img/escudo.png");
            if (url == null) {
                throw new IOException("sin escudo");
            }
            com.lowagie.text.Image img = com.lowagie.text.Image.getInstance(url);
            img.scaleToFit(alto, alto);
            c = new PdfPCell(img, false);
        } catch (IOException | DocumentException e) {
            c = new PdfPCell(new Phrase(""));
        }
        c.setBorder(Rectangle.NO_BORDER);
        c.setVerticalAlignment(Element.ALIGN_MIDDLE);
        return c;
    }

    private static PdfPTable lineaDorada() {
        PdfPTable t = new PdfPTable(1);
        t.setWidthPercentage(100);
        PdfPCell c = new PdfPCell(new Phrase(""));
        c.setFixedHeight(2.5f);
        c.setBorder(Rectangle.NO_BORDER);
        c.setBackgroundColor(DORADO);
        t.addCell(c);
        t.setSpacingBefore(4);
        return t;
    }

    private static Paragraph seccion(String texto) {
        Paragraph p = new Paragraph(texto, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, MARINO));
        p.setSpacingBefore(12);
        p.setSpacingAfter(4);
        return p;
    }

    private static PdfPTable tabla(float[] anchos, String... titulos) {
        PdfPTable t = new PdfPTable(anchos);
        t.setWidthPercentage(100);
        t.setHeaderRows(1);
        for (String titulo : titulos) {
            PdfPCell c = new PdfPCell(new Phrase(titulo, CABECERA));
            c.setBackgroundColor(ACENTO);
            c.setHorizontalAlignment(Element.ALIGN_CENTER);
            c.setVerticalAlignment(Element.ALIGN_MIDDLE);
            c.setPadding(5);
            c.setBorderColor(BORDE);
            c.setBorderWidthBottom(2f);
            c.setBorderColorBottom(DORADO);
            t.addCell(c);
        }
        return t;
    }

    private static PdfPCell celda(String texto, Font fuente, int alineacion) {
        PdfPCell c = new PdfPCell(new Phrase(texto == null ? "" : texto, fuente));
        c.setHorizontalAlignment(alineacion);
        c.setVerticalAlignment(Element.ALIGN_MIDDLE);
        c.setPadding(4);
        c.setBorderColor(BORDE);
        return c;
    }

    private static PdfPCell calificativo(Calificativo c) {
        if (c == null) {
            return celda("-", PIE, Element.ALIGN_CENTER);
        }
        Color color = switch (c) {
            case AD -> new Color(0x1E, 0x6B, 0x47);
            case A -> new Color(0x1F, 0x3A, 0x5F);
            case B -> new Color(0x8A, 0x63, 0x0B);
            case C -> new Color(0xA3, 0x2A, 0x22);
        };
        PdfPCell celda = celda(c.name(), FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, color), Element.ALIGN_CENTER);
        if (c == Calificativo.C) {
            celda.setBackgroundColor(new Color(0xFB, 0xEE, 0xEC));
        }
        return celda;
    }

    private static PdfPTable resumenAsistencia(ResumenAsistencia r) {
        PdfPTable t = tabla(new float[]{1, 1, 1, 1, 1}, "Asistencias", "Tardanzas", "Faltas", "Faltas justificadas",
                "% Asistencia");
        for (String v : new String[]{String.valueOf(r.asistencias()), String.valueOf(r.tardanzas()),
                String.valueOf(r.faltas()), String.valueOf(r.justificadas()),
                String.format(Locale.ROOT, "%.1f%%", r.porcentajeAsistencia())}) {
            PdfPCell c = celda(v, NEGRITA, Element.ALIGN_CENTER);
            c.setBackgroundColor(GRIS);
            t.addCell(c);
        }
        return t;
    }

    private static void firmas(Document doc) throws DocumentException {
        firmas(doc, "Dirección", "Docente / Tutor(a)");
    }

    private static void firmas(Document doc, String... cargos) throws DocumentException {
        PdfPTable t = new PdfPTable(cargos.length);
        t.setWidthPercentage(85);
        t.setSpacingBefore(40);
        t.setKeepTogether(true);
        for (String cargo : cargos) {
            PdfPCell c = new PdfPCell();
            c.setBorder(Rectangle.NO_BORDER);
            Paragraph linea = new Paragraph("______________________________", PIE);
            linea.setAlignment(Element.ALIGN_CENTER);
            Paragraph nombre = new Paragraph(cargo, PIE);
            nombre.setAlignment(Element.ALIGN_CENTER);
            c.addElement(linea);
            c.addElement(nombre);
            t.addCell(c);
        }
        doc.add(t);
    }
}
