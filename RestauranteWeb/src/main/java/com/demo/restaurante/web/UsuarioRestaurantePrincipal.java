package com.demo.restaurante.web;

import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.User;

/**
 * Identifica las sesiones autenticadas contra la tabla usuarios.
 * Las identidades simuladas por tests de seguridad permanecen independientes.
 */
public final class UsuarioRestaurantePrincipal extends User {
    private static final long serialVersionUID = 1L;

    public UsuarioRestaurantePrincipal(String usuario, String hash, String rol, boolean activo) {
        super(usuario, hash, activo, true, true, true,
                AuthorityUtils.createAuthorityList("ROLE_" + rol));
    }
}
