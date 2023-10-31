

package com.demo.restaurante;

import java.util.LinkedList;
import java.util.Queue;
import java.util.Stack;

/**
 *
 * @author Henry
 */
public class Restaurante {

    public static void main(String[] args) {
          // Lista circular de platillos
        Proveedor[] platillos = new Proveedor[2];
        platillos[0] = new Proveedor(0, "Arroz con Pollo", 3000, 4000);
        platillos[1] = new Proveedor(1, "La Carnita Roja", 4000, 6000);
        platillos[0] = platillos[1]; // Hacemos que sea circular

        // Pila de empleados
        Stack<Empleado> empleados = new Stack<>();
        empleados.push(new Empleado(0, "Pablo Ortiz", "Cocinero"));
        empleados.push(new Empleado(1, "Luis Calderon", "Cajero"));

        // Cola de órdenes de clientes
        Queue<Orden> ordenes = new LinkedList<>();
        ordenes.add(new Orden(1));
        ordenes.add(new Orden(2));
        ordenes.add(new Orden(3));

        System.out.println("Empleados:");
        imprimirEmpleados(empleados);

        System.out.println("\nPlatillos:");
        imprimirPlatillos(platillos);
        }
        
    // Función para imprimir empleados
    public static void imprimirEmpleados(Stack<Empleado> empleados) {
        for (Empleado empleado : empleados) {
            System.out.println("Id: " + empleado.id + ", Nombre: " + empleado.nombre + ", Puesto: " + empleado.puesto);
        }
    }

    // Función para imprimir platillos
    public static void imprimirPlatillos(Proveedor[] platillos) {
        for (Proveedor platillo : platillos) {
            System.out.println("Id: " + platillo.id + ", Nombre: " + platillo.nombre + ", Gasto en Ingredientes: " + platillo.gastoIngredientes + ", Precio: " + platillo.precio);
        }
    }
}