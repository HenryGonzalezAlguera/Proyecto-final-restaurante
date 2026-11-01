package com.demo.restaurante;

import java.math.BigDecimal;

/**
 * Línea de pedido con instantánea del nombre y precio del platillo.
 */
public final class DetallePedido {
    private final int platilloId;
    private final String nombrePlatillo;
    private final int cantidad;
    private final BigDecimal precioUnitario;

    public DetallePedido(Platillo platillo, int cantidad) {
        if (platillo == null) {
            throw new IllegalArgumentException("El platillo es obligatorio.");
        }
        if (cantidad <= 0) {
            throw new IllegalArgumentException("La cantidad debe ser positiva.");
        }
        this.platilloId = platillo.getId();
        this.nombrePlatillo = platillo.getNombre();
        this.cantidad = cantidad;
        this.precioUnitario = platillo.getPrecio();
    }

    public int getPlatilloId() {
        return platilloId;
    }

    public String getNombrePlatillo() {
        return nombrePlatillo;
    }

    public int getCantidad() {
        return cantidad;
    }

    public BigDecimal getPrecioUnitario() {
        return precioUnitario;
    }

    public BigDecimal getSubtotal() {
        return precioUnitario.multiply(BigDecimal.valueOf(cantidad));
    }
}
