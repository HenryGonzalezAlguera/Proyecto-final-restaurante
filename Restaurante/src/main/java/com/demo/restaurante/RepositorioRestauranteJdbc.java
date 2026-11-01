package com.demo.restaurante;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

/**
 * Persistencia de catálogo, empleados y pedidos en MySQL 8 mediante JDBC.
 *
 * Registro de pedido y detalles: transacción única.
 * Cambios de estado: bloqueo FOR UPDATE y transacción única.
 * El orden de preparación se determina por el ID autoincremental (FIFO).
 *
 * No configura conexiones globales ni mantiene credenciales en el código.
 */
public final class RepositorioRestauranteJdbc {
    private final FabricaConexiones fabrica;

    public RepositorioRestauranteJdbc(FabricaConexiones fabrica) {
        if (fabrica == null) {
            throw new IllegalArgumentException("La fábrica de conexiones es obligatoria.");
        }
        this.fabrica = fabrica;
    }

    public void registrarPlatillo(Platillo platillo) throws SQLException {
        if (platillo == null) {
            throw new IllegalArgumentException("El platillo es obligatorio.");
        }
        String sql = "INSERT INTO platillos(id, nombre, costo_ingredientes, precio) VALUES (?, ?, ?, ?)";
        try (Connection conexion = fabrica.abrir();
             PreparedStatement ps = conexion.prepareStatement(sql)) {
            ps.setInt(1, platillo.getId());
            ps.setString(2, platillo.getNombre());
            ps.setBigDecimal(3, platillo.getCostoIngredientes());
            ps.setBigDecimal(4, platillo.getPrecio());
            ps.executeUpdate();
        }
    }

    public void registrarEmpleado(Empleado empleado) throws SQLException {
        if (empleado == null) {
            throw new IllegalArgumentException("El empleado es obligatorio.");
        }
        String sql = "INSERT INTO empleados(id, nombre, puesto) VALUES (?, ?, ?)";
        try (Connection conexion = fabrica.abrir();
             PreparedStatement ps = conexion.prepareStatement(sql)) {
            ps.setInt(1, empleado.getId());
            ps.setString(2, empleado.getNombre());
            ps.setString(3, empleado.getPuesto());
            ps.executeUpdate();
        }
    }

    public List<Platillo> listarPlatillos() throws SQLException {
        String sql = "SELECT id, nombre, costo_ingredientes, precio FROM platillos WHERE activo = TRUE ORDER BY id";
        List<Platillo> lista = new ArrayList<>();
        try (Connection conexion = fabrica.abrir();
             PreparedStatement ps = conexion.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                lista.add(new Platillo(
                        rs.getInt("id"),
                        rs.getString("nombre"),
                        rs.getBigDecimal("costo_ingredientes"),
                        rs.getBigDecimal("precio")));
            }
        }
        return List.copyOf(lista);
    }

    public List<Empleado> listarEmpleados() throws SQLException {
        String sql = "SELECT id, nombre, puesto FROM empleados ORDER BY id";
        List<Empleado> lista = new ArrayList<>();
        try (Connection conexion = fabrica.abrir();
             PreparedStatement ps = conexion.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                lista.add(new Empleado(rs.getInt("id"),
                        rs.getString("nombre"), rs.getString("puesto")));
            }
        }
        return List.copyOf(lista);
    }

    /**
     * Valida el precio del catálogo en la base y registra cabecera y detalles
     * de forma atómica. Un fallo revierte todo, sin generar pedidos incompletos.
     */
    public Pedido registrarPedido(int clienteId, List<DetallePedido> detalles) throws SQLException {
        if (clienteId <= 0 || detalles == null || detalles.isEmpty()
                || detalles.stream().anyMatch(d -> d == null)) {
            throw new IllegalArgumentException("Cliente y detalles del pedido no son válidos.");
        }

        try (Connection conexion = fabrica.abrir()) {
            conexion.setAutoCommit(false);
            try {
                for (DetallePedido detalle : detalles) {
                    verificarPlatilloYPrecio(conexion, detalle);
                }

                int pedidoId;
                try (PreparedStatement ps = conexion.prepareStatement(
                        "INSERT INTO pedidos(cliente_id, estado) VALUES (?, ?)",
                        Statement.RETURN_GENERATED_KEYS)) {
                    ps.setInt(1, clienteId);
                    ps.setString(2, EstadoPedido.PENDIENTE.name());
                    if (ps.executeUpdate() != 1) {
                        throw new SQLException("No se pudo registrar la cabecera del pedido.");
                    }
                    try (ResultSet claves = ps.getGeneratedKeys()) {
                        if (!claves.next()) {
                            throw new SQLException("MySQL no devolvió el ID del pedido.");
                        }
                        pedidoId = claves.getInt(1);
                    }
                }

                String sql = "INSERT INTO pedido_detalles "
                        + "(pedido_id, platillo_id, nombre_platillo, cantidad, precio_unitario) "
                        + "VALUES (?, ?, ?, ?, ?)";
                try (PreparedStatement ps = conexion.prepareStatement(sql)) {
                    for (DetallePedido detalle : detalles) {
                        ps.setInt(1, pedidoId);
                        ps.setInt(2, detalle.getPlatilloId());
                        ps.setString(3, detalle.getNombrePlatillo());
                        ps.setInt(4, detalle.getCantidad());
                        ps.setBigDecimal(5, detalle.getPrecioUnitario());
                        ps.addBatch();
                    }
                    int[] resultados = ps.executeBatch();
                    if (resultados.length != detalles.size()) {
                        throw new SQLException("No se guardaron todos los detalles.");
                    }
                    for (int resultado : resultados) {
                        if (resultado == Statement.EXECUTE_FAILED || resultado == 0) {
                            throw new SQLException("Fallo al registrar un detalle de pedido.");
                        }
                    }
                }

                Pedido pedido = new Pedido(pedidoId, clienteId, detalles);
                conexion.commit();
                return pedido;
            } catch (SQLException | RuntimeException ex) {
                deshacer(conexion, ex);
                throw ex;
            }
        }
    }

    private static void verificarPlatilloYPrecio(Connection conexion, DetallePedido detalle)
            throws SQLException {
        String sql = "SELECT nombre, precio FROM platillos WHERE id = ? AND activo = TRUE FOR SHARE";
        try (PreparedStatement ps = conexion.prepareStatement(sql)) {
            ps.setInt(1, detalle.getPlatilloId());
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new IllegalArgumentException(
                            "El platillo no existe o está inactivo: " + detalle.getPlatilloId());
                }
                String nombreActual = rs.getString("nombre");
                BigDecimal precioActual = rs.getBigDecimal("precio");
                if (!nombreActual.equals(detalle.getNombrePlatillo())
                        || precioActual.compareTo(detalle.getPrecioUnitario()) != 0) {
                    throw new IllegalArgumentException(
                            "El detalle tiene datos desactualizados del platillo: "
                                    + detalle.getPlatilloId());
                }
            }
        }
    }

    public Pedido buscarPedido(int pedidoId) throws SQLException {
        try (Connection conexion = fabrica.abrir()) {
            return cargarPedido(conexion, pedidoId, false);
        }
    }

    public List<Pedido> listarPedidos() throws SQLException {
        return listarPorConsulta("SELECT id FROM pedidos ORDER BY id");
    }

    public List<Pedido> listarPendientes() throws SQLException {
        return listarPorConsulta("SELECT id FROM pedidos WHERE estado = 'PENDIENTE' ORDER BY id");
    }

    private List<Pedido> listarPorConsulta(String sql) throws SQLException {
        List<Pedido> pedidos = new ArrayList<>();
        try (Connection conexion = fabrica.abrir();
             PreparedStatement ps = conexion.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            List<Integer> ids = new ArrayList<>();
            while (rs.next()) {
                ids.add(rs.getInt("id"));
            }
            for (int id : ids) {
                pedidos.add(cargarPedido(conexion, id, false));
            }
        }
        return List.copyOf(pedidos);
    }

    public Pedido cambiarEstado(int pedidoId, EstadoPedido nuevoEstado) throws SQLException {
        try (Connection conexion = fabrica.abrir()) {
            conexion.setAutoCommit(false);
            try {
                Pedido actual = cargarPedido(conexion, pedidoId, true);
                Pedido actualizado = actualizarEstadoBloqueado(conexion, actual, nuevoEstado);
                conexion.commit();
                return actualizado;
            } catch (SQLException | RuntimeException ex) {
                deshacer(conexion, ex);
                throw ex;
            }
        }
    }

    public Pedido cancelarPedido(int pedidoId) throws SQLException {
        return cambiarEstado(pedidoId, EstadoPedido.CANCELADO);
    }

    /**
     * Selecciona y bloquea el primer pendiente disponible.
     * SKIP LOCKED evita que dos trabajadores tomen el mismo pedido.
     */
    public Optional<Pedido> iniciarSiguientePendiente() throws SQLException {
        try (Connection conexion = fabrica.abrir()) {
            conexion.setAutoCommit(false);
            try {
                Integer id = null;
                try (PreparedStatement ps = conexion.prepareStatement(
                        "SELECT id FROM pedidos WHERE estado = 'PENDIENTE' "
                                + "ORDER BY id LIMIT 1 FOR UPDATE SKIP LOCKED");
                     ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        id = rs.getInt("id");
                    }
                }
                if (id == null) {
                    conexion.commit();
                    return Optional.empty();
                }
                Pedido actual = cargarPedido(conexion, id, false);
                Pedido actualizado = actualizarEstadoBloqueado(
                        conexion, actual, EstadoPedido.EN_PREPARACION);
                conexion.commit();
                return Optional.of(actualizado);
            } catch (SQLException | RuntimeException ex) {
                deshacer(conexion, ex);
                throw ex;
            }
        }
    }

    private static Pedido actualizarEstadoBloqueado(
            Connection conexion, Pedido actual, EstadoPedido nuevoEstado) throws SQLException {
        Pedido actualizado = actual.cambiarEstado(nuevoEstado);
        String sql = "UPDATE pedidos SET estado = ? WHERE id = ? AND estado = ?";
        try (PreparedStatement ps = conexion.prepareStatement(sql)) {
            ps.setString(1, actualizado.getEstado().name());
            ps.setInt(2, actual.getId());
            ps.setString(3, actual.getEstado().name());
            if (ps.executeUpdate() != 1) {
                throw new SQLException("El pedido cambió durante la operación; no se actualizó.");
            }
        }
        return actualizado;
    }

    private static Pedido cargarPedido(Connection conexion, int id, boolean bloquear)
            throws SQLException {
        String sql = "SELECT cliente_id, estado FROM pedidos WHERE id = ?"
                + (bloquear ? " FOR UPDATE" : "");
        int clienteId;
        EstadoPedido estado;
        try (PreparedStatement ps = conexion.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new NoSuchElementException("Pedido no encontrado: " + id);
                }
                clienteId = rs.getInt("cliente_id");
                estado = EstadoPedido.valueOf(rs.getString("estado"));
            }
        }
        List<DetallePedido> detalles = new ArrayList<>();
        try (PreparedStatement ps = conexion.prepareStatement(
                "SELECT platillo_id, nombre_platillo, cantidad, precio_unitario "
                        + "FROM pedido_detalles WHERE pedido_id = ? ORDER BY id")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    detalles.add(DetallePedido.desdePersistencia(
                            rs.getInt("platillo_id"),
                            rs.getString("nombre_platillo"),
                            rs.getInt("cantidad"),
                            rs.getBigDecimal("precio_unitario")));
                }
            }
        }
        return Pedido.desdePersistencia(id, clienteId, detalles, estado);
    }

    private static void deshacer(Connection conexion, Exception causa) {
        try {
            conexion.rollback();
        } catch (SQLException errorRollback) {
            causa.addSuppressed(errorRollback);
        }
    }
}
