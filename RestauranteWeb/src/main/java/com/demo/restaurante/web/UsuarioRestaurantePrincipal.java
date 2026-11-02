package com.demo.restaurante.web;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.User;

/**
 * Identifica las sesiones autenticadas contra MySQL.
 * Spring Security elimina getPassword() después del login. La huella SHA-256
 * del hash BCrypt permite reconocer una contraseña reemplazada sin conservar
 * el propio hash BCrypt como contraseña de autenticación en la sesión.
 */
public final class UsuarioRestaurantePrincipal extends User {
    private static final long serialVersionUID = 1L;
    private final String huellaClave;

    public UsuarioRestaurantePrincipal(String usuario, String hash, String rol, boolean activo) {
        super(usuario, hash, activo, true, true, true,
                AuthorityUtils.createAuthorityList("ROLE_" + rol));
        this.huellaClave = calcularHuella(hash);
    }

    public String getHuellaClave() {
        return huellaClave;
    }

    private static String calcularHuella(String hash) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(hash.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("No está disponible SHA-256.", ex);
        }
    }
}
