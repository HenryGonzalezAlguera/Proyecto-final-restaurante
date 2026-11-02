package com.demo.restaurante.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Valida la cuenta almacenada antes de cada petición autenticada.
 * Desactivar o cambiar el hash de una clave revoca sesiones anteriores
 * en la siguiente petición, incluido acceso a la API.
 * Prototipo local: realiza una consulta MySQL por petición autenticada.
 */
public final class VigenciaSesionRestauranteFiltro extends OncePerRequestFilter {
    private final UsuariosRestauranteService usuarios;

    public VigenciaSesionRestauranteFiltro(UsuariosRestauranteService usuarios) {
        this.usuarios = usuarios;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain)
            throws ServletException, IOException {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()
                && auth.getPrincipal() instanceof UsuarioRestaurantePrincipal original) {
            try {
                UserDetails actual = usuarios.loadUserByUsername(original.getUsername());
                if (!actual.isEnabled()
                        || !original.getPassword().equals(actual.getPassword())
                        || !original.getAuthorities().equals(actual.getAuthorities())) {
                    revocar(request, response);
                    return;
                }
            } catch (UsernameNotFoundException ex) {
                revocar(request, response);
                return;
            } catch (AuthenticationServiceException ex) {
                // Si MySQL no responde, nunca continuamos con permisos obsoletos.
                response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
                return;
            }
        }
        chain.doFilter(request, response);
    }

    private static void revocar(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        SecurityContextHolder.clearContext();
        var sesion = request.getSession(false);
        if (sesion != null) {
            sesion.invalidate();
        }
        String uri = request.getRequestURI().substring(request.getContextPath().length());
        if (uri.startsWith("/api/")) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
        } else {
            response.sendRedirect(request.getContextPath() + "/login?sesion=revocada");
        }
    }
}
