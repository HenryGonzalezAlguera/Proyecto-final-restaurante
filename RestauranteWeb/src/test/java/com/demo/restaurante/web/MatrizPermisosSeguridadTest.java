package com.demo.restaurante.web;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.mockito.Mockito.when;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class MatrizPermisosSeguridadTest {
    @Autowired MockMvc mvc;
    @MockitoBean ServicioRestauranteWeb pedidos;
    @MockitoBean AdministracionEmpleadosService empleados;
    @MockitoBean AuditoriaRestauranteService auditoria;

    @BeforeEach
    void preparar() throws Exception {
        when(pedidos.listarPedidos()).thenReturn(List.of());
        when(pedidos.listarPlatillos()).thenReturn(List.of());
    }

    @Test
    @WithMockUser(roles = "EMPLEADO")
    void empleadoNoAccedeAAdministracionPorUrlNiPorPost() throws Exception {
        mvc.perform(get("/usuarios")).andExpect(status().isForbidden());
        mvc.perform(get("/auditoria")).andExpect(status().isForbidden());
        mvc.perform(get("/catalogo")).andExpect(status().isForbidden());
        mvc.perform(get("/api/platillos")).andExpect(status().isForbidden());
        mvc.perform(post("/usuarios/empleados").with(csrf())
                .param("usuario", "intruso").param("password", "SeguraSegura2026!"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/catalogo/1/disponibilidad").with(csrf())
                .param("accion", "desactivar"))
                .andExpect(status().isForbidden());
        mvc.perform(get("/pedidos")).andExpect(status().isOk());
        mvc.perform(get("/historial")).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void operacionesMutablesRequierenCsrf() throws Exception {
        mvc.perform(post("/usuarios/empleados/1/estado").param("accion", "desactivar"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/usuarios/empleados/1/clave")
                .param("password", "ClaveSeguraNueva2026!").param("confirmacion", "ClaveSeguraNueva2026!"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/catalogo/1/editar")
                .param("nombre", "Prueba").param("costo", "1").param("precio", "10"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/pedidos/siguiente"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/pedidos/1/estado")
                .contentType("application/json").content("{\"estado\":\"LISTO\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void respuestasIncluyenCabecerasContraClickjackingYMimesniffing() throws Exception {
        mvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Frame-Options", "DENY"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("Cache-Control",
                        org.hamcrest.Matchers.containsString("no-store")));
    }
}
