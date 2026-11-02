package com.demo.restaurante;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.NoSuchElementException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

/**
 * Pruebas con MySQL real. Solamente operan contra la base aislada restaurante_ci.
 * Sin RESTAURANTE_TEST_DB_URL se omiten; los tests unitarios siguen funcionando.
 */
@EnabledIfEnvironmentVariable(named = "RESTAURANTE_TEST_DB_URL", matches = "^jdbc:mysql://.+")
class RepositorioRestauranteJdbcTest {
    private final RepositorioRestauranteJdbc repositorio =
            new RepositorioRestauranteJdbc(RepositorioRestauranteJdbcTest::conectar);

    private static Connection conectar() throws SQLException {
        String url = System.getenv("RESTAURANTE_TEST_DB_URL");
        String usuario = System.getenv("RESTAURANTE_TEST_DB_USER");
        String clave = System.getenv("RESTAURANTE_TEST_DB_PASSWORD");
        if (url == null || usuario == null || clave == null) {
            throw new IllegalStateException("Faltan variables RESTAURANTE_TEST_DB_*.");
        }
        Connection conexion = DriverManager.getConnection(url, usuario, clave);
        if (!"restaurante_ci".equals(conexion.getCatalog())) {
            conexion.close();
            throw new IllegalStateException("Las pruebas JDBC solo pueden modificar restaurante_ci.");
        }
        return conexion;
    }

    @BeforeAll
    static void crearEsquema() throws Exception {
        String texto = Files.readString(Path.of("..", "database", "01_esquema.sql"))
                .replaceAll("(?m)^\\s*--[^\\r\\n]*(?:\\r?\\n|$)", "");
        try (Connection c = conectar(); Statement stmt = c.createStatement()) {
            for (String fragmento : texto.split(";")) {
                String sql = fragmento.strip();
                if (sql.isEmpty() || sql.regionMatches(true, 0, "CREATE DATABASE", 0, 15)
                        || sql.regionMatches(true, 0, "USE ", 0, 4)) {
                    continue;
                }
                stmt.execute(sql);
            }
            stmt.execute("ALTER TABLE platillos DROP CHECK chk_platillo_id");
            stmt.execute("ALTER TABLE platillos MODIFY COLUMN id INT NOT NULL AUTO_INCREMENT");
        }
    }

    @BeforeEach
    void limpiarYCrearCatalogo() throws SQLException {
        try (Connection c = conectar(); Statement stmt = c.createStatement()) {
            stmt.executeUpdate("DELETE FROM pedido_detalles");
            stmt.executeUpdate("DELETE FROM pedidos");
            stmt.executeUpdate("DELETE FROM platillos");
            stmt.executeUpdate("DELETE FROM empleados");
        }
        repositorio.registrarPlatillo(new Platillo(
                1, "Casado", new BigDecimal("2000"), new BigDecimal("3500")));
        repositorio.registrarPlatillo(new Platillo(
                2, "Arroz con Pollo", new BigDecimal("2300"), new BigDecimal("4000")));
        repositorio.registrarEmpleado(new Empleado(1, "Ana", "Cocinera"));
    }

    private DetallePedido detalle(int id, int cantidad) {
        Platillo p = repositorioCatalogo(id);
        return new DetallePedido(p, cantidad);
    }

    private Platillo repositorioCatalogo(int id) {
        return repositorioSinSqlException(id);
    }

    private Platillo repositorioSinSqlException(int id) {
        if (id == 1) {
            return new Platillo(1, "Casado", new BigDecimal("2000"), new BigDecimal("3500"));
        }
        return new Platillo(2, "Arroz con Pollo", new BigDecimal("2300"), new BigDecimal("4000"));
    }

    @Test
    void persisteCatalogoPersonalYDetallesConImportes() throws Exception {
        assertEquals(2, repositorio.listarPlatillos().size());
        assertEquals("Ana", repositorio.listarEmpleados().get(0).getNombre());

        Pedido nuevo = repositorio.registrarPedido(10, List.of(detalle(1, 2), detalle(2, 1)));
        assertTrue(nuevo.getId() > 0);
        assertEquals(new BigDecimal("11000.00"), nuevo.getTotal());

        // Nueva instancia y conexión: confirma que no depende de la memoria del proceso.
        RepositorioRestauranteJdbc segundaInstancia =
                new RepositorioRestauranteJdbc(RepositorioRestauranteJdbcTest::conectar);
        Pedido recuperado = segundaInstancia.buscarPedido(nuevo.getId());
        assertEquals(EstadoPedido.PENDIENTE, recuperado.getEstado());
        assertEquals(new BigDecimal("11000.00"), recuperado.getTotal());
        assertEquals(2, recuperado.getDetalles().size());
        assertEquals("Casado", recuperado.getDetalles().get(0).getNombrePlatillo());
        assertEquals(List.of(nuevo.getId()),
                segundaInstancia.listarPendientes().stream().map(Pedido::getId).toList());
    }

    @Test
    void conservaPrecioHistoricoTrasActualizarCatalogo() throws Exception {
        Pedido anterior = repositorio.registrarPedido(10, List.of(detalle(1, 1)));
        try (Connection c = conectar();
             PreparedStatement ps = c.prepareStatement("UPDATE platillos SET precio = ? WHERE id = 1")) {
            ps.setBigDecimal(1, new BigDecimal("4500.00"));
            ps.executeUpdate();
        }
        Pedido recuperado = repositorio.buscarPedido(anterior.getId());
        assertEquals(new BigDecimal("3500.00"), recuperado.getTotal());
        assertEquals(new BigDecimal("3500.00"),
                recuperado.getDetalles().get(0).getPrecioUnitario());
        assertThrows(IllegalArgumentException.class,
                () -> repositorio.registrarPedido(11, List.of(detalle(1, 1))));
    }

    @Test
    void gestionaEstadosYFIFOEnBaseDeDatos() throws Exception {
        Pedido a = repositorio.registrarPedido(1, List.of(detalle(1, 1)));
        Pedido b = repositorio.registrarPedido(2, List.of(detalle(2, 1)));
        Pedido c = repositorio.registrarPedido(3, List.of(detalle(1, 1)));

        repositorio.cancelarPedido(b.getId());
        assertEquals(a.getId(), repositorio.iniciarSiguientePendiente().orElseThrow().getId());
        assertEquals(c.getId(), repositorio.iniciarSiguientePendiente().orElseThrow().getId());
        assertTrue(repositorio.iniciarSiguientePendiente().isEmpty());

        assertThrows(IllegalStateException.class, () -> repositorio.cancelarPedido(a.getId()));
        assertThrows(IllegalStateException.class,
                () -> repositorio.cambiarEstado(a.getId(), EstadoPedido.ENTREGADO));
        repositorio.cambiarEstado(a.getId(), EstadoPedido.LISTO);
        repositorio.cambiarEstado(a.getId(), EstadoPedido.ENTREGADO);
        assertEquals(EstadoPedido.ENTREGADO, repositorio.buscarPedido(a.getId()).getEstado());
        assertEquals(EstadoPedido.CANCELADO, repositorio.buscarPedido(b.getId()).getEstado());
        assertEquals(EstadoPedido.EN_PREPARACION, repositorio.buscarPedido(c.getId()).getEstado());
        assertTrue(repositorio.listarPendientes().isEmpty());
        assertEquals(3, repositorio.listarPedidos().size());
    }

    @Test
    void rechazaDatosInvalidosSinCrearPedidos() throws Exception {
        assertThrows(IllegalArgumentException.class,
                () -> repositorio.registrarPedido(1, List.of()));
        assertThrows(IllegalArgumentException.class,
                () -> repositorio.registrarPedido(1, List.of(
                        new DetallePedido(new Platillo(
                                99, "Desconocido", BigDecimal.ONE, BigDecimal.TEN), 1))));
        assertThrows(NoSuchElementException.class, () -> repositorio.buscarPedido(999999));
        assertFalse(repositorio.listarPedidos().iterator().hasNext());
    }

    @Test
    void revierteCabeceraSiFallaInsercionDelDetalle() throws Exception {
        // Restricción exclusiva de la base restaurante_ci: fuerza un fallo
        // en el segundo INSERT, cuando la cabecera ya existe en la transacción.
        try (Connection c = conectar(); Statement stmt = c.createStatement()) {
            stmt.execute("ALTER TABLE pedido_detalles "
                    + "ADD CONSTRAINT chk_test_rechazar CHECK (cantidad < 2)");
        }
        try {
            assertThrows(SQLException.class,
                    () -> repositorio.registrarPedido(1, List.of(detalle(1, 2))));
            assertTrue(repositorio.listarPedidos().isEmpty(),
                    "El rollback debe eliminar la cabecera insertada antes del fallo.");
        } finally {
            try (Connection c = conectar(); Statement stmt = c.createStatement()) {
                stmt.execute("ALTER TABLE pedido_detalles DROP CHECK chk_test_rechazar");
            }
        }
    }
    @Test
    void creaPlatilloConIdAutomaticoYPermiteEditarSinRepreciarPedidos() throws Exception {
        Pedido historico = repositorio.registrarPedido(10, List.of(detalle(1, 2)));

        Platillo creado = repositorio.crearPlatillo("Ensalada", new BigDecimal("1200"),
                new BigDecimal("2500"));
        assertTrue(creado.getId() > 2);
        assertTrue(repositorio.listarPlatillos().stream()
                .anyMatch(p -> p.getId() == creado.getId()));

        var actualizado = repositorio.actualizarPlatillo(1, "Casado especial",
                new BigDecimal("2200"), new BigDecimal("4000"));
        assertTrue(actualizado.activo());
        assertEquals("Casado especial", actualizado.platillo().getNombre());

        Pedido cargado = repositorio.buscarPedido(historico.getId());
        assertEquals(new BigDecimal("7000.00"), cargado.getTotal());
        assertEquals("Casado", cargado.getDetalles().get(0).getNombrePlatillo());
        assertEquals(new BigDecimal("3500.00"), cargado.getDetalles().get(0).getPrecioUnitario());
    }

    @Test
    void desactivacionLogicaConservaPedidosEImpideNuevasOrdenes() throws Exception {
        Pedido historico = repositorio.registrarPedido(10, List.of(detalle(1, 1)));
        repositorio.cambiarDisponibilidadPlatillo(1, false);

        assertFalse(repositorio.listarPlatillos().stream().anyMatch(p -> p.getId() == 1));
        var desactivado = repositorio.listarCatalogoAdministracion().stream()
                .filter(r -> r.platillo().getId() == 1).findFirst().orElseThrow();
        assertFalse(desactivado.activo());
        assertEquals(new BigDecimal("3500.00"),
                repositorio.buscarPedido(historico.getId()).getTotal());
        assertThrows(IllegalArgumentException.class,
                () -> repositorio.registrarPedido(20, List.of(detalle(1, 1))));

        repositorio.cambiarDisponibilidadPlatillo(1, true);
        assertTrue(repositorio.listarPlatillos().stream().anyMatch(p -> p.getId() == 1));
        assertEquals(EstadoPedido.PENDIENTE,
                repositorio.registrarPedido(20, List.of(detalle(1, 1))).getEstado());
    }

    @Test
    void rechazaIdsInexistentesSinModificarDatos() throws Exception {
        assertThrows(NoSuchElementException.class, () -> repositorio.actualizarPlatillo(
                999999, "Inexistente", BigDecimal.ONE, BigDecimal.TEN));
        assertThrows(NoSuchElementException.class,
                () -> repositorio.cambiarDisponibilidadPlatillo(999999, false));
        assertThrows(IllegalArgumentException.class,
                () -> repositorio.crearPlatillo(" ", BigDecimal.ONE, BigDecimal.TEN));
        assertEquals(2, repositorio.listarPlatillos().size());
    }


}
