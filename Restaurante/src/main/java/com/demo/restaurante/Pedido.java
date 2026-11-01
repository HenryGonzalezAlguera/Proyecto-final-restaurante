package com.demo.restaurante;

import java.math.BigDecimal;
import java.util.List;

/**
 * Pedido inmutable: cada transición genera una nueva versión.
 * Los detalles y sus precios se conservan durante todo el ciclo.
 */
public final class Pedido {
    private final int id;
    private final int clienteId;
    private final List<DetallePedido> detalles;
    private final EstadoPedido estado;

    public Pedido(int id, int clienteId, List<DetallePedido> detalles) {
        this(id, clienteId, detalles, EstadoPedido.PENDIENTE);
    }

    private Pedido(int id, int clienteId, List<DetallePedido> detalles, EstadoPedido estado) {
        if (id <= 0) {
            throw new IllegalArgumentException("El ID del pedido debe ser positivo.");
        }
        if (clienteId <= 0) {
            throw new IllegalArgumentException("El ID del cliente debe ser positivo.");
        }
        if (detalles == null || detalles.isEmpty() || detalles.stream().anyMatch(d -> d == null)) {
            throw new IllegalArgumentException("El pedido debe contener detalles válidos.");
        }
        if (estado == null) {
            throw new IllegalArgumentException("El estado del pedido es obligatorio.");
        }
        this.id = id;
        this.clienteId = clienteId;
        this.detalles = List.copyOf(detalles);
        this.estado = estado;
    }

    /**
     * Restaura el estado persistido. No efectúa transición ni sobrescribe el precio histórico.
     */
    static Pedido desdePersistencia(int id, int clienteId, List<DetallePedido> detalles,
                                    EstadoPedido estado) {
        return new Pedido(id, clienteId, detalles, estado);
    }

    /**
     * PENDIENTE -> EN_PREPARACION o CANCELADO
     * EN_PREPARACION -> LISTO
     * LISTO -> ENTREGADO
     * ENTREGADO y CANCELADO son estados terminales.
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
        return new Pedido(id, clienteId, detalles, nuevoEstado);
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
