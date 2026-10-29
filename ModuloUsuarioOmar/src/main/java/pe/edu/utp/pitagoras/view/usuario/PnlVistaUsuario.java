package pe.edu.utp.pitagoras.view.usuario;

import pe.edu.utp.pitagoras.dao.UsuarioDAO;
import pe.edu.utp.pitagoras.model.Usuario;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

public class PnlVistaUsuario extends JPanel {
    private JTable tablaUsuarios;
    private DefaultTableModel modeloTabla;
    private JTextField txtNombre, txtApellido, txtCorreo;
    private JComboBox<String> cbTipo, cbEstado;
    private JButton btnGuardar, btnActualizar, btnEliminar, btnLimpiar;
    private UsuarioDAO usuarioDAO;
    private int idUsuarioSeleccionado = -1;

    public PnlVistaUsuario() {
        usuarioDAO = new UsuarioDAO();
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // Título
        JLabel lblTitulo = new JLabel("MÓDULO DE GESTIÓN DE USUARIOS - OMAR BAVIN", JLabel.CENTER);
        lblTitulo.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lblTitulo.setForeground(new Color(41, 128, 185));
        add(lblTitulo, BorderLayout.NORTH);

        // Panel Formulario (Izquierda)
        JPanel panelForm = new JPanel(new GridBagLayout());
        panelForm.setBorder(BorderFactory.createTitledBorder("Datos del Usuario"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0; gbc.gridy = 0;
        panelForm.add(new JLabel("Nombre:"), gbc);
        gbc.gridx = 1; 
        txtNombre = new JTextField(15);
        panelForm.add(txtNombre, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        panelForm.add(new JLabel("Apellido:"), gbc);
        gbc.gridx = 1; 
        txtApellido = new JTextField(15);
        panelForm.add(txtApellido, gbc);

        gbc.gridx = 0; gbc.gridy = 2;
        panelForm.add(new JLabel("Correo:"), gbc);
        gbc.gridx = 1; 
        txtCorreo = new JTextField(15);
        panelForm.add(txtCorreo, gbc);

        gbc.gridx = 0; gbc.gridy = 3;
        panelForm.add(new JLabel("Tipo:"), gbc);
        gbc.gridx = 1;
        cbTipo = new JComboBox<>(new String[]{"Estudiante", "Docente", "Administrador"});
        panelForm.add(cbTipo, gbc);

        gbc.gridx = 0; gbc.gridy = 4;
        panelForm.add(new JLabel("Estado:"), gbc);
        gbc.gridx = 1;
        cbEstado = new JComboBox<>(new String[]{"Activo", "Inactivo"});
        panelForm.add(cbEstado, gbc);

        // Botones CRUD
        JPanel panelBotones = new JPanel(new GridLayout(2, 2, 5, 5));
        btnGuardar = new JButton("Guardar");
        btnActualizar = new JButton("Actualizar");
        btnEliminar = new JButton("Eliminar");
        btnLimpiar = new JButton("Limpiar");

        styleButton(btnGuardar, new Color(39, 174, 96));
        styleButton(btnActualizar, new Color(41, 128, 185));
        styleButton(btnEliminar, new Color(192, 57, 43));
        styleButton(btnLimpiar, new Color(127, 140, 141));

        panelBotones.add(btnGuardar);
        panelBotones.add(btnActualizar);
        panelBotones.add(btnEliminar);
        panelBotones.add(btnLimpiar);

        gbc.gridx = 0; gbc.gridy = 5; gbc.gridwidth = 2;
        panelForm.add(panelBotones, gbc);

        add(panelForm, BorderLayout.WEST);

        // Panel Tabla (Centro)
        String[] columnas = {"ID", "Nombre", "Apellido", "Correo", "Tipo", "Estado"};
        modeloTabla = new DefaultTableModel(columnas, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        tablaUsuarios = new JTable(modeloTabla);
        JScrollPane scrollPane = new JScrollPane(tablaUsuarios);
        scrollPane.setBorder(BorderFactory.createTitledBorder("Lista de Usuarios Registrados"));
        add(scrollPane, BorderLayout.CENTER);

        // Eventos
        btnGuardar.addActionListener(e -> guardarUsuario());
        btnActualizar.addActionListener(e -> actualizarUsuario());
        btnEliminar.addActionListener(e -> eliminarUsuario());
        btnLimpiar.addActionListener(e -> limpiarFormulario());

        tablaUsuarios.getSelectionModel().addListSelectionListener(e -> {
            int fila = tablaUsuarios.getSelectedRow();
            if (fila >= 0) {
                idUsuarioSeleccionado = (int) modeloTabla.getValueAt(fila, 0);
                txtNombre.setText(modeloTabla.getValueAt(fila, 1).toString());
                txtApellido.setText(modeloTabla.getValueAt(fila, 2).toString());
                txtCorreo.setText(modeloTabla.getValueAt(fila, 3).toString());
                cbTipo.setSelectedItem(modeloTabla.getValueAt(fila, 4).toString());
                cbEstado.setSelectedItem(modeloTabla.getValueAt(fila, 5).toString());
            }
        });

        cargarDatosTabla();
    }

    private void styleButton(JButton btn, Color color) {
        btn.setBackground(color);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 12));
    }

    public void cargarDatosTabla() {
        modeloTabla.setRowCount(0);
        List<Usuario> lista = usuarioDAO.listarUsuarios();
        for (Usuario u : lista) {
            modeloTabla.addRow(new Object[]{
                u.getId(), u.getNombre(), u.getApellido(), u.getCorreo(), u.getTipoUsuario(), u.getEstado()
            });
        }
    }

    private void guardarUsuario() {
        if (txtNombre.getText().isEmpty() || txtApellido.getText().isEmpty() || txtCorreo.getText().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Complete todos los campos obligatorios.", "Advertencia", JOptionPane.WARNING_MESSAGE);
            return;
        }
        Usuario u = new Usuario();
        u.setNombre(txtNombre.getText());
        u.setApellido(txtApellido.getText());
        u.setCorreo(txtCorreo.getText());
        u.setTipoUsuario(cbTipo.getSelectedItem().toString());
        u.setEstado(cbEstado.getSelectedItem().toString());

        if (usuarioDAO.insertarUsuario(u)) {
            JOptionPane.showMessageDialog(this, "Usuario registrado exitosamente.");
            cargarDatosTabla();
            limpiarFormulario();
        } else {
            JOptionPane.showMessageDialog(this, "Error al registrar usuario.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void actualizarUsuario() {
        if (idUsuarioSeleccionado == -1) {
            JOptionPane.showMessageDialog(this, "Seleccione un usuario de la tabla para actualizar.", "Advertencia", JOptionPane.WARNING_MESSAGE);
            return;
        }
        Usuario u = new Usuario();
        u.setId(idUsuarioSeleccionado);
        u.setNombre(txtNombre.getText());
        u.setApellido(txtApellido.getText());
        u.setCorreo(txtCorreo.getText());
        u.setTipoUsuario(cbTipo.getSelectedItem().toString());
        u.setEstado(cbEstado.getSelectedItem().toString());

        if (usuarioDAO.actualizarUsuario(u)) {
            JOptionPane.showMessageDialog(this, "Usuario actualizado exitosamente.");
            cargarDatosTabla();
            limpiarFormulario();
        } else {
            JOptionPane.showMessageDialog(this, "Error al actualizar usuario.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void eliminarUsuario() {
        if (idUsuarioSeleccionado == -1) {
            JOptionPane.showMessageDialog(this, "Seleccione un usuario de la tabla para eliminar.", "Advertencia", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int opcion = JOptionPane.showConfirmDialog(this, "¿Está seguro de eliminar este usuario?", "Confirmar Eliminación", JOptionPane.YES_NO_OPTION);
        if (opcion == JOptionPane.YES_OPTION) {
            if (usuarioDAO.eliminarUsuario(idUsuarioSeleccionado)) {
                JOptionPane.showMessageDialog(this, "Usuario eliminado.");
                cargarDatosTabla();
                limpiarFormulario();
            } else {
                JOptionPane.showMessageDialog(this, "Error al eliminar usuario.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void limpiarFormulario() {
        txtNombre.setText("");
        txtApellido.setText("");
        txtCorreo.setText("");
        cbTipo.setSelectedIndex(0);
        cbEstado.setSelectedIndex(0);
        idUsuarioSeleccionado = -1;
        tablaUsuarios.clearSelection();
    }
}
