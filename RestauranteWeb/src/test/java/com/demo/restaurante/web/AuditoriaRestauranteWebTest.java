package com.demo.restaurante.web;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

import com.demo.restaurante.DetallePedido;
import com.demo.restaurante.Pedido;
import com.demo.restaurante.Platillo;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class AuditoriaRestauranteWebTest {
    @Autowired MockMvc mvc;
    @MockitoBean AuditoriaRestauranteService auditoria;
    @MockitoBean ServicioRestauranteWeb pedidos;

    @Test
    void anonimoDebeAutenticarse() throws Exception {
        mvc.perform(get("/auditoria"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    @WithMockUser(roles = "EMPLEADO")
    void empleadoNoPuedeConsultarLaAuditoria() throws Exception {
        mvc.perform(get("/auditoria")).andExpect(status().isForbidden());
        verifyNoInteractions(auditoria);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void administradorConsultaEventosPaginadosSinDatosSecretos() throws Exception {
        when(auditoria.consultar(1)).thenReturn(new AuditoriaRestauranteService.Pagina(
                List.of(new AuditoriaRestauranteService.Evento(5,
                        LocalDateTime.of(2026, 11, 2, 10, 30),
                        "empleado_ci", "PEDIDO_CREADO", "PEDIDO", "75", "PENDIENTE")),
                1, 45, true, true));
        mvc.perform(get("/auditoria").param("pagina", "1"))
                .andExpect(status().isOk())
                .andExpect(view().name("auditoria"))
                .andExpect(content().string(containsString("empleado_ci")))
                .andExpect(content().string(containsString("PEDIDO_CREADO")))
                .andExpect(content().string(containsString("Página 2")))
                .andExpect(content().string(not(containsString("password_hash"))));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void errorSqlDeConsultaNoSeExpone() throws Exception {
        when(auditoria.consultar(null)).thenThrow(new SQLException("CLAVE_OCULTA"));
        mvc.perform(get("/auditoria"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("migración 05")))
                .andExpect(content().string(not(containsString("CLAVE_OCULTA"))));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void pedidoExitosoGeneraEventoYErrorNoGeneraEvento() throws Exception {
        var platillo = new Platillo(1, "Casado", BigDecimal.ONE, BigDecimal.TEN);
        var creado = new Pedido(75, 5, List.of(new DetallePedido(platillo, 2)));
        when(pedidos.registrarPedido(5, 1, 2)).thenReturn(creado);
        when(auditoria.registrar(AuditoriaRestauranteService.Accion.PEDIDO_CREADO,
                "75", "PENDIENTE")).thenReturn(true);
        mvc.perform(post("/pedidos").with(csrf()).param("clienteId", "5")
                        .param("platilloId", "1").param("cantidad", "2"))
                .andExpect(status().is3xxRedirection());
        verify(auditoria).registrar(AuditoriaRestauranteService.Accion.PEDIDO_CREADO,
                "75", "PENDIENTE");

        reset(auditoria);
        when(pedidos.registrarPedido(99, 1, 2)).thenThrow(
                new IllegalArgumentException("Pedido inválido"));
        mvc.perform(post("/pedidos").with(csrf()).param("clienteId", "99")
                        .param("platilloId", "1").param("cantidad", "2"))
                .andExpect(flash().attribute("error", "Pedido inválido"));
        verifyNoInteractions(auditoria);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void falloDeBitacoraNoAnulaPedidoYaConfirmado() throws Exception {
        var platillo = new Platillo(1, "Casado", BigDecimal.ONE, BigDecimal.TEN);
        when(pedidos.registrarPedido(5, 1, 2))
                .thenReturn(new Pedido(75, 5, List.of(new DetallePedido(platillo, 2))));
        when(auditoria.registrar(any(), any(), any())).thenReturn(false);
        mvc.perform(post("/pedidos").with(csrf()).param("clienteId", "5")
                        .param("platilloId", "1").param("cantidad", "2"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("mensaje", "Pedido #75 registrado."))
                .andExpect(flash().attribute("advertenciaAuditoria",
                        "El cambio se realizó, pero no se pudo guardar la auditoría."));
    }
}
