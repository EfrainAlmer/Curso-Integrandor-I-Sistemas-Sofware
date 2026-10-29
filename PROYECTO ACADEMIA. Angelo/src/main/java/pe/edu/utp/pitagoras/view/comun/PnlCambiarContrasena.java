package pe.edu.utp.pitagoras.view.comun;

import pe.edu.utp.pitagoras.config.EstilosUI;
import pe.edu.utp.pitagoras.temporal.EstadoSistema;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.util.Arrays;

/**
 * CU02 / RF02 / RNF02: cambio de contraseña para todos los perfiles.
 * Con {@code primerIngreso = true} muestra el aviso de cambio obligatorio y ejecuta
 * {@code alCambiar} cuando la contraseña se actualiza (para continuar al sistema).
 */
public class PnlCambiarContrasena extends JPanel {

    private static final int MIN_LARGO = 8;
    private static final String PUNTO = "\u25CF";

    private final boolean primerIngreso;
    private final Runnable alCambiar;

    private JPasswordField txtActual, txtNueva, txtConfirmar;
    private JCheckBox chkMostrar;
    private JLabel lblError;
    private JLabel[] lblReq;
    private JLabel lblFuerza;
    private BarraFuerza barra;

    private static final String[] REQUISITOS = {
        "Mínimo " + MIN_LARGO + " caracteres",
        "Al menos una letra mayúscula",
        "Al menos una letra minúscula",
        "Al menos un número",
        "No contiene tu nombre de usuario",
        "Es distinta de la contraseña actual",
        "Coincide con la confirmación"
    };

    public PnlCambiarContrasena() {
        this(false, null);
    }

    public PnlCambiarContrasena(boolean primerIngreso, Runnable alCambiar) {
        this.primerIngreso = primerIngreso;
        this.alCambiar = alCambiar;
        initComponents();
    }

    private void initComponents() {
        setLayout(null);
        setBackground(EstilosUI.BG_MAIN);
        setPreferredSize(new Dimension(980, 700));

        EstilosUI.encabezado(this, "Cambiar contraseña",
                "Actualiza tu credencial de acceso · Se almacena cifrada con hash y sal, nunca en texto plano (RF02, RNF02)");

        int yForm = 92;
        if (primerIngreso) {
            JLabel aviso = EstilosUI.label("Es tu primer ingreso: por seguridad debes cambiar la contraseña temporal para continuar.",
                    32, 88, 916, 30, 13, Font.BOLD, EstilosUI.WARNING_AMBER);
            add(aviso);
            yForm = 126;
        }

        // ── Formulario ────────────────────────────────────────────
        JPanel cardForm = EstilosUI.card(32, yForm, 520, 460);

        cardForm.add(EstilosUI.etiquetaCampo("USUARIO", 24, 20, 200));
        cardForm.add(EstilosUI.label(EstadoSistema.getUsuarioActual(), 24, 38, 472, 22, 14, Font.BOLD, EstilosUI.TEXT_DARK));

        cardForm.add(EstilosUI.etiquetaCampo("CONTRASEÑA ACTUAL", 24, 76, 200));
        txtActual = EstilosUI.campoPassword();
        txtActual.setBounds(24, 96, 472, 38);
        cardForm.add(txtActual);

        cardForm.add(EstilosUI.etiquetaCampo("NUEVA CONTRASEÑA", 24, 150, 200));
        txtNueva = EstilosUI.campoPassword();
        txtNueva.setBounds(24, 170, 472, 38);
        cardForm.add(txtNueva);

        barra = new BarraFuerza();
        barra.setBounds(24, 216, 340, 8);
        cardForm.add(barra);
        lblFuerza = EstilosUI.label("", 376, 208, 120, 20, 12, Font.BOLD, EstilosUI.TEXT_GRAY);
        lblFuerza.setHorizontalAlignment(SwingConstants.RIGHT);
        cardForm.add(lblFuerza);

        cardForm.add(EstilosUI.etiquetaCampo("CONFIRMAR NUEVA CONTRASEÑA", 24, 240, 260));
        txtConfirmar = EstilosUI.campoPassword();
        txtConfirmar.setBounds(24, 260, 472, 38);
        cardForm.add(txtConfirmar);

        chkMostrar = new JCheckBox("Mostrar contraseñas");
        chkMostrar.setFont(EstilosUI.font(Font.PLAIN, 12));
        chkMostrar.setForeground(EstilosUI.TEXT_GRAY);
        chkMostrar.setOpaque(false);
        chkMostrar.setFocusPainted(false);
        chkMostrar.setBounds(22, 308, 200, 24);
        chkMostrar.addActionListener(e -> alternarVisibilidad());
        cardForm.add(chkMostrar);

        lblError = new JLabel("");
        lblError.setFont(EstilosUI.font(Font.BOLD, 12));
        lblError.setForeground(EstilosUI.DANGER_RED);
        lblError.setVerticalAlignment(SwingConstants.TOP);
        lblError.setBounds(24, 340, 472, 36);
        cardForm.add(lblError);

        JButton btnGuardar = EstilosUI.botonPrimario("Guardar nueva contraseña");
        btnGuardar.setBounds(24, 392, 250, 40);
        btnGuardar.addActionListener(e -> accionGuardar());
        cardForm.add(btnGuardar);

        JButton btnLimpiar = EstilosUI.botonSecundario("Limpiar");
        btnLimpiar.setBounds(286, 392, 120, 40);
        btnLimpiar.addActionListener(e -> limpiarCampos());
        cardForm.add(btnLimpiar);
        add(cardForm);

        // ── Requisitos en vivo ────────────────────────────────────
        JPanel cardReq = EstilosUI.card(572, yForm, 376, 330);
        cardReq.add(EstilosUI.label("Requisitos de la contraseña", 20, 18, 330, 22, 15, Font.BOLD, EstilosUI.TEXT_DARK));
        lblReq = new JLabel[REQUISITOS.length];
        for (int i = 0; i < REQUISITOS.length; i++) {
            lblReq[i] = EstilosUI.label(PUNTO + "  " + REQUISITOS[i], 20, 56 + i * 30, 336, 22, 13, Font.PLAIN, EstilosUI.TEXT_GRAY);
            cardReq.add(lblReq[i]);
        }
        JLabel nota = new JLabel("<html>Tras " + EstadoSistema.MAX_INTENTOS + " intentos fallidos con la contraseña actual, "
                + "el cambio se bloquea " + EstadoSistema.MINUTOS_BLOQUEO + " minutos.</html>");
        nota.setFont(EstilosUI.font(Font.PLAIN, 12));
        nota.setForeground(EstilosUI.TEXT_GRAY);
        nota.setBounds(20, 268, 336, 44);
        cardReq.add(nota);
        add(cardReq);

        DocumentListener dl = new DocumentListener() {
            public void insertUpdate(DocumentEvent e)  { actualizarRequisitos(); }
            public void removeUpdate(DocumentEvent e)  { actualizarRequisitos(); }
            public void changedUpdate(DocumentEvent e) { actualizarRequisitos(); }
        };
        txtActual.getDocument().addDocumentListener(dl);
        txtNueva.getDocument().addDocumentListener(dl);
        txtConfirmar.getDocument().addDocumentListener(dl);
        txtConfirmar.addActionListener(e -> accionGuardar());
        actualizarRequisitos();
    }

    // ── Validación ────────────────────────────────────────────────
    /** @return para cada requisito, si se cumple con los valores actuales de los campos */
    private boolean[] evaluar(char[] actual, char[] nueva, char[] confirmar) {
        boolean mayus = false, minus = false, digito = false;
        for (char c : nueva) {
            if (Character.isUpperCase(c)) mayus = true;
            else if (Character.isLowerCase(c)) minus = true;
            else if (Character.isDigit(c)) digito = true;
        }
        String usuario = EstadoSistema.getUsuarioActual().toLowerCase();
        boolean sinUsuario = nueva.length > 0 && !new String(nueva).toLowerCase().contains(usuario);
        boolean distinta = nueva.length > 0 && !Arrays.equals(actual, nueva);
        boolean coincide = nueva.length > 0 && Arrays.equals(nueva, confirmar);
        return new boolean[]{ nueva.length >= MIN_LARGO, mayus, minus, digito, sinUsuario, distinta, coincide };
    }

    private void actualizarRequisitos() {
        char[] a = txtActual.getPassword(), n = txtNueva.getPassword(), c = txtConfirmar.getPassword();
        boolean[] ok = evaluar(a, n, c);
        for (int i = 0; i < ok.length; i++) {
            lblReq[i].setForeground(ok[i] ? EstilosUI.SUCCESS_GREEN : EstilosUI.TEXT_GRAY);
        }
        // La fortaleza considera los 4 primeros criterios de composición + longitud ≥ 12 como bonus
        int puntos = 0;
        for (int i = 0; i < 4; i++) if (ok[i]) puntos++;
        if (n.length >= 12 && puntos == 4) puntos = 5;
        barra.setNivel(n.length == 0 ? 0 : puntos);
        if (n.length == 0)      { lblFuerza.setText("");       }
        else if (puntos <= 2)   { lblFuerza.setText("Débil");  lblFuerza.setForeground(EstilosUI.DANGER_RED); }
        else if (puntos <= 4)   { lblFuerza.setText("Media");  lblFuerza.setForeground(EstilosUI.WARNING_AMBER); }
        else                    { lblFuerza.setText("Fuerte"); lblFuerza.setForeground(EstilosUI.SUCCESS_GREEN); }
        Arrays.fill(a, '\0'); Arrays.fill(n, '\0'); Arrays.fill(c, '\0');
    }

    // ── Acciones ──────────────────────────────────────────────────
    private void accionGuardar() {
        String usuario = EstadoSistema.getUsuarioActual();
        lblError.setText("");

        if (EstadoSistema.estaBloqueado(usuario)) {
            mostrarError("Cambio bloqueado por intentos fallidos. Intenta de nuevo en "
                    + EstadoSistema.minutosBloqueoRestantes(usuario) + " min.");
            return;
        }

        char[] actual = txtActual.getPassword(), nueva = txtNueva.getPassword(), conf = txtConfirmar.getPassword();
        try {
            if (actual.length == 0 || nueva.length == 0 || conf.length == 0) {
                mostrarError("Completa los tres campos.");
                return;
            }
            if (!EstadoSistema.verificarPassword(usuario, actual)) {
                int quedan = EstadoSistema.registrarIntentoFallido(usuario);
                txtActual.setText("");
                txtActual.requestFocusInWindow();
                if (quedan == 0) {
                    mostrarError("Contraseña actual incorrecta. Cambio bloqueado " + EstadoSistema.MINUTOS_BLOQUEO + " minutos.");
                } else {
                    mostrarError("La contraseña actual es incorrecta. Intentos restantes: " + quedan + ".");
                }
                return;
            }
            boolean[] ok = evaluar(actual, nueva, conf);
            for (int i = 0; i < ok.length; i++) {
                if (!ok[i]) { mostrarError("No se cumple: " + REQUISITOS[i].toLowerCase() + "."); return; }
            }

            EstadoSistema.cambiarPassword(usuario, nueva);     // guarda hash + sal y audita CAMBIAR_PASSWORD
            limpiarCampos();
            EstilosUI.info(this, "Contraseña actualizada", "Tu contraseña se cambió correctamente.");
            if (alCambiar != null) alCambiar.run();
        } finally {
            Arrays.fill(actual, '\0'); Arrays.fill(nueva, '\0'); Arrays.fill(conf, '\0');
        }
    }

    private void mostrarError(String msg) {
        lblError.setText("<html>" + msg + "</html>");
    }

    private void limpiarCampos() {
        txtActual.setText("");
        txtNueva.setText("");
        txtConfirmar.setText("");
        lblError.setText("");
        actualizarRequisitos();
    }

    private void alternarVisibilidad() {
        char eco = chkMostrar.isSelected() ? (char) 0 : '\u25CF';
        txtActual.setEchoChar(eco);
        txtNueva.setEchoChar(eco);
        txtConfirmar.setEchoChar(eco);
    }

    /** Barra de fortaleza de 5 tramos. */
    private static class BarraFuerza extends JComponent {
        private int nivel;
        void setNivel(int n) { nivel = n; repaint(); }
        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int tramos = 5, sep = 4;
            int w = (getWidth() - sep * (tramos - 1)) / tramos;
            Color on = nivel <= 2 ? EstilosUI.DANGER_RED : (nivel <= 4 ? EstilosUI.WARNING_AMBER : EstilosUI.SUCCESS_GREEN);
            for (int i = 0; i < tramos; i++) {
                g2.setColor(i < nivel ? on : EstilosUI.BORDER_CLR);
                g2.fill(new RoundRectangle2D.Float(i * (w + sep), 0, w, getHeight(), 6, 6));
            }
            g2.dispose();
        }
    }
}
