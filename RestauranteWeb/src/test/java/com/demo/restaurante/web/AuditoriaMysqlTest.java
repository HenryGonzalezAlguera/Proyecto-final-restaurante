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
import org.springframework.security.test.context.support.WithMockUser;

/**
 * Solo utiliza restaurante_ci para evitar escrituras en la base local del usuario.
 */
@SpringBootTest
@EnabledIfEnvironmentVariable(named = "RESTAURANTE_SECURITY_TESTS", matches = "true")
class AuditoriaMysqlTest {
    private static final String ACTOR = "auditoria_ci";

    @Autowired AuditoriaRestauranteService auditoria;

    @BeforeAll
    static void prepararTabla() throws Exception {
        try (Connection conexion = ConexionRestaurante.desdeEntorno()) {
            exigirCi(conexion);
            String script = Files.readString(Path.of("..", "database",
                    "05_auditoria_operaciones.sql"))
                    .replaceAll("(?m)^\\s*--[^\\r\\n]*(?:\\r?\\n|$)", "");
            try (Statement stmt = conexion.createStatement()) {
                for (String bloque : script.split(";")) {
                    String sql = bloque.strip();
                    if (!sql.isEmpty() && !sql.regionMatches(true, 0, "USE ", 0, 4)) {
                        stmt.execute(sql);
                    }
                }
            }
        }
    }

    @Test
    @WithMockUser(username = ACTOR, roles = "ADMIN")
    void persisteEventosConIdentidadYNoGuardaSecretos() throws Exception {
        try (Connection conexion = ConexionRestaurante.desdeEntorno()) {
            exigirCi(conexion);
            borrarPruebas(conexion);
            try {
                assertTrue(auditoria.registrar(
                        AuditoriaRestauranteService.Accion.PEDIDO_CREADO, "56", "PENDIENTE"));
                try (PreparedStatement ps = conexion.prepareStatement(
                        "SELECT actor, accion, entidad, referencia, detalle, fecha "
                                + "FROM auditoria_operaciones WHERE actor=? ORDER BY id DESC LIMIT 1")) {
                    ps.setString(1, ACTOR);
                    try (ResultSet rs = ps.executeQuery()) {
                        assertTrue(rs.next());
                        assertEquals(ACTOR, rs.getString("actor"));
                        assertEquals("PEDIDO_CREADO", rs.getString("accion"));
                        assertEquals("PEDIDO", rs.getString("entidad"));
                        assertEquals("56", rs.getString("referencia"));
                        assertEquals("PENDIENTE", rs.getString("detalle"));
                        assertNotNull(rs.getTimestamp("fecha"));
                    }
                }
                assertTrue(auditoria.consultar(0).total() >= 1);
            } finally {
                borrarPruebas(conexion);
            }
        }
    }

    @Test
    @WithMockUser(username = ACTOR, roles = "ADMIN")
    void validaReferenciasAntesDeIntentarEscribir() {
        assertThrows(IllegalArgumentException.class,
                () -> auditoria.registrar(
                        AuditoriaRestauranteService.Accion.EMPLEADO_CLAVE,
                        "id\npassword=secreto", ""));
    }

    private static void exigirCi(Connection conexion) throws Exception {
        if (!"restaurante_ci".equals(conexion.getCatalog())) {
            throw new IllegalStateException("Auditoría de integración solo en restaurante_ci.");
        }
    }

    private static void borrarPruebas(Connection conexion) throws Exception {
        try (PreparedStatement ps = conexion.prepareStatement(
                "DELETE FROM auditoria_operaciones WHERE actor=?")) {
            ps.setString(1, ACTOR);
            ps.executeUpdate();
        }
    }
}
