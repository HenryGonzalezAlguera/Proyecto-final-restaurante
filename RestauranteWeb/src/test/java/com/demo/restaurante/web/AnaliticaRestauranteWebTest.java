package com.demo.restaurante.web;

import static org.junit.jupiter.api.Assertions.*;
import com.demo.restaurante.DetallePedido;
import com.demo.restaurante.EstadoPedido;
import com.demo.restaurante.Pedido;
import com.demo.restaurante.Platillo;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class AnaliticaRestauranteWebTest {
    private final AnaliticaRestauranteWeb analitica = new AnaliticaRestauranteWeb();

    private Pedido pedido(int id, int cliente, int cantidad) {
        Platillo plato = new Platillo(1, "Casado",
                new BigDecimal("2000.00"), new BigDecimal("3500.00"));
        return new Pedido(id, cliente, List.of(new DetallePedido(plato, cantidad)));
    }

    @Test
    void resumeImportesSoloDeEntregadosYExcluyeCanceladosEnRanking() {
        Pedido entregado = pedido(1, 101, 2)
                .cambiarEstado(EstadoPedido.EN_PREPARACION)
                .cambiarEstado(EstadoPedido.LISTO)
                .cambiarEstado(EstadoPedido.ENTREGADO);
        Pedido pendiente = pedido(2, 102, 1);
        Pedido cancelado = pedido(3, 103, 10).cambiarEstado(EstadoPedido.CANCELADO);
        var r = analitica.resumir(List.of(entregado, pendiente, cancelado));

        assertEquals(3, r.total());
        assertEquals(1, r.entregados());
        assertEquals(1, r.pendientes());
        assertEquals(1, r.cancelados());
        assertEquals(new BigDecimal("7000.00"), r.valorEntregado());
        assertEquals(1, r.destacados().size());
        assertEquals(3, r.destacados().get(0).unidades());
    }

    @Test
    void filtraPedidosPorEstadoEIdDeClienteEnOrdenDescendente() {
        Pedido a = pedido(3, 99, 1);
        Pedido b = pedido(9, 101, 1).cambiarEstado(EstadoPedido.CANCELADO);
        Pedido c = pedido(5, 101, 1);
        assertEquals(List.of(5), analitica.filtrar(List.of(a, b, c), "PENDIENTE", "101")
                .stream().map(Pedido::getId).toList());
        assertEquals(List.of(9, 5, 3), analitica.filtrar(List.of(a, b, c), "", "")
                .stream().map(Pedido::getId).toList());
        assertThrows(IllegalArgumentException.class,
                () -> analitica.filtrar(List.of(a), "desconocido", ""));
    }

    @Test
    void sinPedidosNoProduceErroresNiMontosInventados() {
        var r = analitica.resumir(List.of());
        assertEquals(0, r.total());
        assertEquals(new BigDecimal("0.00"), r.valorEntregado());
        assertTrue(r.destacados().isEmpty());
    }
}
