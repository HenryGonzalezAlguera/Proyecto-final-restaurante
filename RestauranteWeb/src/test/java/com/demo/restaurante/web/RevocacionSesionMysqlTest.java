package com.demo.restaurante.web;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.demo.restaurante.ConexionRestaurante;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

/** Prueba el ciclo de login y revocación con sesiones HTTP y MySQL efímero de CI. */
@SpringBootTest
@AutoConfigureMockMvc
@EnabledIfEnvironmentVariable(named = "RESTAURANTE_SECURITY_TESTS", matches = "true")
class RevocacionSesionMysqlTest {
    private static final String USUARIO = "empleado_revocacion_ci";
    @Autowired MockMvc mvc;
    @Autowired AdministracionEmpleadosService empleados;

    @BeforeAll
    static void inicializarTablaEnCi() throws Exception {
        try (Connection conexion = ConexionRestaurante.desdeEntorno()) {
            verificarBase(conexion);
            String script = Files.readString(
                    Path.of("..", "database", "04_usuarios_seguridad.sql"))
                    .replaceAll("(?m)^\\s*--[^\\r\\n]*(?:\\r?\\n|$)", "");
            try (Statement stmt = conexion.createStatement()) {
                for (String fragmento : script.split(";")) {
                    String sql = fragmento.strip();
                    if (!sql.isEmpty() && !sql.regionMatches(true, 0, "USE ", 0, 4)) {
                        stmt.execute(sql);
                    }
                }
            }
        }
    }

    @Test
    void desactivarEmpleadoRevocaSesionEnSiguientePeticion() throws Exception {
        String clave = "ClaveSeguraEmpleadoCI2026!";
        try (Connection conexion = ConexionRestaurante.desdeEntorno()) {
            verificarBase(conexion);
            borrar(conexion);
            try {
                empleados.crearEmpleado(USUARIO, clave);
                int id = buscarId();
                MockHttpSession sesion = iniciarSesion(clave);
                mvc.perform(get("/pedidos").session(sesion)).andExpect(status().isOk());
                empleados.cambiarEstadoEmpleado(id, false);
                mvc.perform(get("/pedidos").session(sesion))
                        .andExpect(status().is3xxRedirection())
                        .andExpect(redirectedUrl("/login?sesion=revocada"));
                mvc.perform(post("/login").with(csrf())
                                .param("username", USUARIO)
                                .param("password", clave))
                        .andExpect(redirectedUrl("/login?error"));
            } finally {
                borrar(conexion);
            }
        }
    }

    @Test
    void cambiarClaveRevocaSesionAnteriorYRequiereClaveNueva() throws Exception {
        String anterior = "ClaveInicialEmpleadoCI2026!";
        String nueva = "ClaveNuevaEmpleadoCI2026!";
        try (Connection conexion = ConexionRestaurante.desdeEntorno()) {
            verificarBase(conexion);
            borrar(conexion);
            try {
                empleados.crearEmpleado(USUARIO, anterior);
                int id = buscarId();
                MockHttpSession sesion = iniciarSesion(anterior);
                empleados.restablecerClaveEmpleado(id, nueva, nueva);
                mvc.perform(get("/pedidos").session(sesion))
                        .andExpect(status().is3xxRedirection())
                        .andExpect(redirectedUrl("/login?sesion=revocada"));
                mvc.perform(post("/login").with(csrf())
                                .param("username", USUARIO)
                                .param("password", anterior))
                        .andExpect(redirectedUrl("/login?error"));
                iniciarSesion(nueva);
            } finally {
                borrar(conexion);
            }
        }
    }

    private MockHttpSession iniciarSesion(String clave) throws Exception {
        var resultado = mvc.perform(post("/login").with(csrf())
                        .param("username", USUARIO).param("password", clave))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"))
                .andReturn();
        var sesion = resultado.getRequest().getSession(false);
        assertNotNull(sesion, "El login debe crear una sesión HTTP.");
        return (MockHttpSession) sesion;
    }

    private int buscarId() throws Exception {
        return empleados.listarEmpleados().stream()
                .filter(e -> e.usuario().equals(USUARIO))
                .findFirst().orElseThrow().id();
    }

    private static void verificarBase(Connection conexion) throws Exception {
        if (!"restaurante_ci".equals(conexion.getCatalog())) {
            throw new IllegalStateException("Prohibido usar una base diferente de restaurante_ci.");
        }
    }

    private static void borrar(Connection conexion) throws Exception {
        try (PreparedStatement ps = conexion.prepareStatement(
                "DELETE FROM usuarios WHERE usuario=? AND rol='EMPLEADO'")) {
            ps.setString(1, USUARIO);
            ps.executeUpdate();
        }
    }
}
