package com.demo.restaurante.web;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class LimiteLoginSeguridadTest {
    @Autowired MockMvc mvc;
    @Autowired PasswordEncoder encoder;
    @MockitoBean UsuariosRestauranteService usuarios;

    @Test
    void quintoFalloBloqueaNuevoIntentoSoloParaLaIpAfectada() throws Exception {
        String cuenta = "cuenta_qa";
        String clave = "ClaveSeguraParaPrueba2026!";
        String hash = encoder.encode(clave);
        when(usuarios.loadUserByUsername(cuenta))
                .thenAnswer(inv -> new UsuarioRestaurantePrincipal(
                        cuenta, hash, "EMPLEADO", true));
        for (int i = 0; i < 5; i++) {
            mvc.perform(post("/login").with(csrf())
                            .with(r -> { r.setRemoteAddr("192.0.2.77"); return r; })
                            .param("username", cuenta).param("password", "Incorrecta2026"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/login?error"));
        }
        clearInvocations(usuarios);
        mvc.perform(post("/login").with(csrf())
                        .with(r -> { r.setRemoteAddr("192.0.2.77"); return r; })
                        .param("username", cuenta).param("password", clave))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?bloqueado"));
        verifyNoInteractions(usuarios);
        mvc.perform(get("/login").param("bloqueado", ""))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("15 minutos")));

        mvc.perform(post("/login").with(csrf())
                        .with(r -> { r.setRemoteAddr("192.0.2.78"); return r; })
                        .param("username", cuenta).param("password", clave))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"));
    }

    @Test
    void loginCorrectoReiniciaElContador() throws Exception {
        String usuario = "cuenta_exito_qa";
        String clave = "ContrasenaCorrecta2026!";
        String hash = encoder.encode(clave);
        when(usuarios.loadUserByUsername(usuario))
                .thenAnswer(inv -> new UsuarioRestaurantePrincipal(
                        usuario, hash, "ADMIN", true));
        for (int i = 0; i < 4; i++) {
            mvc.perform(post("/login").with(csrf())
                            .with(r -> { r.setRemoteAddr("198.51.100.91"); return r; })
                            .param("username", usuario).param("password", "clave_mala"))
                    .andExpect(redirectedUrl("/login?error"));
        }
        mvc.perform(post("/login").with(csrf())
                        .with(r -> { r.setRemoteAddr("198.51.100.91"); return r; })
                        .param("username", usuario).param("password", clave))
                .andExpect(redirectedUrl("/dashboard"));
        mvc.perform(post("/login").with(csrf())
                        .with(r -> { r.setRemoteAddr("198.51.100.91"); return r; })
                        .param("username", usuario).param("password", "clave_mala"))
                .andExpect(redirectedUrl("/login?error"));
    }

    @Test
    void sinTokenCsrfNoProcesaAutenticacionNiCuentaComoFallo() throws Exception {
        mvc.perform(post("/login")
                        .with(r -> { r.setRemoteAddr("203.0.113.42"); return r; })
                        .param("username", "cualquiera").param("password", "cualquiera"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(usuarios);
    }
}
