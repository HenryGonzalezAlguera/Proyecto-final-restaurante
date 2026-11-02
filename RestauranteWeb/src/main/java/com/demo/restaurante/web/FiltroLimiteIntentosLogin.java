package com.demo.restaurante.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.web.filter.OncePerRequestFilter;

/** Rechaza intentos de autenticación mientras la IP remota está bloqueada. */
public final class FiltroLimiteIntentosLogin extends OncePerRequestFilter {
    private final ControlIntentosLogin control;

    public FiltroLimiteIntentosLogin(ControlIntentosLogin control) {
        this.control = control;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest solicitud,
                                    HttpServletResponse respuesta,
                                    FilterChain cadena) throws ServletException, IOException {
        // En MockMvc getServletPath() puede venir vacío aunque la URI sea /login.
        // Usar la URI relativa al contexto mantiene el mismo comportamiento real.
        String ruta = solicitud.getRequestURI()
                .substring(solicitud.getContextPath().length());
        if (ruta.startsWith("/login;")) {
            ruta = "/login"; // rutas con parámetro de sesión URL
        }
        if ("POST".equals(solicitud.getMethod())
                && "/login".equals(ruta)
                && control.estaBloqueado(solicitud.getRemoteAddr())) {
            respuesta.sendRedirect(solicitud.getContextPath() + "/login?bloqueado");
            return;
        }
        cadena.doFilter(solicitud, respuesta);
    }
}
