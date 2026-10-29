package pe.edu.utp.pitagoras;

import pe.edu.utp.pitagoras.controller.AuthController;
import pe.edu.utp.pitagoras.controller.DocenteController;
import pe.edu.utp.pitagoras.model.CalificacionCompetencia;
import pe.edu.utp.pitagoras.model.Estudiante;
import pe.edu.utp.pitagoras.model.RegistroAsistencia;
import pe.edu.utp.pitagoras.model.ReglaCalificativo;
import pe.edu.utp.pitagoras.util.SeguridadUtil;

import java.util.Arrays;
import java.util.List;

/**
 * Suite de Pruebas Unitarias y de Integración bajo metodología <b>TDD (Test-Driven Development)</b>
 * exigida en la Consigna del Avance de Proyecto Final 3 (Semana 12 - UTP).
 *
 * @author Angelo Giuseppe Paredes Quispe
 * @version 2.0
 */
public class DocenteTDDTest {

    public static void main(String[] args) {
        System.out.println("=================================================================");
        System.out.println(" EJECUTANDO SUITE DE PRUEBAS UNITARIAS TDD - COLEGIO PITÁGORAS");
        System.out.println("=================================================================");

        probarEscalaLiteralMINEDU_RF13();
        probarCalificativoAnualUltimoBimestre_RF14();
        probarReglaFaltaJustificadaAsistencia_RF12();
        probarSegundoDocenteYGrado5A_RF11();
        probarAuditoriaAlModificarNotas_RF09();
        probarBloqueoTresIntentosYHashSeguridad_RF02();

        System.out.println("=================================================================");
        System.out.println(" RESULTADO TDD: 6/6 PRUEBAS UNITARIAS APROBADAS EXITOSAMENTE");
        System.out.println("=================================================================");
    }

    public static void probarEscalaLiteralMINEDU_RF13() {
        asegurar(ReglaCalificativo.esCalificativoValido("AD"), "AD debe ser válido");
        asegurar(ReglaCalificativo.esCalificativoValido("A"), "A debe ser válido");
        asegurar(ReglaCalificativo.esCalificativoValido("B"), "B debe ser válido");
        asegurar(ReglaCalificativo.esCalificativoValido("C"), "C debe ser válido");
        asegurar(!ReglaCalificativo.esCalificativoValido("20"), "Escala vigesimal (20) no debe aceptarse en secundaria CNEB");
        System.out.println("[OK] TDD-01: Validación de escala literal MINEDU (AD, A, B, C) - RF13");
    }

    public static void probarCalificativoAnualUltimoBimestre_RF14() {
        String pendiente = ReglaCalificativo.determinarCalificativoAnual("A", false);
        asegurar(pendiente.startsWith("Pendiente"), "Antes del 4.° bimestre debe figurar Pendiente");

        String notaAnual = ReglaCalificativo.determinarCalificativoAnual("AD", true);
        asegurar("AD".equals(notaAnual), "Al cierre del 4.° bimestre debe fijar AD como calificativo anual");
        System.out.println("[OK] TDD-02: Determinación automática de calificativo anual (Último Bimestre) - RF14");
    }

    public static void probarReglaFaltaJustificadaAsistencia_RF12() {
        RegistroAsistencia reg = new RegistroAsistencia(1, "Estudiante 01", "5.° A", "29/10/2026", "Asistió", false, "");
        reg.setJustificada(true);
        asegurar("Falta".equals(reg.getEstado()) && reg.isJustificada(),
                "Al justificar asistencia, el estado debe pasar y conservarse como 'Falta'");

        reg.setEstado("Tardanza");
        asegurar(!reg.isJustificada(), "Al cambiar a Tardanza, debe desmarcarse la casilla de falta justificada");
        System.out.println("[OK] TDD-03: Regla de negocio de Falta Justificada y Asistencia en Lote - RF12");
    }

    public static void probarSegundoDocenteYGrado5A_RF11() {
        AuthController auth = new AuthController();
        AuthController.ResultadoLogin loginDocente2 = auth.autenticar("docente2", "123456");
        asegurar(loginDocente2.exito(), "El usuario 'docente2' debe autenticarse correctamente");

        DocenteController controller = DocenteController.getInstancia();
        String[] areasDoc2 = controller.obtenerAreasDocenteActivo();
        asegurar(areasDoc2.length == 2
                        && Arrays.asList(areasDoc2).contains("Comunicación")
                        && Arrays.asList(areasDoc2).contains("Ciencias Sociales"),
                "El docente2 solo debe enseñar 2 cursos diferentes: Comunicación y Ciencias Sociales");

        List<Estudiante> alumnos5A = controller.obtenerEstudiantesPorSeccion("5.° A");
        asegurar(!alumnos5A.isEmpty(), "La sección 5.° A debe contar con estudiantes matriculados");

        // Restaurar docente principal por defecto
        auth.autenticar("docente", "123456");
        System.out.println("[OK] TDD-04: Segregación de cursos para 'docente2' (Comunicación y CC.SS.) y sección '5.° A' - RF11");
    }

    public static void probarAuditoriaAlModificarNotas_RF09() {
        DocenteController controller = DocenteController.getInstancia();
        List<CalificacionCompetencia> notas = controller.obtenerCalificaciones("Bimestre 2", "5.° A", "Matemática");
        notas.get(0).setCalificativo(0, "AD");
        int auditados = controller.guardarCalificaciones("Bimestre 2", "5.° A", "Matemática", notas);
        asegurar(auditados >= 1, "Debe registrarse al menos 1 evento en la bitácora de auditoría");
        System.out.println("[OK] TDD-05: Trazabilidad en Bitácora de Auditoría al modificar calificativos en 5.° A - RF09");
    }

    public static void probarBloqueoTresIntentosYHashSeguridad_RF02() {
        String hash = SeguridadUtil.generarHashConSal("123456");
        asegurar(hash.startsWith("$2a$10$"), "El hash debe incluir prefijo criptográfico y sal");
        asegurar(SeguridadUtil.verificarPassword("123456", hash), "La verificación de contraseña con sal debe coincidir");

        AuthController auth = new AuthController();
        auth.autenticar("docente", "claveErronea1");
        auth.autenticar("docente", "claveErronea2");
        AuthController.ResultadoLogin intento3 = auth.autenticar("docente", "claveErronea3");
        asegurar(!intento3.exito() && intento3.mensaje().contains("bloqueada"),
                "Tras 3 intentos fallidos la cuenta debe quedar bloqueada temporalmente (RF02)");
        System.out.println("[OK] TDD-06: Seguridad criptográfica con sal y bloqueo al 3.° intento fallido - RF02 / RNF02");
    }

    private static void asegurar(boolean condicion, String mensajeError) {
        if (!condicion) {
            throw new AssertionError("Fallo en prueba TDD: " + mensajeError);
        }
    }
}
