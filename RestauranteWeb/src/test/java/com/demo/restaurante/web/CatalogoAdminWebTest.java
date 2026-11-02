package com.demo.restaurante.web;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.demo.restaurante.Platillo;
import com.demo.restaurante.RegistroPlatillo;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.security.test.context.support.WithMockUser;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@WithMockUser(roles = "ADMIN")
class CatalogoAdminWebTest {
    @Autowired MockMvc mvc;
    @MockitoBean ServicioRestauranteWeb servicio;

    @Test
    void nuevoPlatilloSeCreaConRedireccionYMensaje() throws Exception {
        Platillo creado = new Platillo(8, "Hamburguesa",
                new BigDecimal("1600"), new BigDecimal("3000"));
        when(servicio.crearPlatillo("Hamburguesa",
                new BigDecimal("1600.00"), new BigDecimal("3000.00")))
                .thenReturn(creado);

        mvc.perform(post("/catalogo/nuevo").with(csrf())
                .param("nombre", "Hamburguesa")
                .param("costo", "1600.00")
                .param("precio", "3000.00"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/catalogo"))
                .andExpect(flash().attribute("mensaje", "Platillo #8 agregado al catálogo."));
    }

    @Test
    void validacionRechazaMontosNegativosYCadenaInvalida() throws Exception {
        when(servicio.crearPlatillo("Malo", new BigDecimal("-1"),
                new BigDecimal("3000"))).thenThrow(new IllegalArgumentException("Costo inválido."));
        mvc.perform(post("/catalogo/nuevo").with(csrf()).param("nombre", "Malo")
                .param("costo", "-1").param("precio", "3000"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("error", "Costo inválido."));
        mvc.perform(post("/catalogo/nuevo").with(csrf()).param("nombre", "Malo")
                .param("costo", "incorrecto").param("precio", "3000"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("error", "Introduce montos numéricos válidos."));
    }

    @Test
    void editarYDesactivarNoEliminanRegistros() throws Exception {
        mvc.perform(post("/catalogo/2/editar").with(csrf())
                .param("nombre", "Casado")
                .param("costo", "1200")
                .param("precio", "3000"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("mensaje", "Platillo #2 actualizado."));
        verify(servicio).actualizarPlatillo(2, "Casado",
                new BigDecimal("1200"), new BigDecimal("3000"));

        mvc.perform(post("/catalogo/2/disponibilidad").with(csrf()).param("accion", "desactivar"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("mensaje", "Platillo #2 desactivado."));
        verify(servicio).cambiarDisponibilidadPlatillo(2, false);
        mvc.perform(post("/catalogo/2/disponibilidad").with(csrf()).param("accion", "activar"))
                .andExpect(flash().attribute("mensaje", "Platillo #2 activado."));
        verify(servicio).cambiarDisponibilidadPlatillo(2, true);
    }

    @Test
    void protegeMensajeSqlYRechazaAccionDesconocida() throws Exception {
        when(servicio.crearPlatillo("Test", BigDecimal.ONE, BigDecimal.TEN))
                .thenThrow(new SQLException("password sensitive"));
        mvc.perform(post("/catalogo/nuevo").with(csrf()).param("nombre", "Test")
                .param("costo", "1").param("precio", "10"))
                .andExpect(flash().attribute("error",
                        "No se pudo guardar el cambio en MySQL. Comprueba los permisos y que ejecutaste la migración 03 del catálogo."));
        mvc.perform(post("/catalogo/1/disponibilidad").with(csrf()).param("accion", "borrar"))
                .andExpect(flash().attribute("error", "Acción de disponibilidad inválida."));
    }

    @Test
    void catalogoIncluyeActivosEInactivosEnAdministracion() throws Exception {
        var activo = new RegistroPlatillo(new Platillo(1, "Casado", BigDecimal.ONE,
                BigDecimal.TEN), true);
        var inactivo = new RegistroPlatillo(new Platillo(2, "Pasta", BigDecimal.ONE,
                BigDecimal.TEN), false);
        when(servicio.listarCatalogoAdministracion()).thenReturn(List.of(activo, inactivo));

        mvc.perform(get("/catalogo")).andExpect(status().isOk())
                .andExpect(content().string(containsString("Agregar nuevo platillo")))
                .andExpect(content().string(containsString("Casado")))
                .andExpect(content().string(containsString("Pasta")))
                .andExpect(content().string(containsString("Desactivar")))
                .andExpect(content().string(containsString("Activar")))
                .andExpect(content().string(containsString("Editar información")));
    }
}
