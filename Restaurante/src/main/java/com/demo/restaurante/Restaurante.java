package com.demo.restaurante;

import java.math.BigDecimal;
import java.util.List;
import java.util.Stack;

/**
 * Demostración de consola de registro y ciclo de vida de pedidos.
 * Los datos son ficticios y se almacenan únicamente en memoria.
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

        GestorPedidos gestor = new GestorPedidos();
        Pedido primerPedido = gestor.registrarPedido(1,
                List.of(new DetallePedido(menu.get(0), 2)));
        gestor.registrarPedido(2, List.of(
                new DetallePedido(menu.get(1), 1),
                new DetallePedido(menu.get(0), 1)
        ));
        Pedido tercerPedido = gestor.registrarPedido(3,
                List.of(new DetallePedido(menu.get(1), 2)));

        // Ejemplo de cancelación antes de iniciar la preparación.
        gestor.cancelarPedido(tercerPedido.getId());

        // La cola FIFO inicia el primer pedido pendiente.
        Pedido enPreparacion = gestor.iniciarSiguientePendiente()
                .orElseThrow(() -> new IllegalStateException("No hay pedidos pendientes."));
        if (enPreparacion.getId() != primerPedido.getId()) {
            throw new IllegalStateException("Se esperaba procesar el primer pedido registrado.");
        }
        gestor.cambiarEstado(enPreparacion.getId(), EstadoPedido.LISTO);
        gestor.cambiarEstado(enPreparacion.getId(), EstadoPedido.ENTREGADO);

        System.out.println("Empleados:");
        imprimirEmpleados(empleados);
        System.out.println("\nPlatillos:");
        imprimirPlatillos(menu);
        System.out.println("\nPedidos registrados:");
        imprimirPedidos(gestor.listarPedidos());
        System.out.println("\nPendientes en cola: " + gestor.listarPendientes().size());
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

    public static void imprimirPedidos(List<Pedido> pedidos) {
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
