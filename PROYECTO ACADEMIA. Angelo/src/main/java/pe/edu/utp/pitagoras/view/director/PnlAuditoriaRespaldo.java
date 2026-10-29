package pe.edu.utp.pitagoras.view.director;

import pe.edu.utp.pitagoras.config.EstilosUI;
import pe.edu.utp.pitagoras.temporal.EstadoSistema;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.io.File;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

/** CU09 / CU10 · RF09 / RF10 / RNF07: bitácora de auditoría y copias de seguridad (solo Director/Coordinador). */
public class PnlAuditoriaRespaldo extends JPanel {

    private static final String CARPETA_BACKUP = "D:\\Backups_Pitagoras\\";
    private static final String BASE_DATOS = "BD_Pitagoras";

    private static final int AUD_FECHA = 1, AUD_USUARIO = 2, AUD_MODULO = 3, AUD_ACCION = 4;

    private DefaultTableModel modeloAuditoria, modeloRespaldos;
    private JTable tablaAudit;
    private TableRowSorter<DefaultTableModel> sorter;
    private JTextField txtFiltroUsuario, txtDesde, txtHasta;
    private JComboBox<String> cboFiltroModulo, cboFiltroAccion;
    private JLabel lblConteo;

    public PnlAuditoriaRespaldo() {
        initComponents();
        // Los eventos nuevos (cierre de periodo, cambio de contraseña...) aparecen solos en la bitácora.
        EstadoSistema.addOyenteBitacora(ev -> SwingUtilities.invokeLater(() -> {
            modeloAuditoria.insertRow(0, ev.aFila());
            actualizarConteo();
        }));
    }

    private void initComponents() {
        setLayout(null);
        setBackground(EstilosUI.BG_MAIN);
        setPreferredSize(new Dimension(980, 700));

        EstilosUI.encabezado(this, "Auditoría del Sistema y Copias de Seguridad",
                "Trazabilidad de modificaciones críticas (notas, asistencia, periodos) y respaldo en SQL Server (.bak)");

        construirBitacora();
        construirRespaldos();
    }

    // ══ Bitácora (CU09) ═══════════════════════════════════════════
    private void construirBitacora() {
        JPanel cardAudit = EstilosUI.card(32, 90, 916, 330);

        cardAudit.add(EstilosUI.label("Bitácora de Eventos y Modificaciones Críticas (RF09)", 20, 14, 480, 22, 15, Font.BOLD, EstilosUI.TEXT_DARK));
        lblConteo = EstilosUI.label("", 600, 16, 296, 18, 12, Font.PLAIN, EstilosUI.TEXT_GRAY);
        lblConteo.setHorizontalAlignment(SwingConstants.RIGHT);
        cardAudit.add(lblConteo);

        // Fila de filtros: usuario · módulo · acción · desde · hasta · [Filtrar] [Restablecer]
        txtFiltroUsuario = EstilosUI.campoTexto("Usuario");
        txtFiltroUsuario.setBounds(20, 44, 120, 34);
        cardAudit.add(txtFiltroUsuario);

        cboFiltroModulo = EstilosUI.combo(new String[]{"Todos los módulos", "Calificacion", "Asistencia", "Periodo", "Usuario"});
        cboFiltroModulo.setFont(EstilosUI.font(Font.PLAIN, 12));
        cboFiltroModulo.setBounds(148, 44, 150, 34);
        cardAudit.add(cboFiltroModulo);

        cboFiltroAccion = EstilosUI.combo(new String[]{"Todas las acciones", "UPDATE_NOTA", "UPDATE_ASISTENCIA",
                "CERRAR_PERIODO", "REABRIR_PERIODO", "RESET_PASSWORD", "CAMBIAR_PASSWORD"});
        cboFiltroAccion.setFont(EstilosUI.font(Font.PLAIN, 12));
        cboFiltroAccion.setBounds(306, 44, 160, 34);
        cardAudit.add(cboFiltroAccion);

        txtDesde = EstilosUI.campoTexto("Desde");
        txtDesde.setToolTipText("Fecha inicial, formato dd/mm/aaaa");
        txtDesde.setBounds(474, 44, 90, 34);
        cardAudit.add(txtDesde);

        txtHasta = EstilosUI.campoTexto("Hasta");
        txtHasta.setToolTipText("Fecha final, formato dd/mm/aaaa");
        txtHasta.setBounds(572, 44, 90, 34);
        cardAudit.add(txtHasta);

        JButton btnBuscar = EstilosUI.botonPrimario("Filtrar bitácora");
        btnBuscar.setBounds(674, 44, 120, 34);
        btnBuscar.setFont(EstilosUI.font(Font.BOLD, 12));
        btnBuscar.addActionListener(e -> aplicarFiltros());
        cardAudit.add(btnBuscar);

        JButton btnLimpiar = EstilosUI.botonSecundario("Restablecer");
        btnLimpiar.setBounds(802, 44, 94, 34);
        btnLimpiar.setFont(EstilosUI.font(Font.BOLD, 11));
        btnLimpiar.addActionListener(e -> restablecerFiltros());
        cardAudit.add(btnLimpiar);

        String[] cols = {"ID", "Fecha y Hora", "Usuario", "Módulo", "Acción", "Registro (PK)", "Anterior", "Nuevo", "Motivo"};
        modeloAuditoria = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        List<EstadoSistema.EventoBitacora> eventos = EstadoSistema.getBitacora();
        for (int i = eventos.size() - 1; i >= 0; i--) modeloAuditoria.addRow(eventos.get(i).aFila());   // más reciente arriba

        tablaAudit = EstilosUI.tabla(modeloAuditoria);
        sorter = new TableRowSorter<>(modeloAuditoria);
        tablaAudit.setRowSorter(sorter);
        tablaAudit.setFont(EstilosUI.font(Font.PLAIN, 12));
        EstilosUI.anchos(tablaAudit, 40, 130, 110, 90, 140, 135, 85, 110, 100);
        EstilosUI.centrar(tablaAudit, 0);
        cardAudit.add(EstilosUI.scroll(tablaAudit, 20, 90, 876, 222));
        add(cardAudit);
        actualizarConteo();
    }

    private void aplicarFiltros() {
        final String usuario = txtFiltroUsuario.getText().trim().toLowerCase();
        final String modulo = cboFiltroModulo.getSelectedIndex() == 0 ? null : (String) cboFiltroModulo.getSelectedItem();
        final String accion = cboFiltroAccion.getSelectedIndex() == 0 ? null : (String) cboFiltroAccion.getSelectedItem();

        final LocalDate desde, hasta;
        try {
            desde = parsearFecha(txtDesde.getText());
            hasta = parsearFecha(txtHasta.getText());
        } catch (DateTimeParseException ex) {
            EstilosUI.aviso(this, "Fecha no válida", "Escribe las fechas con el formato dd/mm/aaaa (por ejemplo, 28/10/2026).");
            return;
        }
        if (desde != null && hasta != null && desde.isAfter(hasta)) {
            EstilosUI.aviso(this, "Rango inválido", "La fecha «Desde» no puede ser posterior a «Hasta».");
            return;
        }

        sorter.setRowFilter(new RowFilter<DefaultTableModel, Integer>() {
            @Override
            public boolean include(Entry<? extends DefaultTableModel, ? extends Integer> e) {
                if (!usuario.isEmpty() && !e.getStringValue(AUD_USUARIO).toLowerCase().contains(usuario)) return false;
                if (modulo != null && !e.getStringValue(AUD_MODULO).equals(modulo)) return false;
                if (accion != null && !e.getStringValue(AUD_ACCION).equals(accion)) return false;
                if (desde != null || hasta != null) {
                    LocalDate f = LocalDate.parse(e.getStringValue(AUD_FECHA).substring(0, 10), EstadoSistema.FMT_FECHA);
                    if (desde != null && f.isBefore(desde)) return false;
                    if (hasta != null && f.isAfter(hasta)) return false;
                }
                return true;
            }
        });
        actualizarConteo();
    }

    private void restablecerFiltros() {
        txtFiltroUsuario.setText("");
        txtDesde.setText("");
        txtHasta.setText("");
        cboFiltroModulo.setSelectedIndex(0);
        cboFiltroAccion.setSelectedIndex(0);
        sorter.setRowFilter(null);
        actualizarConteo();
    }

    private LocalDate parsearFecha(String texto) {
        String t = texto.trim();
        return t.isEmpty() ? null : LocalDate.parse(t, DateTimeFormatter.ofPattern("d/M/uuuu"));
    }

    private void actualizarConteo() {
        lblConteo.setText("Mostrando " + tablaAudit.getRowCount() + " de " + modeloAuditoria.getRowCount() + " eventos");
    }

    // ══ Copias de seguridad (CU10) ════════════════════════════════
    private void construirRespaldos() {
        JPanel cardBackup = EstilosUI.card(32, 434, 916, 240);

        cardBackup.add(EstilosUI.label("Copias de Seguridad (RF10 / RNF07)", 20, 14, 410, 22, 15, Font.BOLD, EstilosUI.TEXT_DARK));

        JButton btnGenerarBak = EstilosUI.botonPrimario("Generar respaldo ahora (.bak)");
        btnGenerarBak.setBounds(440, 10, 230, 34);
        btnGenerarBak.addActionListener(e -> accionGenerarBackup());
        cardBackup.add(btnGenerarBak);

        JButton btnRestaurarBak = EstilosUI.botonSecundario("Restaurar base de datos");
        btnRestaurarBak.setBounds(682, 10, 214, 34);
        btnRestaurarBak.addActionListener(e -> accionRestaurarBackup());
        cardBackup.add(btnRestaurarBak);

        cardBackup.add(EstilosUI.label("Respaldo automático diario activo (20:00 h) · Se conservan las últimas 7 copias en "
                + CARPETA_BACKUP, 20, 48, 876, 18, 12, Font.PLAIN, EstilosUI.SUCCESS_GREEN));

        String[] cols = {"ID", "Fecha y Hora", "Usuario", "Tipo Operación", "Ruta del Archivo (.bak)", "Resultado"};
        Object[][] datos = {
            {"12", "28/10/2026 20:00", "SYSTEM_TASK",   "Respaldo Automático", "D:\\Backups_Pitagoras\\BD_Pitagoras_20261028.bak", "Exitoso"},
            {"11", "27/10/2026 20:00", "SYSTEM_TASK",   "Respaldo Automático", "D:\\Backups_Pitagoras\\BD_Pitagoras_20261027.bak", "Exitoso"},
            {"10", "26/10/2026 12:30", "dir_pitagoras", "Respaldo Manual",     "E:\\USB_Direccion\\BD_Pitagoras_PreCierre.bak",    "Exitoso"}
        };
        modeloRespaldos = new DefaultTableModel(datos, cols) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable tablaBak = EstilosUI.tabla(modeloRespaldos);
        tablaBak.setFont(EstilosUI.font(Font.PLAIN, 12));
        EstilosUI.anchos(tablaBak, 40, 125, 105, 160, 320, 90);
        EstilosUI.centrar(tablaBak, 0, 5);
        cardBackup.add(EstilosUI.scroll(tablaBak, 20, 74, 876, 154));
        add(cardBackup);
    }

    private void accionGenerarBackup() {
        String stamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmm"));
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Guardar copia de seguridad");
        chooser.setFileFilter(new FileNameExtensionFilter("Copia de seguridad SQL Server (*.bak)", "bak"));
        File carpeta = new File(CARPETA_BACKUP);
        if (carpeta.isDirectory()) chooser.setCurrentDirectory(carpeta);
        chooser.setSelectedFile(new File(BASE_DATOS + "_" + stamp + ".bak"));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;

        File destino = chooser.getSelectedFile();
        if (!destino.getName().toLowerCase().endsWith(".bak")) {
            destino = new File(destino.getParentFile(), destino.getName() + ".bak");
        }

        boolean ok = ejecutarBackupSQL(destino);
        agregarRespaldo("Respaldo Manual", destino.getAbsolutePath(), ok ? "Exitoso" : "Fallido");
        if (ok) EstilosUI.info(this, "Respaldo generado", "Copia de seguridad generada en:\n" + destino.getAbsolutePath());
        else    EstilosUI.error(this, "Respaldo fallido", "No se pudo generar la copia de seguridad. Revisa la conexión con SQL Server.");
    }

    private void accionRestaurarBackup() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Seleccionar copia de seguridad a restaurar");
        chooser.setFileFilter(new FileNameExtensionFilter("Copia de seguridad SQL Server (*.bak)", "bak"));
        File carpeta = new File(CARPETA_BACKUP);
        if (carpeta.isDirectory()) chooser.setCurrentDirectory(carpeta);
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;

        File origen = chooser.getSelectedFile();
        if (!origen.getName().toLowerCase().endsWith(".bak")) {
            EstilosUI.aviso(this, "Archivo no válido", "Selecciona un archivo de copia de seguridad con extensión .bak.");
            return;
        }

        // Operación destructiva: la confirmación es obligatoria y el botón por defecto es «Cancelar».
        Object[] opciones = {"Restaurar", "Cancelar"};
        int r = JOptionPane.showOptionDialog(this,
                "Se reemplazará TODA la información actual por la de:\n" + origen.getName()
                + "\n\nLos datos registrados después de esa copia se perderán y los demás usuarios\n"
                + "deberán cerrar sesión. Esta acción no se puede deshacer.\n\n¿Deseas continuar?",
                "Confirmar restauración", JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE,
                null, opciones, opciones[1]);
        if (r != 0) return;

        boolean ok = ejecutarRestoreSQL(origen);
        agregarRespaldo("Restauración", origen.getAbsolutePath(), ok ? "Exitoso" : "Fallido");
        if (ok) EstilosUI.info(this, "Restauración completada", "Base de datos restaurada con éxito.");
        else    EstilosUI.error(this, "Restauración fallida", "No se pudo restaurar la base de datos.");
    }

    private void agregarRespaldo(String tipo, String ruta, String resultado) {
        int max = 0;
        for (int i = 0; i < modeloRespaldos.getRowCount(); i++) {
            max = Math.max(max, Integer.parseInt(modeloRespaldos.getValueAt(i, 0).toString()));
        }
        modeloRespaldos.insertRow(0, new Object[]{
            String.valueOf(max + 1), LocalDateTime.now().format(EstadoSistema.FMT_FECHA_HORA),
            EstadoSistema.getUsuarioActual(), tipo, ruta, resultado });
    }

    // ── Punto de conexión con SQL Server (pendiente de la capa JDBC) ──
    /**
     * TODO (capa de datos): ejecutar con la conexión JDBC del proyecto
     *   BACKUP DATABASE [BD_Pitagoras] TO DISK = ? WITH INIT, CHECKSUM
     * IMPORTANTE: SQL Server escribe el archivo en el equipo DEL SERVIDOR; la ruta debe existir allí.
     * Por ahora simula un resultado exitoso.
     */
    private boolean ejecutarBackupSQL(File destino) {
        return true;
    }

    /**
     * TODO (capa de datos): conectado a la base «master»:
     *   ALTER DATABASE [BD_Pitagoras] SET SINGLE_USER WITH ROLLBACK IMMEDIATE;
     *   RESTORE DATABASE [BD_Pitagoras] FROM DISK = ? WITH REPLACE;
     *   ALTER DATABASE [BD_Pitagoras] SET MULTI_USER;
     * Por ahora simula un resultado exitoso.
     */
    private boolean ejecutarRestoreSQL(File origen) {
        return true;
    }
}
