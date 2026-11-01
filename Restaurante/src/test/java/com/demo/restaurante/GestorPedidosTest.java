package com.demo.restaurante;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import org.junit.jupiter.api.Test;

class GestorPedidosTest {

    private DetallePedido detalle() {
        Platillo casado = new Platillo(
                1, "Casado", new BigDecimal("2000.00"), new BigDecimal("3500.00"));
        return new DetallePedido(casado, 2);
    }

    @Test
    void registraPedidosConIdConsecutivoYLosListaEnOrden() {
        GestorPedidos gestor = new GestorPedidos();
        Pedido primero = gestor.registrarPedido(10, List.of(detalle()));
        Pedido segundo = gestor.registrarPedido(20, List.of(detalle()));

        assertEquals(1, primero.getId());
        assertEquals(2, segundo.getId());
        assertEquals(List.of(primero, segundo), gestor.listarPedidos());
        assertEquals(List.of(primero, segundo), gestor.listarPendientes());
        assertEquals(new BigDecimal("7000.00"), primero.getTotal());
        assertEquals(EstadoPedido.PENDIENTE, primero.getEstado());
        assertThrows(UnsupportedOperationException.class, () -> gestor.listarPedidos().clear());
        assertThrows(UnsupportedOperationException.class, () -> gestor.listarPendientes().clear());
    }

    @Test
    void iniciaPedidosEnOrdenDeLlegadaYSaltandoCancelados() {
        GestorPedidos gestor = new GestorPedidos();
        Pedido primero = gestor.registrarPedido(1, List.of(detalle()));
        Pedido segundo = gestor.registrarPedido(2, List.of(detalle()));
        Pedido tercero = gestor.registrarPedido(3, List.of(detalle()));

        gestor.cancelarPedido(segundo.getId());

        assertEquals(List.of(1, 3),
                gestor.listarPendientes().stream().map(Pedido::getId).toList());

        assertEquals(primero.getId(), gestor.iniciarSiguientePendiente().orElseThrow().getId());
        assertEquals(tercero.getId(), gestor.iniciarSiguientePendiente().orElseThrow().getId());
        assertTrue(gestor.iniciarSiguientePendiente().isEmpty());
        assertTrue(gestor.listarPendientes().isEmpty());
        assertEquals(EstadoPedido.CANCELADO, gestor.buscarPedido(segundo.getId()).getEstado());
    }

    @Test
    void permiteCicloNormalYConservaDetallesYTotal() {
        GestorPedidos gestor = new GestorPedidos();
        Pedido original = gestor.registrarPedido(20, List.of(detalle()));
        Pedido preparando = gestor.cambiarEstado(original.getId(), EstadoPedido.EN_PREPARACION);
        Pedido listo = gestor.cambiarEstado(original.getId(), EstadoPedido.LISTO);
        Pedido entregado = gestor.cambiarEstado(original.getId(), EstadoPedido.ENTREGADO);

        assertEquals(EstadoPedido.PENDIENTE, original.getEstado());
        assertEquals(EstadoPedido.EN_PREPARACION, preparando.getEstado());
        assertEquals(EstadoPedido.LISTO, listo.getEstado());
        assertEquals(EstadoPedido.ENTREGADO, entregado.getEstado());
        assertNotSame(original, preparando);
        assertNotSame(preparando, listo);
        assertEquals(new BigDecimal("7000.00"), entregado.getTotal());
        assertEquals(original.getDetalles(), entregado.getDetalles());
        assertEquals(EstadoPedido.ENTREGADO, gestor.buscarPedido(original.getId()).getEstado());
        assertTrue(gestor.listarPendientes().isEmpty());
    }

    @Test
    void rechazaSaltosDeEstadoYCambiosEnPedidosTerminados() {
        GestorPedidos gestor = new GestorPedidos();
        Pedido pedido = gestor.registrarPedido(10, List.of(detalle()));

        assertThrows(IllegalArgumentException.class,
                () -> gestor.cambiarEstado(pedido.getId(), null));
        assertThrows(IllegalStateException.class,
                () -> gestor.cambiarEstado(pedido.getId(), EstadoPedido.ENTREGADO));
        assertThrows(IllegalStateException.class,
                () -> gestor.cambiarEstado(pedido.getId(), EstadoPedido.PENDIENTE));

        assertEquals(EstadoPedido.PENDIENTE, gestor.buscarPedido(pedido.getId()).getEstado());
        assertEquals(1, gestor.listarPendientes().size());

        gestor.cambiarEstado(pedido.getId(), EstadoPedido.EN_PREPARACION);
        assertThrows(IllegalStateException.class, () -> gestor.cancelarPedido(pedido.getId()));
        assertThrows(IllegalStateException.class,
                () -> gestor.cambiarEstado(pedido.getId(), EstadoPedido.ENTREGADO));

        gestor.cambiarEstado(pedido.getId(), EstadoPedido.LISTO);
        gestor.cambiarEstado(pedido.getId(), EstadoPedido.ENTREGADO);
        assertThrows(IllegalStateException.class, () -> gestor.cancelarPedido(pedido.getId()));
        assertThrows(IllegalStateException.class,
                () -> gestor.cambiarEstado(pedido.getId(), EstadoPedido.ENTREGADO));
        assertEquals(EstadoPedido.ENTREGADO, gestor.buscarPedido(pedido.getId()).getEstado());
    }

    @Test
    void cancelacionSoloPermitePedidosPendientesYNoAfectaLaColaRestante() {
        GestorPedidos gestor = new GestorPedidos();
        Pedido pedido = gestor.registrarPedido(10, List.of(detalle()));
        Pedido siguiente = gestor.registrarPedido(11, List.of(detalle()));

        Pedido cancelado = gestor.cancelarPedido(pedido.getId());

        assertEquals(EstadoPedido.CANCELADO, cancelado.getEstado());
        assertEquals(EstadoPedido.PENDIENTE, pedido.getEstado());
        assertEquals(List.of(siguiente.getId()),
                gestor.listarPendientes().stream().map(Pedido::getId).toList());
        assertThrows(IllegalStateException.class, () -> gestor.cancelarPedido(pedido.getId()));
        assertEquals(siguiente.getId(), gestor.iniciarSiguientePendiente().orElseThrow().getId());
        assertFalse(gestor.listarPedidos().isEmpty());
    }

    @Test
    void operacionesInvalidasNoCreanPedidosNiConsumenIdentificadores() {
        GestorPedidos gestor = new GestorPedidos();
        assertThrows(IllegalArgumentException.class,
                () -> gestor.registrarPedido(0, List.of(detalle())));
        assertThrows(IllegalArgumentException.class,
                () -> gestor.registrarPedido(10, List.of()));
        assertThrows(IllegalArgumentException.class,
                () -> gestor.registrarPedido(10, null));
        assertThrows(NoSuchElementException.class, () -> gestor.buscarPedido(999));
        assertThrows(NoSuchElementException.class,
                () -> gestor.cambiarEstado(999, EstadoPedido.LISTO));
        assertTrue(gestor.listarPedidos().isEmpty());
        assertTrue(gestor.listarPendientes().isEmpty());

        assertEquals(1, gestor.registrarPedido(10, List.of(detalle())).getId());
    }

    @Test
    void losDetallesSeCopianAlRegistrar() {
        GestorPedidos gestor = new GestorPedidos();
        List<DetallePedido> detalles = new ArrayList<>();
        detalles.add(detalle());

        Pedido pedido = gestor.registrarPedido(10, detalles);
        detalles.clear();

        assertEquals(1, pedido.getDetalles().size());
        assertEquals(new BigDecimal("7000.00"), gestor.buscarPedido(pedido.getId()).getTotal());
    }
}
