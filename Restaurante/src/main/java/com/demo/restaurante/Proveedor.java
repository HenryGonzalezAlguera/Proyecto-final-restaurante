
package com.demo.restaurante;

/**
 *
 * @author Henry
 */
public class Proveedor {
    int id;
    String nombre;
    int gastoIngredientes;
    int precio;

    public Proveedor(int id, String nombre, int gastoIngredientes, int precio) {
        this.id = id;
        this.nombre = nombre;
        this.gastoIngredientes = gastoIngredientes;
        this.precio = precio;
    }
    
}
