package com.demo.restaurante;

import java.math.BigDecimal;
import java.util.List;

/**
 * Pedido en memoria con detalles inmutables y estado inicial PENDIENTE.
 * La gestión de transiciones y cobros pertenece a la fase 3.
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
