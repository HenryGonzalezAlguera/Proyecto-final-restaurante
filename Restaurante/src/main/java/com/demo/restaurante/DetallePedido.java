package com.demo.restaurante;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Línea de pedido con instantánea de nombre y precio del platillo.
 */
public final class DetallePedido {
    private final int platilloId;
    private final String nombrePlatillo;
    private final int cantidad;
    private final BigDecimal precioUnitario;

    public DetallePedido(Platillo platillo, int cantidad) {
        this(platilloRequerido(platillo).getId(), platillo.getNombre(),
                cantidad, platillo.getPrecio());
    }

    private static Platillo platilloRequerido(Platillo platillo) {
        if (platillo == null) {
            throw new IllegalArgumentException("El platillo es obligatorio.");
        }
        return platillo;
    }

    private DetallePedido(int platilloId, String nombrePlatillo, int cantidad, BigDecimal precioUnitario) {
        if (platilloId <= 0) {
            throw new IllegalArgumentException("El ID del platillo debe ser positivo.");
        }
        if (nombrePlatillo == null || nombrePlatillo.isBlank()) {
            throw new IllegalArgumentException("El nombre del platillo es obligatorio.");
        }
        if (cantidad <= 0) {
            throw new IllegalArgumentException("La cantidad debe ser positiva.");
        }
        if (precioUnitario == null || precioUnitario.signum() <= 0) {
            throw new IllegalArgumentException("El precio unitario debe ser positivo.");
        }
        try {
            this.precioUnitario = precioUnitario.setScale(2, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException ex) {
            throw new IllegalArgumentException("El precio unitario admite como máximo dos decimales.", ex);
        }
        this.platilloId = platilloId;
        this.nombrePlatillo = nombrePlatillo.strip();
        this.cantidad = cantidad;
    }

    /**
     * Permite reconstruir la instantánea histórica sin consultar el precio vigente del catálogo.
     * Solo se utiliza al recuperar detalles persistidos.
     */
    static DetallePedido desdePersistencia(int platilloId, String nombrePlatillo,
                                           int cantidad, BigDecimal precioUnitario) {
        return new DetallePedido(platilloId, nombrePlatillo, cantidad, precioUnitario);
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
