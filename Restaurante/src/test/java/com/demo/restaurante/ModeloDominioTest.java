package com.demo.restaurante;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

class ModeloDominioTest {

    private Platillo platillo() {
        return new Platillo(1, "Casado", new BigDecimal("2000"), new BigDecimal("3500"));
    }

    @Test
    void creaPlatilloConMontosPrecisosYMargen() {
        Platillo platillo = platillo();

        assertEquals(new BigDecimal("3500.00"), platillo.getPrecio());
        assertEquals(new BigDecimal("2000.00"), platillo.getCostoIngredientes());
        assertEquals(new BigDecimal("1500.00"), platillo.getMargenBrutoUnitario());
    }

    @Test
    void rechazaPlatillosInvalidos() {
        assertThrows(IllegalArgumentException.class,
                () -> new Platillo(0, "Casado", new BigDecimal("1"), new BigDecimal("2")));
        assertThrows(IllegalArgumentException.class,
                () -> new Platillo(1, " ", new BigDecimal("1"), new BigDecimal("2")));
        assertThrows(IllegalArgumentException.class,
                () -> new Platillo(1, "Casado", new BigDecimal("-1"), new BigDecimal("2")));
        assertThrows(IllegalArgumentException.class,
                () -> new Platillo(1, "Casado", new BigDecimal("1"), BigDecimal.ZERO));
        assertThrows(IllegalArgumentException.class,
                () -> new Platillo(1, "Casado", new BigDecimal("1"), new BigDecimal("2.001")));
    }

    @Test
    void calculaSubtotalesYTotalSinUsarDouble() {
        Platillo platillo = platillo();
        DetallePedido detalle = new DetallePedido(platillo, 3);
        Pedido pedido = new Pedido(20, 8, List.of(detalle));

        assertEquals(new BigDecimal("10500.00"), detalle.getSubtotal());
        assertEquals(new BigDecimal("10500.00"), pedido.getTotal());
        assertEquals(EstadoPedido.PENDIENTE, pedido.getEstado());
        assertEquals("Casado", detalle.getNombrePlatillo());
    }

    @Test
    void rechazaCantidadesYPedidosInvalidos() {
        assertThrows(IllegalArgumentException.class, () -> new DetallePedido(platillo(), 0));
        assertThrows(IllegalArgumentException.class, () -> new DetallePedido(null, 1));
        assertThrows(IllegalArgumentException.class, () -> new Pedido(1, 1, List.of()));
        assertThrows(IllegalArgumentException.class,
                () -> new Pedido(1, 1, Arrays.asList((DetallePedido) null)));
        assertThrows(IllegalArgumentException.class,
                () -> new Pedido(0, 1, List.of(new DetallePedido(platillo(), 1))));
        assertThrows(IllegalArgumentException.class,
                () -> new Pedido(1, 0, List.of(new DetallePedido(platillo(), 1))));
    }

    @Test
    void pedidoHaceCopiaDefensivaDeDetalles() {
        List<DetallePedido> detalles = new ArrayList<>();
        detalles.add(new DetallePedido(platillo(), 2));
        Pedido pedido = new Pedido(1, 2, detalles);

        detalles.clear();

        assertEquals(1, pedido.getDetalles().size());
        assertEquals(new BigDecimal("7000.00"), pedido.getTotal());
        assertThrows(UnsupportedOperationException.class,
                () -> pedido.getDetalles().clear());
    }

    @Test
    void empleadoExigeDatosCompletos() {
        assertThrows(IllegalArgumentException.class, () -> new Empleado(0, "Ana", "Cajera"));
        assertThrows(IllegalArgumentException.class, () -> new Empleado(1, " ", "Cajera"));
        assertThrows(IllegalArgumentException.class, () -> new Empleado(1, "Ana", " "));
        assertEquals("Cocinera", new Empleado(1, " Ana ", " Cocinera ").getPuesto());
    }
}
