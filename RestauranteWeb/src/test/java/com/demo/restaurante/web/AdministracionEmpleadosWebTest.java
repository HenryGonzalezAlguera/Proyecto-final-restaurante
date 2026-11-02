package com.demo.restaurante.web;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class AdministracionEmpleadosWebTest {
    @Autowired MockMvc mvc;
    @MockitoBean AdministracionEmpleadosService empleados;
    @MockitoBean AuditoriaRestauranteService auditoria;

    @BeforeEach
    void configurar() throws Exception {
        when(empleados.listarEmpleados()).thenReturn(List.of(
                new AdministracionEmpleadosService.EmpleadoResumen(5, "empleado_ci", true)));
    }

    @Test
    void anonimoDebeIniciarSesion() throws Exception {
        mvc.perform(get("/usuarios")).andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    @WithMockUser(roles = "EMPLEADO")
    void empleadoNoPuedeConsultarNiCrearUsuarios() throws Exception {
        mvc.perform(get("/usuarios")).andExpect(status().isForbidden());
        mvc.perform(post("/usuarios/empleados").with(csrf())
                        .param("usuario", "nuevo_empleado")
                        .param("password", "ClaveTemporal12345!"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminPuedeVerCuentasSinExponerContraseñas() throws Exception {
        mvc.perform(get("/usuarios"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("empleado_ci")))
                .andExpect(content().string(containsString("name=\"_csrf\"")));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void csrfEsObligatorioParaCrearEmpleado() throws Exception {
        mvc.perform(post("/usuarios/empleados")
                        .param("usuario", "nuevo_empleado")
                        .param("password", "ClaveTemporal12345!"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminPuedeCrearEmpleadoConCsrf() throws Exception {
        mvc.perform(post("/usuarios/empleados").with(csrf())
                        .param("usuario", "nuevo_empleado")
                        .param("password", "ClaveTemporal12345!"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/usuarios"))
                .andExpect(flash().attribute("mensaje", containsString("creada correctamente")));
        verify(empleados).crearEmpleado(eq("nuevo_empleado"), eq("ClaveTemporal12345!"));
    }
    @Test
    @WithMockUser(roles = "EMPLEADO")
    void empleadoNoPuedeCambiarEstadosNiClaves() throws Exception {
        mvc.perform(post("/usuarios/empleados/5/estado").with(csrf())
                        .param("accion", "desactivar"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/usuarios/empleados/5/clave").with(csrf())
                        .param("password", "ClavePruebaSegura2026!")
                        .param("confirmacion", "ClavePruebaSegura2026!"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void estadoYClaveRequierenCsrf() throws Exception {
        mvc.perform(post("/usuarios/empleados/5/estado")
                        .param("accion", "desactivar"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/usuarios/empleados/5/clave")
                        .param("password", "ClavePruebaSegura2026!")
                        .param("confirmacion", "ClavePruebaSegura2026!"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminPuedeDesactivarYReactivarEmpleado() throws Exception {
        mvc.perform(post("/usuarios/empleados/5/estado").with(csrf())
                        .param("accion", "desactivar"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/usuarios"));
        mvc.perform(post("/usuarios/empleados/5/estado").with(csrf())
                        .param("accion", "activar"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/usuarios"));
        verify(empleados).cambiarEstadoEmpleado(5, false);
        verify(empleados).cambiarEstadoEmpleado(5, true);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminPuedeRestablecerClaveEmpleadoConConfirmacion() throws Exception {
        mvc.perform(post("/usuarios/empleados/5/clave").with(csrf())
                        .param("password", "ClavePruebaSegura2026!")
                        .param("confirmacion", "ClavePruebaSegura2026!"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/usuarios"));
        verify(empleados).restablecerClaveEmpleado(
                5, "ClavePruebaSegura2026!", "ClavePruebaSegura2026!");
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void rechazaAccionesDesconocidasSinModificarLaCuenta() throws Exception {
        mvc.perform(post("/usuarios/empleados/5/estado").with(csrf())
                        .param("accion", "eliminar"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("error", containsString("inválida")));
        org.mockito.Mockito.verify(empleados, org.mockito.Mockito.never())
                .cambiarEstadoEmpleado(eq(5), org.mockito.ArgumentMatchers.anyBoolean());
    }

}
