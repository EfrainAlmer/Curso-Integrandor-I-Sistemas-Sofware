package pe.edu.pitagoras.sga.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import pe.edu.pitagoras.sga.model.evaluacion.Calificativo;
import pe.edu.pitagoras.sga.model.evaluacion.EstadoAsistencia;
import pe.edu.pitagoras.sga.model.evaluacion.ReglaCalificativo;
import pe.edu.pitagoras.sga.model.evaluacion.ResumenAsistencia;

/** Reglas de calificación del MINEDU (RF13, RF14) y estados de asistencia (RF12). */
class ReglaCalificativoTest {

    @ParameterizedTest
    @ValueSource(strings = {"AD", "a", " b ", "C"})
    void escala_aceptaSoloADABC(String texto) {
        assertTrue(Calificativo.desdeTexto(texto).isPresent());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "D", "20", "AA", "E"})
    void escala_rechazaValoresFueraDeLaEscala(String texto) {
        assertTrue(Calificativo.desdeTexto(texto).isEmpty());
        assertTrue(Calificativo.desdeTexto(null).isEmpty());
    }

    @Test
    void area_esElPromedioRedondeadoDeLasCompetencias() {
        assertEquals(Calificativo.A, ReglaCalificativo.calificativoArea(
                List.of(Calificativo.A, Calificativo.A, Calificativo.B, Calificativo.AD)).orElseThrow());
        assertEquals(Calificativo.AD, ReglaCalificativo.calificativoArea(
                List.of(Calificativo.AD, Calificativo.AD, Calificativo.A)).orElseThrow());
    }

    @Test
    void area_conCYSumaBajaQuedaEnC() {
        // C(1) + B(2) + B(2) = 5 <= 7 → C
        assertEquals(Calificativo.C, ReglaCalificativo.calificativoArea(
                List.of(Calificativo.C, Calificativo.B, Calificativo.B)).orElseThrow());
    }

    @Test
    void area_sinNotasEstaVacia() {
        assertTrue(ReglaCalificativo.calificativoArea(List.of()).isEmpty());
    }

    @Test
    void anual_tomaElBimestre4SinPromediar_RF14() {
        Map<Integer, Calificativo> bimestres = Map.of(1, Calificativo.C, 2, Calificativo.C, 3, Calificativo.C,
                4, Calificativo.AD);
        assertEquals(Optional.of(Calificativo.AD), ReglaCalificativo.calificativoAnual(bimestres));
    }

    @Test
    void anual_pendienteSiNoHayBimestre4() {
        assertTrue(ReglaCalificativo.calificativoAnual(Map.of(1, Calificativo.A, 3, Calificativo.B)).isEmpty());
    }

    @Test
    void situacionFinal_segunAreasEnC() {
        Optional<Calificativo> a = Optional.of(Calificativo.A);
        Optional<Calificativo> c = Optional.of(Calificativo.C);
        assertEquals("Promovido de grado (PRO)", ReglaCalificativo.situacionFinal(List.of(a, a, a)));
        assertTrue(ReglaCalificativo.situacionFinal(List.of(a, c)).endsWith("(RR)"));
        assertEquals("Permanece en el grado (PER)", ReglaCalificativo.situacionFinal(List.of(c, c, c, c, a)));
        assertTrue(ReglaCalificativo.situacionFinal(List.of(a, Optional.empty())).startsWith("En proceso"));
    }

    @Test
    void asistencia_faltaJustificadaSeGuardaComoFConJustificacion() {
        assertEquals("F", EstadoAsistencia.FALTA_JUSTIFICADA.getCodigo());
        assertTrue(EstadoAsistencia.FALTA_JUSTIFICADA.isJustificada());
        assertEquals(EstadoAsistencia.FALTA_JUSTIFICADA, EstadoAsistencia.desde("F", true));
        assertEquals(EstadoAsistencia.TARDANZA, EstadoAsistencia.desde("T", false));
    }

    @Test
    void resumen_porcentajeCuentaTardanzasComoPresente() {
        ResumenAsistencia r = new ResumenAsistencia(8, 1, 1, 0);
        assertEquals(90.0, r.porcentajeAsistencia(), 0.001);
        assertEquals(100.0, ResumenAsistencia.VACIO.porcentajeAsistencia(), 0.001);
    }
}
