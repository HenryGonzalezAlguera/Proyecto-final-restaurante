package com.demo.restaurante.web;

import static org.junit.jupiter.api.Assertions.*;

import com.demo.restaurante.ConexionRestaurante;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Ejercita la persistencia real solo en la base MySQL efímera restaurante_ci.
 * No usa ni modifica la base local del usuario.
 */
@SpringBootTest
@EnabledIfEnvironmentVariable(named = "RESTAURANTE_SECURITY_TESTS", matches = "true")
class AdministracionEmpleadosMysqlTest {
    private static final String USUARIO_TEST = "empleado_fase9a_ci";

    @Autowired AdministracionEmpleadosService servicio;
    @Autowired PasswordEncoder encoder;

    @BeforeAll
    static void prepararTabla() throws Exception {
        try (Connection conexion = ConexionRestaurante.desdeEntorno()) {
            exigirBaseDePruebas(conexion);
            String sqlScript = Files.readString(
                    Path.of("..", "database", "04_usuarios_seguridad.sql"))
                    .replaceAll("(?m)^\\s*--[^\\r\\n]*(?:\\r?\\n|$)", "");
            try (Statement stmt = conexion.createStatement()) {
                for (String sentencia : sqlScript.split(";")) {
                    String sql = sentencia.strip();
                    if (!sql.isBlank() && !sql.regionMatches(true, 0, "USE ", 0, 4)) {
                        stmt.execute(sql);
                    }
                }
            }
        }
    }

    @Test
    void creaEmpleadoConHashBcryptYNoPermiteDuplicarlo() throws Exception {
        String clave = "ClaveTemporalCI2026Fase9!";
        try (Connection conexion = ConexionRestaurante.desdeEntorno()) {
            exigirBaseDePruebas(conexion);
            borrarUsuario(conexion);
            try {
                servicio.crearEmpleado(USUARIO_TEST, clave);

                try (PreparedStatement ps = conexion.prepareStatement(
                        "SELECT usuario, password_hash, rol, activo FROM usuarios WHERE usuario=?")) {
                    ps.setString(1, USUARIO_TEST);
                    try (ResultSet rs = ps.executeQuery()) {
                        assertTrue(rs.next());
                        assertEquals("EMPLEADO", rs.getString("rol"));
                        assertTrue(rs.getBoolean("activo"));
                        assertNotEquals(clave, rs.getString("password_hash"));
                        assertTrue(encoder.matches(clave, rs.getString("password_hash")));
                    }
                }

                assertTrue(servicio.listarEmpleados().stream()
                        .anyMatch(e -> e.usuario().equals(USUARIO_TEST)));
                assertThrows(IllegalArgumentException.class,
                        () -> servicio.crearEmpleado(USUARIO_TEST, clave));
            } finally {
                borrarUsuario(conexion);
            }
        }
    }

    private static void exigirBaseDePruebas(Connection conexion) throws Exception {
        if (!"restaurante_ci".equals(conexion.getCatalog())) {
            throw new IllegalStateException("La prueba solo puede escribir en restaurante_ci.");
        }
    }

    private static void borrarUsuario(Connection conexion) throws Exception {
        try (PreparedStatement ps = conexion.prepareStatement(
                "DELETE FROM usuarios WHERE usuario=? AND rol='EMPLEADO'")) {
            ps.setString(1, USUARIO_TEST);
            ps.executeUpdate();
        }
    }
    @Test
    void desactivacionReactivacionYClaveReemplazadaPersistenEnMysql() throws Exception {
        String claveAnterior = "ClaveInicialParaCI2026!";
        String claveNueva = "NuevaClaveSeguraCI2026!";
        try (Connection conexion = ConexionRestaurante.desdeEntorno()) {
            exigirBaseDePruebas(conexion);
            borrarUsuario(conexion);
            try {
                servicio.crearEmpleado(USUARIO_TEST, claveAnterior);
                int id = servicio.listarEmpleados().stream()
                        .filter(e -> e.usuario().equals(USUARIO_TEST))
                        .findFirst().orElseThrow().id();

                servicio.cambiarEstadoEmpleado(id, false);
                assertFalse(servicio.listarEmpleados().stream()
                        .filter(e -> e.id() == id).findFirst().orElseThrow().activo());

                servicio.restablecerClaveEmpleado(id, claveNueva, claveNueva);
                try (PreparedStatement ps = conexion.prepareStatement(
                        "SELECT password_hash, activo FROM usuarios WHERE id=? AND rol='EMPLEADO'")) {
                    ps.setInt(1, id);
                    try (ResultSet rs = ps.executeQuery()) {
                        assertTrue(rs.next());
                        assertTrue(encoder.matches(claveNueva, rs.getString("password_hash")));
                        assertFalse(encoder.matches(claveAnterior, rs.getString("password_hash")));
                        assertFalse(rs.getBoolean("activo"));
                    }
                }
                servicio.cambiarEstadoEmpleado(id, true);
                assertTrue(servicio.listarEmpleados().stream()
                        .filter(e -> e.id() == id).findFirst().orElseThrow().activo());
                assertThrows(IllegalArgumentException.class,
                        () -> servicio.cambiarEstadoEmpleado(id, true));
            } finally {
                borrarUsuario(conexion);
            }
        }
    }

    @Test
    void operacionesPorIdNuncaModificanCuentasAdmin() throws Exception {
        String admin = "admin_resguardo_ci";
        int adminId;
        try (Connection conexion = ConexionRestaurante.desdeEntorno()) {
            exigirBaseDePruebas(conexion);
            try (PreparedStatement ps = conexion.prepareStatement(
                    "INSERT INTO usuarios(usuario,password_hash,rol,activo) "
                            + "VALUES (?, ?, 'ADMIN', TRUE) ON DUPLICATE KEY UPDATE activo=TRUE")) {
                ps.setString(1, admin);
                ps.setString(2, encoder.encode("ClaveAdminTemporalCI2026!"));
                ps.executeUpdate();
            }
            try (PreparedStatement ps = conexion.prepareStatement(
                    "SELECT id FROM usuarios WHERE usuario=?")) {
                ps.setString(1, admin);
                try (ResultSet rs = ps.executeQuery()) {
                    assertTrue(rs.next());
                    adminId = rs.getInt(1);
                }
            }
            try {
                assertThrows(IllegalArgumentException.class,
                        () -> servicio.cambiarEstadoEmpleado(adminId, false));
                assertThrows(IllegalArgumentException.class,
                        () -> servicio.restablecerClaveEmpleado(adminId,
                                "OtraClaveTemporalCI2026!", "OtraClaveTemporalCI2026!"));
                try (PreparedStatement ps = conexion.prepareStatement(
                        "SELECT activo FROM usuarios WHERE id=? AND rol='ADMIN'")) {
                    ps.setInt(1, adminId);
                    try (ResultSet rs = ps.executeQuery()) {
                        assertTrue(rs.next());
                        assertTrue(rs.getBoolean("activo"));
                    }
                }
            } finally {
                try (PreparedStatement ps = conexion.prepareStatement(
                        "DELETE FROM usuarios WHERE usuario=? AND rol='ADMIN'")) {
                    ps.setString(1, admin);
                    ps.executeUpdate();
                }
            }
        }
    }

}
