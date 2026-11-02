package com.demo.restaurante.web;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

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
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Prueba de login real contra la base temporal del CI. Jamás utiliza ni
 * modifica la base local restaurante_portafolio del desarrollador.
 */
@SpringBootTest
@AutoConfigureMockMvc
@EnabledIfEnvironmentVariable(named = "RESTAURANTE_SECURITY_TESTS", matches = "true")
class UsuariosMysqlSeguridadTest {
    @Autowired MockMvc mvc;
    @Autowired PasswordEncoder encoder;
    @Autowired UsuariosRestauranteService usuarios;

    @BeforeAll
    static void crearTablaAislada() throws Exception {
        try (Connection conexion = ConexionRestaurante.desdeEntorno()) {
            if (!"restaurante_ci".equals(conexion.getCatalog())) {
                throw new IllegalStateException("Pruebas de login solo permitidas en restaurante_ci");
            }
            String texto = Files.readString(Path.of("..", "database", "04_usuarios_seguridad.sql"))
                    .replaceAll("(?m)^\\s*--[^\\r\\n]*(?:\\r?\\n|$)", "");
            try (Statement stmt = conexion.createStatement()) {
                for (String parte : texto.split(";")) {
                    String sql = parte.strip();
                    if (!sql.isEmpty() && !sql.regionMatches(true, 0, "USE ", 0, 4)) {
                        stmt.execute(sql);
                    }
                }
            }
        }
    }

    @Test
    void usuariosPersistenHashBcryptYLoginValidaClaveYRol() throws Exception {
        String usuario = "seguridad_ci_admin";
        String clave = "ClavePruebaTemporalCI2026!";
        String hash = encoder.encode(clave);

        try (Connection conexion = ConexionRestaurante.desdeEntorno();
             PreparedStatement ps = conexion.prepareStatement(
                     "INSERT INTO usuarios(usuario, password_hash, rol, activo) "
                             + "VALUES (?, ?, 'ADMIN', TRUE) ON DUPLICATE KEY UPDATE "
                             + "password_hash=VALUES(password_hash), rol='ADMIN', activo=TRUE")) {
            if (!"restaurante_ci".equals(conexion.getCatalog())) {
                throw new IllegalStateException("Base incorrecta para pruebas de seguridad.");
            }
            ps.setString(1, usuario);
            ps.setString(2, hash);
            ps.executeUpdate();
        }
        assertTrue(encoder.matches(clave, usuarios.loadUserByUsername(usuario).getPassword()));
        assertEquals("ROLE_ADMIN", usuarios.loadUserByUsername(usuario)
                .getAuthorities().iterator().next().getAuthority());

        mvc.perform(post("/login").with(csrf())
                        .param("username", usuario)
                        .param("password", clave))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"));

        mvc.perform(post("/login").with(csrf())
                        .param("username", usuario)
                        .param("password", "contraseña-incorrecta"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?error"));

        try (Connection conexion = ConexionRestaurante.desdeEntorno();
             PreparedStatement ps = conexion.prepareStatement(
                     "SELECT password_hash FROM usuarios WHERE usuario=?")) {
            ps.setString(1, usuario);
            try (ResultSet rs = ps.executeQuery()) {
                assertTrue(rs.next());
                assertNotEquals(clave, rs.getString(1));
                assertTrue(rs.getString(1).startsWith("$2"));
            }
        }
    }
}
