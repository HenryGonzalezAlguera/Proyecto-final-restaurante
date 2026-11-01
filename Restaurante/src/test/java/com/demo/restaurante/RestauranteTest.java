package com.demo.restaurante;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class RestauranteTest {

    @Test
    void muestraPlatillosDiferentesYOrdenesEnEspera() {
        PrintStream consolaOriginal = System.out;
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        try (PrintStream consolaPrueba = new PrintStream(buffer, true, StandardCharsets.UTF_8)) {
            System.setOut(consolaPrueba);
            Restaurante.main(new String[0]);
        } finally {
            System.setOut(consolaOriginal);
        }

        String salida = buffer.toString(StandardCharsets.UTF_8);

        assertTrue(salida.contains("Nombre: Arroz con Pollo"));
        assertTrue(salida.contains("Nombre: La Carnita Roja"));
        assertEquals(1, contar(salida, "Nombre: Arroz con Pollo"));
        assertEquals(1, contar(salida, "Nombre: La Carnita Roja"));
        assertEquals(3, contar(salida, "Cliente Id: "));
    }

    private static int contar(String texto, String fragmento) {
        return (texto.length() - texto.replace(fragmento, "").length()) / fragmento.length();
    }
}
