package com.demo.restaurante;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class RestauranteTest {

    @Test
    void muestraMenuYPedidosConEstadosYTotales() {
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
        assertEquals(3, contar(salida, "Pedido #"));
        assertEquals(1, contar(salida, "Estado: ENTREGADO"));
        assertEquals(1, contar(salida, "Estado: PENDIENTE"));
        assertEquals(1, contar(salida, "Estado: CANCELADO"));
        assertTrue(salida.contains("Pedido #1 | Cliente: 1 | Estado: ENTREGADO | Total CRC: 8000.00"));
        assertTrue(salida.contains("Pedido #2 | Cliente: 2 | Estado: PENDIENTE | Total CRC: 10000.00"));
        assertTrue(salida.contains("Pedido #3 | Cliente: 3 | Estado: CANCELADO | Total CRC: 12000.00"));
        assertTrue(salida.contains("Pendientes en cola: 1"));
    }

    private static int contar(String texto, String fragmento) {
        return (texto.length() - texto.replace(fragmento, "").length()) / fragmento.length();
    }
}
