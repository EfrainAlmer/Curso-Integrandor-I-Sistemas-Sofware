package pe.edu.pitagoras.sga.view.director;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.time.LocalDate;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import pe.edu.pitagoras.sga.controller.DirectorController;
import pe.edu.pitagoras.sga.model.gestion.CopiaSeguridad;
import pe.edu.pitagoras.sga.model.gestion.EventoAuditoria;
import pe.edu.pitagoras.sga.view.comun.Combos;
import pe.edu.pitagoras.sga.view.comun.Fechas;
import pe.edu.pitagoras.sga.view.comun.Mensajes;
import pe.edu.pitagoras.sga.view.comun.ModeloTabla;
import pe.edu.pitagoras.sga.view.comun.PanelModulo;
import pe.edu.pitagoras.sga.view.comun.RenderInsignia;
import pe.edu.pitagoras.sga.view.comun.Tarea;
import pe.edu.pitagoras.sga.view.comun.Tarjeta;
import pe.edu.pitagoras.sga.view.comun.Tema;

/** RF09 y RF10: bitácora de auditoría (quién cambió qué y cuándo) y copias de seguridad. */
public class PnlAuditoriaRespaldo extends PanelModulo {

    private static final String TODAS = "Todas las acciones";

    private final transient DirectorController controller;
    private final JSpinner spDesde = Fechas.selector(LocalDate.now().minusDays(30));
    private final JSpinner spHasta = Fechas.selector(LocalDate.now());
    private final JComboBox<String> cboAccion = Combos.nuevo();
    private final ModeloTabla<EventoAuditoria> modeloEventos = new ModeloTabla<EventoAuditoria>()
            .columna("Fecha y hora", e -> Fechas.texto(e.fecha()))
            .columna("Usuario", EventoAuditoria::usuario)
            .columna("Acción", String.class, EventoAuditoria::accion)
            .columna("Tabla", EventoAuditoria::tabla)
            .columna("Registro", EventoAuditoria::clave)
            .columna("Valor anterior", e -> e.valorAnterior() == null ? "-" : e.valorAnterior())
            .columna("Valor nuevo", EventoAuditoria::valorNuevo);
    private final ModeloTabla<CopiaSeguridad> modeloCopias = new ModeloTabla<CopiaSeguridad>()
            .columna("Fecha", c -> Fechas.texto(c.fecha()))
            .columna("Archivo en el servidor", CopiaSeguridad::ruta)
            .columna("Generada por", c -> c.usuario() == null ? "-" : c.usuario())
            .columna("Estado", String.class, c -> "EXITOSO".equals(c.estado()) ? "Exitoso" : "Fallido");
    private final JTable tablaCopias = new JTable(modeloCopias);

    public PnlAuditoriaRespaldo(DirectorController controller) {
        super("Auditoría y respaldo", "Bitácora de cambios en notas, asistencia y configuración; copias de seguridad (RF09, RF10)");
        this.controller = controller;

        Combos.llenar(cboAccion, List.of(TODAS, "MODIFICAR", "REGISTRAR", "CERRAR_PERIODO", "REABRIR_PERIODO",
                "CREAR_USUARIO", "ESTADO_USUARIO", "RESTABLECER_CLAVE", "INSCRIBIR", "ACTUALIZAR_ALUMNO",
                "ASIGNAR_CARGA", "ACTUALIZAR_DOCENTE", "CONFIGURAR", "BACKUP", "RESTAURAR"));
        cboAccion.setPreferredSize(new java.awt.Dimension(220, 36));
        spDesde.setPreferredSize(new java.awt.Dimension(140, 36));
        spHasta.setPreferredSize(new java.awt.Dimension(140, 36));
        JButton buscar = Tema.botonPrimario("Buscar");
        JPanel filtros = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        filtros.setOpaque(false);
        filtros.add(Tema.etiqueta("Desde"));
        filtros.add(spDesde);
        filtros.add(Tema.etiqueta("Hasta"));
        filtros.add(spHasta);
        filtros.add(cboAccion);
        filtros.add(buscar);
        JTable tablaEventos = new JTable(modeloEventos);
        tablaEventos.getColumnModel().getColumn(2).setCellRenderer(new RenderInsignia(true));
        tablaEventos.getColumnModel().getColumn(2).setPreferredWidth(150);
        tablaEventos.getColumnModel().getColumn(4).setPreferredWidth(220);
        tablaEventos.getColumnModel().getColumn(6).setPreferredWidth(220);
        JPanel bitacora = new JPanel(new BorderLayout(0, 12));
        bitacora.setOpaque(false);
        bitacora.setBorder(javax.swing.BorderFactory.createEmptyBorder(12, 0, 0, 0));
        bitacora.add(filtros, BorderLayout.NORTH);
        bitacora.add(Tema.tabla(tablaEventos), BorderLayout.CENTER);
        bitacora.add(Tema.subtitulo("Se muestran como máximo los 500 eventos más recientes del rango."), BorderLayout.SOUTH);

        JButton generar = Tema.botonPrimario("Generar copia ahora");
        JButton restaurar = Tema.botonPeligro("Restaurar copia seleccionada");
        JPanel accionesRespaldo = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        accionesRespaldo.setOpaque(false);
        accionesRespaldo.add(generar);
        accionesRespaldo.add(restaurar);
        tablaCopias.getColumnModel().getColumn(1).setPreferredWidth(420);
        tablaCopias.getColumnModel().getColumn(3).setCellRenderer(new RenderInsignia(true));
        JPanel respaldo = new JPanel(new BorderLayout(0, 12));
        respaldo.setOpaque(false);
        respaldo.setBorder(javax.swing.BorderFactory.createEmptyBorder(12, 0, 0, 0));
        respaldo.add(accionesRespaldo, BorderLayout.NORTH);
        respaldo.add(Tema.tabla(tablaCopias), BorderLayout.CENTER);
        respaldo.add(Tema.subtitulo("<html>El respaldo diario automático se programa en el servidor (Programador de "
                + "tareas de Windows), conservando las últimas 7 copias (RNF07).<br>Desde aquí puede generar una copia "
                + "manual o restaurar una existente. El archivo .bak queda en el disco del servidor.</html>"),
                BorderLayout.SOUTH);

        JTabbedPane pestanas = new JTabbedPane();
        pestanas.addTab("Bitácora de auditoría", bitacora);
        pestanas.addTab("Copias de seguridad", respaldo);
        Tarjeta card = new Tarjeta();
        card.setLayout(new BorderLayout());
        card.add(pestanas, BorderLayout.CENTER);
        contenido.add(card, BorderLayout.CENTER);

        buscar.addActionListener(e -> buscar());
        generar.addActionListener(e -> generar());
        restaurar.addActionListener(e -> restaurar());
    }

    @Override
    protected void alMostrar(boolean primeraVez) {
        buscar();
        Tarea.ejecutar(this, controller::copias, modeloCopias::setFilas);
    }

    private void buscar() {
        LocalDate desde = Fechas.leer(spDesde);
        LocalDate hasta = Fechas.leer(spHasta);
        String accion = TODAS.equals(cboAccion.getSelectedItem()) ? null : (String) cboAccion.getSelectedItem();
        Tarea.ejecutar(this, () -> controller.eventos(desde, hasta, accion), modeloEventos::setFilas);
    }

    private void generar() {
        if (Mensajes.confirmar(this, "¿Generar una copia de seguridad de la base de datos ahora?")) {
            Tarea.ejecutar(this, controller::generarCopia, c -> {
                Mensajes.info(this, "Copia generada en el servidor:\n" + c.ruta());
                alMostrar(false);
            });
        }
    }

    private void restaurar() {
        int f = tablaCopias.getSelectedRow();
        if (f < 0) {
            Mensajes.aviso(this, "Seleccione una copia de la lista.");
            return;
        }
        CopiaSeguridad c = modeloCopias.getFila(tablaCopias.convertRowIndexToModel(f));
        String confirmacion = JOptionPane.showInputDialog(this, "<html>Restaurar reemplaza TODOS los datos actuales por "
                + "los de la copia del " + Fechas.texto(c.fecha()) + ".<br>Se desconectará a todos los usuarios."
                + "<br><br>Escriba RESTAURAR para confirmar:</html>", "Restaurar copia", JOptionPane.WARNING_MESSAGE);
        if ("RESTAURAR".equals(confirmacion)) {
            Tarea.ejecutarAccion(this, () -> controller.restaurar(c), () -> {
                Mensajes.info(this, "Base de datos restaurada. Se recomienda cerrar sesión y volver a ingresar.");
                alMostrar(false);
            });
        }
    }
}
