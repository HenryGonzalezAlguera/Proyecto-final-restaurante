package com.demo.restaurante;

/**
 * Vista del registro de catálogo que conserva disponibilidad sin alterar
 * el modelo inmutable Platillo usado por las órdenes.
 */
public record RegistroPlatillo(Platillo platillo, boolean activo) {
    public RegistroPlatillo {
        if (platillo == null) {
            throw new IllegalArgumentException("El platillo es obligatorio.");
        }
    }
}
