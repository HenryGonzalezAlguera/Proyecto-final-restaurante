package com.demo.restaurante;

/**
 * Empleado en memoria. Los datos se validan al crear la instancia.
 */
public final class Empleado {
    private final int id;
    private final String nombre;
    private final String puesto;

    public Empleado(int id, String nombre, String puesto) {
        if (id <= 0) {
            throw new IllegalArgumentException("El ID del empleado debe ser positivo.");
        }
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("El nombre del empleado es obligatorio.");
        }
        if (puesto == null || puesto.isBlank()) {
            throw new IllegalArgumentException("El puesto del empleado es obligatorio.");
        }
        this.id = id;
        this.nombre = nombre.strip();
        this.puesto = puesto.strip();
    }

    public int getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public String getPuesto() {
        return puesto;
    }
}
