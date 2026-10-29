package pe.edu.utp.pitagoras.view.docente;

import pe.edu.utp.pitagoras.config.EstilosUI;
import pe.edu.utp.pitagoras.controller.DocenteController;
import pe.edu.utp.pitagoras.model.CargaAcademica;
import pe.edu.utp.pitagoras.model.Estudiante;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

/**
 * Vista del Módulo Academic para el Docente (RF11 / CU11 - págs. 26 y 31 del PDF):
 * "El sistema deberá permitir al docente consultar su carga académica y la lista de
 * estudiantes de cada sección asignada."
 *
 * Muestra dinámicamente los cursos del docente autenticado (ej. Matemática/CyT para 'docente'
 * o Comunicación/Ciencias Sociales para 'docente2') incluyendo el grado 5.° A.
 */
public class PnlMiCargaDocente extends JPanel {
    private final DocenteController controller = DocenteController.getInstancia();
    private JTable tablaCargas;
    private DefaultTableModel modeloEstudiantes;
    private JLabel lblDetalleSeccion;

    public PnlMiCargaDocente() {
        setLayout(new BorderLayout(0, 16));
        setBackground(EstilosUI.FONDO_GRIS);
        setBorder(new EmptyBorder(26, 34, 26, 34));
        inicializarUI();
    }

    private void inicializarUI() {
        JPanel norte = new JPanel();
        norte.setBackground(EstilosUI.FONDO_GRIS);
        norte.setLayout(new BoxLayout(norte, BoxLayout.Y_AXIS));

        JLabel lblTitulo = new JLabel("Mi carga académica — " + controller.getNombreDocenteActivo());
        lblTitulo.setFont(new Font("SansSerif", Font.BOLD, 24));
        lblTitulo.setForeground(EstilosUI.TEXTO_OSCURO);
        lblTitulo.setAlignmentX(Component.LEFT_ALIGNMENT);

        String cursosResumen = String.join(" y ", controller.obtenerAreasDocenteActivo());
        JLabel lblSub = new JLabel("Áreas asignadas: " + cursosResumen + " · Grados 1.° A a 5.° A (RF11 / CU11)");
        lblSub.setFont(new Font("SansSerif", Font.PLAIN, 13));
        lblSub.setForeground(EstilosUI.TEXTO_SECUNDARIO);
        lblSub.setAlignmentX(Component.LEFT_ALIGNMENT);

        norte.add(lblTitulo);
        norte.add(Box.createVerticalStrut(4));
        norte.add(lblSub);
        add(norte, BorderLayout.NORTH);

        JPanel centro = new JPanel(new GridLayout(2, 1, 0, 16));
        centro.setBackground(EstilosUI.FONDO_GRIS);

        // 1. Panel Superior: Secciones y Áreas Asignadas al Docente Activo
        JPanel cardCargas = new JPanel(new BorderLayout(0, 10));
        cardCargas.setBackground(Color.WHITE);
        cardCargas.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(EstilosUI.BORDE_GRIS, 1, true),
                new EmptyBorder(14, 16, 14, 16)
        ));

        JLabel lblSubCarga = new JLabel("1. Secciones y áreas asignadas — Año Escolar 2026 (Seleccione una fila para ver sus alumnos)");
        lblSubCarga.setFont(new Font("SansSerif", Font.BOLD, 14));
        lblSubCarga.setForeground(EstilosUI.TEXTO_OSCURO);
        cardCargas.add(lblSubCarga, BorderLayout.NORTH);

        String[] colsCarga = {"Código", "Grado y Sección", "Turno", "Área Curricular", "Competencias CNEB (SIAGIE)", "Estado Bimestre 2"};
        DefaultTableModel modeloCargas = new DefaultTableModel(colsCarga, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        List<CargaAcademica> cargas = controller.obtenerCargaDocente();
        for (CargaAcademica c : cargas) {
            modeloCargas.addRow(new Object[]{
                    "   " + c.getCodigoCarga(),
                    c.getGradoSeccionCorto() + " - Secundaria",
                    c.getTurno(),
                    c.getAreaCurricular(),
                    c.getCompetencias().length + " competencias oficiales",
                    controller.esPeriodoAbierto("Bimestre 2") ? "Abierto (En curso)" : "Cerrado"
            });
        }

        tablaCargas = new JTable(modeloCargas);
        EstilosUI.estilizarTabla(tablaCargas);
        tablaCargas.setRowHeight(40);
        tablaCargas.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JScrollPane scrollCargas = new JScrollPane(tablaCargas);
        scrollCargas.setBorder(new LineBorder(EstilosUI.BORDE_GRIS, 1));
        scrollCargas.getViewport().setBackground(Color.WHITE);
        cardCargas.add(scrollCargas, BorderLayout.CENTER);

        // 2. Panel Inferior: Lista de Estudiantes de la Sección Seleccionada (RF11 / CU11)
        JPanel cardAlumnos = new JPanel(new BorderLayout(0, 10));
        cardAlumnos.setBackground(Color.WHITE);
        cardAlumnos.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(EstilosUI.BORDE_GRIS, 1, true),
                new EmptyBorder(14, 16, 14, 16)
        ));

        lblDetalleSeccion = new JLabel("2. Lista de estudiantes matriculados");
        lblDetalleSeccion.setFont(new Font("SansSerif", Font.BOLD, 14));
        lblDetalleSeccion.setForeground(EstilosUI.TEXTO_OSCURO);
        cardAlumnos.add(lblDetalleSeccion, BorderLayout.NORTH);

        String[] colsAlumnos = {"Código", "DNI", "Estudiante", "Grado y Sección", "Apoderado Vinculado", "Estado Matrícula"};
        modeloEstudiantes = new DefaultTableModel(colsAlumnos, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        JTable tablaAlumnos = new JTable(modeloEstudiantes);
        EstilosUI.estilizarTabla(tablaAlumnos);
        tablaAlumnos.setRowHeight(40);

        JScrollPane scrollAlumnos = new JScrollPane(tablaAlumnos);
        scrollAlumnos.setBorder(new LineBorder(EstilosUI.BORDE_GRIS, 1));
        scrollAlumnos.getViewport().setBackground(Color.WHITE);
        cardAlumnos.add(scrollAlumnos, BorderLayout.CENTER);

        tablaCargas.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int fila = tablaCargas.getSelectedRow();
                if (fila >= 0 && fila < cargas.size()) {
                    CargaAcademica seleccionada = cargas.get(fila);
                    cargarEstudiantesDeSeccion(seleccionada.getGradoSeccionCorto(), seleccionada.getAreaCurricular());
                }
            }
        });

        if (cargas.size() > 2) {
            tablaCargas.setRowSelectionInterval(2, 2);
            cargarEstudiantesDeSeccion(cargas.get(2).getGradoSeccionCorto(), cargas.get(2).getAreaCurricular());
        } else if (!cargas.isEmpty()) {
            tablaCargas.setRowSelectionInterval(0, 0);
            cargarEstudiantesDeSeccion(cargas.get(0).getGradoSeccionCorto(), cargas.get(0).getAreaCurricular());
        }

        centro.add(cardCargas);
        centro.add(cardAlumnos);
        add(centro, BorderLayout.CENTER);
    }

    private void cargarEstudiantesDeSeccion(String seccion, String area) {
        lblDetalleSeccion.setText("2. Lista de estudiantes matriculados — Sección " + seccion + " · Área: " + area);
        modeloEstudiantes.setRowCount(0);
        List<Estudiante> alumnos = controller.obtenerEstudiantesPorSeccion(seccion);
        for (Estudiante e : alumnos) {
            modeloEstudiantes.addRow(new Object[]{
                    "   " + e.getCodigoModular(),
                    e.getDni(),
                    e.getNombresApellidos(),
                    e.getGradoSeccion(),
                    e.getApoderadoNombre() + " (" + e.getTelefonoApoderado() + ")",
                    e.getEstadoMatricula()
            });
        }
    }
}
