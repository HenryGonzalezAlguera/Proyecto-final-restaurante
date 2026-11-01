package com.demo.restaurante;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;

/**
 * Demostración optativa que escribe datos ficticios en la base configurada.
 * Requiere --demo y las variables RESTAURANTE_DB_*.
 */
public final class RestauranteMysqlDemo {
    private RestauranteMysqlDemo() {
    }

    public static void main(String[] args) throws SQLException {
        if (args.length != 1 || !"--demo".equals(args[0])) {
            System.out.println("Esta demostración escribe en MySQL.");
            System.out.println("Usa --demo y define RESTAURANTE_DB_URL, RESTAURANTE_DB_USER y RESTAURANTE_DB_PASSWORD.");
            return;
        }

        RepositorioRestauranteJdbc repositorio =
                new RepositorioRestauranteJdbc(ConexionRestaurante::desdeEntorno);

        List<Platillo> platillos = repositorio.listarPlatillos();
        if (platillos.isEmpty()) {
            repositorio.registrarPlatillo(new Platillo(
                    1, "Arroz con Pollo", new BigDecimal("3000.00"), new BigDecimal("4000.00")));
            repositorio.registrarPlatillo(new Platillo(
                    2, "La Carnita Roja", new BigDecimal("4000.00"), new BigDecimal("6000.00")));
            platillos = repositorio.listarPlatillos();
        }

        if (repositorio.listarEmpleados().isEmpty()) {
            repositorio.registrarEmpleado(new Empleado(1, "Ana", "Cocinera"));
        }

        Platillo seleccionado = platillos.get(0);
        Pedido registrado = repositorio.registrarPedido(
                1, List.of(new DetallePedido(seleccionado, 2)));
        System.out.println("Pedido almacenado #" + registrado.getId()
                + " | CRC " + registrado.getTotal());

        Pedido recuperado = repositorio.buscarPedido(registrado.getId());
        System.out.println("Pedido recuperado #" + recuperado.getId()
                + " | Estado: " + recuperado.getEstado()
                + " | CRC " + recuperado.getTotal());

        Pedido iniciado = repositorio.iniciarSiguientePendiente()
                .orElseThrow(() -> new IllegalStateException("No hay pendientes."));
        System.out.println("Primer pedido FIFO en preparación: #" + iniciado.getId());
        System.out.println("Pedidos almacenados en total: " + repositorio.listarPedidos().size());
        System.out.println("Ejecutar nuevamente crea otro pedido; no se borran registros.");
    }
}
