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
}
