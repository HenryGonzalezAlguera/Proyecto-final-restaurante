package com.demo.restaurante.web;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class VistasRestauranteWebTest {
    @Autowired MockMvc mvc;
    @MockitoBean ServicioRestauranteWeb servicio;

    private Platillo platillo() {
        return new Platillo(1, "Arroz con Pollo", new BigDecimal("3000"), new BigDecimal("4000"));
    }

    private Pedido pedido(int id, EstadoPedido estado) {
        Pedido p = new Pedido(id, 101, List.of(new DetallePedido(platillo(), 2)));
        return switch (estado) {
            case PENDIENTE -> p;
            case CANCELADO -> p.cambiarEstado(EstadoPedido.CANCELADO);
            case EN_PREPARACION -> p.cambiarEstado(EstadoPedido.EN_PREPARACION);
            case LISTO -> p.cambiarEstado(EstadoPedido.EN_PREPARACION).cambiarEstado(EstadoPedido.LISTO);
            case ENTREGADO -> p.cambiarEstado(EstadoPedido.EN_PREPARACION)
                    .cambiarEstado(EstadoPedido.LISTO).cambiarEstado(EstadoPedido.ENTREGADO);
        };
    }

    @Test
    void catalogoRenderizaTarjetasConPreciosYMárgenes() throws Exception {
        when(servicio.listarPlatillos()).thenReturn(List.of(platillo()));
        mvc.perform(get("/catalogo")).andExpect(status().isOk())
                .andExpect(view().name("catalogo"))
                .andExpect(content().string(containsString("Catálogo de platillos")))
                .andExpect(content().string(containsString("Arroz con Pollo")))
                .andExpect(content().string(containsString("1000.00")))
                .andExpect(content().string(containsString("Dashboard")));
    }

    @Test
    void historialMuestraDetallesYFiltraPorEstado() throws Exception {
        when(servicio.listarPedidos()).thenReturn(List.of(
                pedido(1, EstadoPedido.ENTREGADO), pedido(2, EstadoPedido.PENDIENTE)));
        mvc.perform(get("/historial").param("estado", "ENTREGADO"))
                .andExpect(status().isOk())
                .andExpect(view().name("historial"))
                .andExpect(content().string(containsString("Historial de pedidos")))
                .andExpect(content().string(containsString("Pedido #1")))
                .andExpect(content().string(not(containsString("Pedido #2"))))
                .andExpect(content().string(containsString("Arroz con Pollo")));
    }

    @Test
    void historialBuscaPorClienteYControlaFiltroInvalido() throws Exception {
        when(servicio.listarPedidos()).thenReturn(List.of(pedido(3, EstadoPedido.PENDIENTE)));
        mvc.perform(get("/historial").param("q", "101"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Pedido #3")));
        mvc.perform(get("/historial").param("estado", "INVALIDO"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("El filtro de estado no es válido.")));
    }

    @Test
    void dashboardPresentaIndicadoresSinConfundirCobros() throws Exception {
        when(servicio.listarPedidos()).thenReturn(List.of(
                pedido(1, EstadoPedido.ENTREGADO),
                pedido(2, EstadoPedido.PENDIENTE),
                pedido(3, EstadoPedido.CANCELADO)));
        mvc.perform(get("/dashboard")).andExpect(status().isOk())
                .andExpect(view().name("dashboard"))
                .andExpect(content().string(containsString("Dashboard del restaurante")))
                .andExpect(content().string(containsString("16000.00")))
                .andExpect(content().string(containsString("Arroz con Pollo")))
                .andExpect(content().string(containsString("no equivale a ingresos cobrados")));
    }

    @Test
    void lasVistasMuestranErrorSinExponerDatosSql() throws Exception {
        when(servicio.listarPlatillos()).thenThrow(new SQLException("password: SENSIBLE"));
        when(servicio.listarPedidos()).thenThrow(new SQLException("password: SENSIBLE"));
        for (String url : List.of("/catalogo", "/historial", "/dashboard")) {
            mvc.perform(get(url)).andExpect(status().isOk())
                    .andExpect(content().string(containsString("Comprueba la conexión")))
                    .andExpect(content().string(not(containsString("SENSIBLE"))));
        }
    }
}
