package com.demo.restaurante;

import java.util.LinkedList;
import java.util.Queue;
import java.util.Stack;

/**
 * Punto de entrada de la demostración de estructuras de datos de restaurante.
 * Los datos se mantienen en memoria y son de ejemplo.
 */
public class Restaurante {

    public static void main(String[] args) {
        Proveedor[] platillos = {
            new Proveedor(0, "Arroz con Pollo", 3000, 4000),
            new Proveedor(1, "La Carnita Roja", 4000, 6000)
        };

        Stack<Empleado> empleados = new Stack<>();
        empleados.push(new Empleado(0, "Pablo Ortiz", "Cocinero"));
        empleados.push(new Empleado(1, "Luis Calderon", "Cajero"));

        Queue<Orden> ordenes = new LinkedList<>();
        ordenes.add(new Orden(1));
        ordenes.add(new Orden(2));
        ordenes.add(new Orden(3));

        System.out.println("Empleados:");
        imprimirEmpleados(empleados);
        System.out.println("\nPlatillos:");
        imprimirPlatillos(platillos);
        System.out.println("\nÓrdenes en espera:");
        imprimirOrdenes(ordenes);
    }

    public static void imprimirEmpleados(Stack<Empleado> empleados) {
        for (Empleado empleado : empleados) {
            System.out.println("Id: " + empleado.id + ", Nombre: " + empleado.nombre
                + ", Puesto: " + empleado.puesto);
        }
    }

    public static void imprimirPlatillos(Proveedor[] platillos) {
        for (Proveedor platillo : platillos) {
            System.out.println("Id: " + platillo.id + ", Nombre: " + platillo.nombre
                + ", Gasto en Ingredientes: " + platillo.gastoIngredientes
                + ", Precio: " + platillo.precio);
        }
    }

    public static void imprimirOrdenes(Queue<Orden> ordenes) {
        for (Orden orden : ordenes) {
            System.out.println("Cliente Id: " + orden.clienteId);
        }
    }
}
