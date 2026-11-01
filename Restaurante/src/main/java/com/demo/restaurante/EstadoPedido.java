package com.demo.restaurante;

/**
 * Estados previstos para el ciclo de vida del pedido.
 * Las transiciones se implementarán y validarán en la fase 3.
 */
public enum EstadoPedido {
    PENDIENTE,
    EN_PREPARACION,
    LISTO,
    ENTREGADO,
    CANCELADO
}
