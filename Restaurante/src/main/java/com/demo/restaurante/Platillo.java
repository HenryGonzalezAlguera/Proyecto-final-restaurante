package com.demo.restaurante;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Platillo del menú. Los montos se expresan en colones (CRC).
 * Modelo inmutable: los pedidos conservan el precio al momento de agregarlos.
 */
public final class Platillo {
    private final int id;
    private final String nombre;
    private final BigDecimal costoIngredientes;
    private final BigDecimal precio;

    public Platillo(int id, String nombre, BigDecimal costoIngredientes, BigDecimal precio) {
        if (id <= 0) {
            throw new IllegalArgumentException("El ID del platillo debe ser positivo.");
        }
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("El nombre del platillo es obligatorio.");
        }
        if (nombre.strip().length() > 150) {
            throw new IllegalArgumentException("El nombre no puede superar 150 caracteres.");
        }
        this.id = id;
        this.nombre = nombre.strip();
        this.costoIngredientes = montoValido(costoIngredientes, false, "Costo de ingredientes");
        this.precio = montoValido(precio, true, "Precio");
    }

    private static BigDecimal montoValido(BigDecimal monto, boolean positivo, String campo) {
        if (monto == null || monto.signum() < 0 || (positivo && monto.signum() == 0)) {
            throw new IllegalArgumentException(campo + " debe ser " + (positivo ? "positivo." : "cero o positivo."));
        }
        if (monto.compareTo(new BigDecimal("9999999999.99")) > 0) {
            throw new IllegalArgumentException(campo + " supera el límite permitido.");
        }
        try {
            return monto.setScale(2, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException e) {
            throw new IllegalArgumentException(campo + " admite máximo dos decimales.", e);
        }
    }

    public int getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public BigDecimal getCostoIngredientes() {
        return costoIngredientes;
    }

    public BigDecimal getPrecio() {
        return precio;
    }

    public BigDecimal getMargenBrutoUnitario() {
        return precio.subtract(costoIngredientes);
    }
}
