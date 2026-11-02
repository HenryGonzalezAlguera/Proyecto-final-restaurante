package com.demo.restaurante.web;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class SeguridadRestauranteWebTest {
    @Autowired MockMvc mvc;
    @Autowired PasswordEncoder encoder;

    @Test
    void loginPublicoYConFormularioCsrf() throws Exception {
        mvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Iniciar sesión")))
                .andExpect(content().string(containsString("name=\"_csrf\"")));
    }

    @Test
    void anonimoNoPuedeEntrarAPedidosNiAPi() throws Exception {
        mvc.perform(get("/pedidos"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
        mvc.perform(get("/api/pedidos"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    @WithMockUser(roles = "EMPLEADO")
    void empleadoPuedeVerPedidosPeroNoAdministrarCatalogo() throws Exception {
        mvc.perform(get("/pedidos")).andExpect(status().isOk());
        mvc.perform(get("/catalogo")).andExpect(status().isForbidden());
        mvc.perform(get("/api/platillos")).andExpect(status().isForbidden());
        mvc.perform(post("/catalogo/nuevo").with(csrf())
                .param("nombre", "Fraude")
                .param("costo", "1")
                .param("precio", "10"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminPuedeVerCatalogoYAccederAAdministracion() throws Exception {
        mvc.perform(get("/catalogo")).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void csrfObligatorioParaCrearPedidosYEditarCatalogoYApi() throws Exception {
        mvc.perform(post("/pedidos")
                .param("clienteId", "1").param("platilloId", "1").param("cantidad", "1"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/catalogo/nuevo")
                .param("nombre", "Prohibido").param("costo", "1").param("precio", "10"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/pedidos")
                .contentType("application/json")
                .content("{\"clienteId\":1,\"platilloId\":1,\"cantidad\":1}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void bcryptNuncaAlmacenaLaClaveEnTextoClaro() {
        String password = "PruebaSeguraLocal2026!";
        String hash = encoder.encode(password);
        assertNotEquals(password, hash);
        assertTrue(hash.startsWith("$2"));
        assertTrue(encoder.matches(password, hash));
        assertFalse(encoder.matches("otra-contraseña", hash));
    }
}
