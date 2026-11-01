package com.demo.restaurante.web;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.demo.restaurante.DetallePedido;
import com.demo.restaurante.EstadoPedido;
import com.demo.restaurante.Pedido;
import com.demo.restaurante.Platillo;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class PedidosWebTest {
    @Autowired
    MockMvc mvc;

    @MockitoBean
    ServicioRestauranteWeb servicio;

    private Platillo platillo() {
        return new Platillo(1, "Casado", new BigDecimal("2000"), new BigDecimal("3500"));
    }

    @Test
    void redirigeRaizAlPanel() throws Exception {
        mvc.perform(get("/")).andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/pedidos"));
    }

    @Test
    void muestraPanelDePedidosConCatalogo() throws Exception {
        when(servicio.listarPlatillos()).thenReturn(List.of(platillo()));
        when(servicio.listarPedidos()).thenReturn(List.of());
        mvc.perform(get("/pedidos"))
                .andExpect(status().isOk())
                .andExpect(view().name("pedidos"))
                .andExpect(content().string(containsString("Panel de pedidos")))
                .andExpect(content().string(containsString("Casado")));
    }

    @Test
    void muestraErrorAmigableCuandoMySqlNoEstaDisponible() throws Exception {
        when(servicio.listarPlatillos()).thenThrow(new SQLException("detalle sensible"));
        mvc.perform(get("/pedidos"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("No se pudo conectar")))
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("detalle sensible"))));
    }

    @Test
    void validaCreacionDesdeFormulario() throws Exception {
        when(servicio.registrarPedido(5, 1, 2)).thenReturn(
                new Pedido(10, 5, List.of(new DetallePedido(platillo(), 2))));
        mvc.perform(post("/pedidos")
                .param("clienteId", "5").param("platilloId", "1").param("cantidad", "2"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/pedidos"))
                .andExpect(flash().attribute("mensaje", "Pedido #10 registrado."));
    }

    @Test
    void listaPedidosPorApiYRegistraUnPedido() throws Exception {
        when(servicio.listarPlatillos()).thenReturn(List.of(platillo()));
        when(servicio.listarPedidos()).thenReturn(List.of());
        when(servicio.registrarPedido(5, 1, 2)).thenReturn(
                new Pedido(10, 5, List.of(new DetallePedido(platillo(), 2))));
        mvc.perform(get("/api/platillos")).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nombre").value("Casado"));
        mvc.perform(get("/api/pedidos")).andExpect(status().isOk())
                .andExpect(content().json("[]"));
        mvc.perform(post("/api/pedidos").contentType(MediaType.APPLICATION_JSON)
                .content("{\"clienteId\":5,\"platilloId\":1,\"cantidad\":2}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.estado").value("PENDIENTE"));
    }

    @Test
    void apiRechazaEstadoFaltanteYExponeErrorControlado() throws Exception {
        mvc.perform(post("/api/pedidos/1/estado")
                .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("El estado es obligatorio."));
    }
}
