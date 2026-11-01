package com.demo.restaurante;

import java.math.BigDecimal;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Stack;

/**
 * Demostración de consola sobre las entidades del dominio.
 * Los datos son ficticios y solo existen durante esta ejecución.
 */
public class Restaurante {

    public static void main(String[] args) {
        List<Platillo> menu = List.of(
                new Platillo(1, "Arroz con Pollo", new BigDecimal("3000.00"), new BigDecimal("4000.00")),
                new Platillo(2, "La Carnita Roja", new BigDecimal("4000.00"), new BigDecimal("6000.00"))
        );

        Stack<Empleado> empleados = new Stack<>();
        empleados.push(new Empleado(1, "Pablo Ortiz", "Cocinero"));
        empleados.push(new Empleado(2, "Luis Calderon", "Cajero"));

        Queue<Pedido> pedidos = new LinkedList<>();
        pedidos.add(new Pedido(1, 1, List.of(new DetallePedido(menu.get(0), 2))));
        pedidos.add(new Pedido(2, 2, List.of(
                new DetallePedido(menu.get(1), 1),
                new DetallePedido(menu.get(0), 1)
        )));
        pedidos.add(new Pedido(3, 3, List.of(new DetallePedido(menu.get(1), 2))));

        System.out.println("Empleados:");
        imprimirEmpleados(empleados);

        System.out.println("\nPlatillos:");
        imprimirPlatillos(menu);

        System.out.println("\nPedidos pendientes:");
        imprimirPedidos(pedidos);
    }

    public static void imprimirEmpleados(Stack<Empleado> empleados) {
        for (Empleado empleado : empleados) {
            System.out.println("Id: " + empleado.getId() + ", Nombre: " + empleado.getNombre()
                    + ", Puesto: " + empleado.getPuesto());
        }
    }

    public static void imprimirPlatillos(List<Platillo> platillos) {
        for (Platillo platillo : platillos) {
            System.out.println("Id: " + platillo.getId() + ", Nombre: " + platillo.getNombre()
                    + ", Costo Ingredientes: " + platillo.getCostoIngredientes()
                    + ", Precio: " + platillo.getPrecio());
        }
    }

    public static void imprimirPedidos(Queue<Pedido> pedidos) {
        for (Pedido pedido : pedidos) {
            System.out.println("Pedido #" + pedido.getId() + " | Cliente: " + pedido.getClienteId()
                    + " | Estado: " + pedido.getEstado() + " | Total CRC: " + pedido.getTotal());
            for (DetallePedido detalle : pedido.getDetalles()) {
                System.out.println("  - " + detalle.getNombrePlatillo()
                        + " x" + detalle.getCantidad() + ": CRC " + detalle.getSubtotal());
            }
        }
    }
}
