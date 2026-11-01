package com.demo.restaurante;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Queue;

/**
 * Servicio de gestión de pedidos exclusivamente en memoria.
 *
 * Registra pedidos con IDs consecutivos, conserva su orden de llegada,
 * procesa pendientes en FIFO y aplica las transiciones del dominio.
 * No es persistente ni está preparado para uso concurrente.
 */
public final class GestorPedidos {
    private final Map<Integer, Pedido> pedidos = new LinkedHashMap<>();
    private final Queue<Integer> colaPendientes = new ArrayDeque<>();
    private int siguienteId = 1;

    public Pedido registrarPedido(int clienteId, List<DetallePedido> detalles) {
        if (siguienteId <= 0) {
            throw new IllegalStateException("Se agotaron los identificadores disponibles.");
        }
        Pedido pedido = new Pedido(siguienteId, clienteId, detalles);
        pedidos.put(pedido.getId(), pedido);
        colaPendientes.add(pedido.getId());
        siguienteId++;
        return pedido;
    }

    public Pedido buscarPedido(int pedidoId) {
        Pedido pedido = pedidos.get(pedidoId);
        if (pedido == null) {
            throw new NoSuchElementException("Pedido no encontrado: " + pedidoId);
        }
        return pedido;
    }

    public List<Pedido> listarPedidos() {
        return List.copyOf(pedidos.values());
    }

    public List<Pedido> listarPendientes() {
        List<Pedido> pendientes = new ArrayList<>();
        for (Integer id : colaPendientes) {
            pendientes.add(pedidos.get(id));
        }
        return List.copyOf(pendientes);
    }

    public Pedido cambiarEstado(int pedidoId, EstadoPedido nuevoEstado) {
        Pedido actual = buscarPedido(pedidoId);
        Pedido actualizado = actual.cambiarEstado(nuevoEstado);
        pedidos.put(pedidoId, actualizado);

        if (actual.getEstado() == EstadoPedido.PENDIENTE) {
            colaPendientes.remove(pedidoId);
        }
        return actualizado;
    }

    public Pedido cancelarPedido(int pedidoId) {
        return cambiarEstado(pedidoId, EstadoPedido.CANCELADO);
    }

    /**
     * Inicia el primer pedido aún pendiente por orden de llegada.
     * No modifica pedidos que ya estén en preparación o cancelados.
     */
    public Optional<Pedido> iniciarSiguientePendiente() {
        Integer siguiente = colaPendientes.peek();
        if (siguiente == null) {
            return Optional.empty();
        }
        return Optional.of(cambiarEstado(siguiente, EstadoPedido.EN_PREPARACION));
    }
}
