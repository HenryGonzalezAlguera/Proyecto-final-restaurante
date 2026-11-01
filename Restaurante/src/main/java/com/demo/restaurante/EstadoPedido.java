package com.demo.restaurante;

/**
 * Estados de un pedido. Las transiciones válidas se aplican en Pedido.
 */
public enum EstadoPedido {
    PENDIENTE,
    EN_PREPARACION,
    LISTO,
    ENTREGADO,
    CANCELADO
}
