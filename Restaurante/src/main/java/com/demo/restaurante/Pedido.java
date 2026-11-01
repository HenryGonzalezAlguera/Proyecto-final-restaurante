package com.demo.restaurante;

import java.math.BigDecimal;
import java.util.List;

/**
 * Pedido inmutable: cada transición válida genera una nueva versión del pedido.
 * Los detalles y sus precios se conservan sin cambios durante todo su ciclo.
 */
public final class Pedido {
    private final int id;
    private final int clienteId;
    private final List<DetallePedido> detalles;
    private final EstadoPedido estado;

    public Pedido(int id, int clienteId, List<DetallePedido> detalles) {
        if (id <= 0) {
            throw new IllegalArgumentException("El ID del pedido debe ser positivo.");
        }
        if (clienteId <= 0) {
            throw new IllegalArgumentException("El ID del cliente debe ser positivo.");
        }
        if (detalles == null || detalles.isEmpty() || detalles.stream().anyMatch(d -> d == null)) {
            throw new IllegalArgumentException("El pedido debe contener detalles válidos.");
        }
        this.id = id;
        this.clienteId = clienteId;
        this.detalles = List.copyOf(detalles);
        this.estado = EstadoPedido.PENDIENTE;
    }

    private Pedido(Pedido anterior, EstadoPedido siguienteEstado) {
        this.id = anterior.id;
        this.clienteId = anterior.clienteId;
        this.detalles = anterior.detalles;
        this.estado = siguienteEstado;
    }

    /**
     * Transiciones permitidas:
     * PENDIENTE -> EN_PREPARACION o CANCELADO
     * EN_PREPARACION -> LISTO
     * LISTO -> ENTREGADO
     * ENTREGADO/CANCELADO -> ninguna (estados terminales).
     */
    public Pedido cambiarEstado(EstadoPedido nuevoEstado) {
        if (nuevoEstado == null) {
            throw new IllegalArgumentException("El nuevo estado es obligatorio.");
        }
        boolean permitido = switch (estado) {
            case PENDIENTE -> nuevoEstado == EstadoPedido.EN_PREPARACION
                    || nuevoEstado == EstadoPedido.CANCELADO;
            case EN_PREPARACION -> nuevoEstado == EstadoPedido.LISTO;
            case LISTO -> nuevoEstado == EstadoPedido.ENTREGADO;
            case ENTREGADO, CANCELADO -> false;
        };
        if (!permitido) {
            throw new IllegalStateException(
                    "Transición de pedido no permitida: " + estado + " -> " + nuevoEstado);
        }
        return new Pedido(this, nuevoEstado);
    }

    public int getId() {
        return id;
    }

    public int getClienteId() {
        return clienteId;
    }

    public List<DetallePedido> getDetalles() {
        return detalles;
    }

    public EstadoPedido getEstado() {
        return estado;
    }

    public BigDecimal getTotal() {
        return detalles.stream()
                .map(DetallePedido::getSubtotal)
                .reduce(BigDecimal.ZERO.setScale(2), BigDecimal::add);
    }
}
