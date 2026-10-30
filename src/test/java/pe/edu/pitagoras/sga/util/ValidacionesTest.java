package pe.edu.pitagoras.sga.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import pe.edu.pitagoras.sga.exception.BusinessException;

/** Validación de entradas y generación de claves temporales. */
class ValidacionesTest {

    @Test
    void dni_ochoDigitos() {
        assertEquals("70000001", Validaciones.dni(" 70000001 "));
        assertThrows(BusinessException.class, () -> Validaciones.dni("7000000"));
        assertThrows(BusinessException.class, () -> Validaciones.dni("7000000A"));
    }

    @Test
    void nombre_soloLetrasYSinEspaciosDeMas() {
        assertEquals("María José", Validaciones.nombre("  María   José ", "el nombre", 50));
        assertThrows(BusinessException.class, () -> Validaciones.nombre("Robert'); DROP TABLE--1", "el nombre", 50));
        assertThrows(BusinessException.class, () -> Validaciones.nombre("", "el nombre", 50));
    }

    @Test
    void usuario_minusculasYLargo() {
        assertEquals("docente11", Validaciones.usuario("Docente11"));
        assertThrows(BusinessException.class, () -> Validaciones.usuario("ab"));
        assertThrows(BusinessException.class, () -> Validaciones.usuario("con espacio"));
    }

    @Test
    void opcionales() {
        assertNull(Validaciones.emailOpcional(" "));
        assertNull(Validaciones.telefonoOpcional(null));
        assertThrows(BusinessException.class, () -> Validaciones.emailOpcional("sin-arroba"));
    }

    @RepeatedTest(20)
    void claveTemporal_tieneLetrasYNumeros() {
        String c = GeneradorClave.generar();
        assertEquals(10, c.length());
        assertTrue(c.chars().anyMatch(Character::isLetter));
        assertTrue(c.chars().anyMatch(Character::isDigit));
    }
}
