package com.demo.restaurante.web;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.access.intercept.AuthorizationFilter;

/**
 * Toda la aplicación usa sesiones autenticadas y CSRF habilitado.
 * ADMIN administra el catálogo; EMPLEADO trabaja con pedidos.
 * El acceso público sigue deshabilitado hasta pruebas de despliegue.
 */
@Configuration
public class SeguridadRestauranteConfiguracion {
    private final UsuariosRestauranteService usuarios;
    private final ControlIntentosLogin intentos;

    public SeguridadRestauranteConfiguracion(UsuariosRestauranteService usuarios,
                                              ControlIntentosLogin intentos) {
        this.usuarios = usuarios;
        this.intentos = intentos;
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    SecurityFilterChain seguridad(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(autorizacion -> autorizacion
                .requestMatchers("/login", "/css/**", "/favicon.ico", "/error").permitAll()
                .requestMatchers("/acceso-denegado").authenticated()
                .requestMatchers("/catalogo", "/catalogo/**").hasRole("ADMIN")
                .requestMatchers("/usuarios", "/usuarios/**").hasRole("ADMIN")
                .requestMatchers("/auditoria", "/auditoria/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/platillos").hasRole("ADMIN")
                .anyRequest().authenticated()
        );
        // El CSRF de Spring Security permanece habilitado para web y API.
        http.formLogin(formulario -> formulario
                .loginPage("/login")
                .loginProcessingUrl("/login")
                .successHandler((request, response, authentication) -> {
                    intentos.limpiar(request.getRemoteAddr());
                    response.sendRedirect(request.getContextPath() + "/dashboard");
                })
                .failureHandler((request, response, exception) -> {
                    intentos.registrarFallo(request.getRemoteAddr());
                    response.sendRedirect(request.getContextPath() + "/login?error");
                })
                .permitAll()
        );
        http.logout(salida -> salida
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
                .permitAll()
        );
        http.exceptionHandling(ex -> ex.accessDeniedPage("/acceso-denegado"));
        http.addFilterBefore(new VigenciaSesionRestauranteFiltro(usuarios), AuthorizationFilter.class);
        http.addFilterBefore(new FiltroLimiteIntentosLogin(intentos),
                UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
