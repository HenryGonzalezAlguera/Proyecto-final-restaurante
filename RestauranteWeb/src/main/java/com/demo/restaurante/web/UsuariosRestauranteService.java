package com.demo.restaurante.web;

import com.demo.restaurante.ConexionRestaurante;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Lee SOLO contraseñas BCrypt almacenadas en restaurante_portafolio.usuarios.
 * Nunca usa contraseñas de root ni almacena claves en el repositorio Git.
 */
@Service
public class UsuariosRestauranteService implements UserDetailsService {
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        if (username == null || username.isBlank()) {
            throw new UsernameNotFoundException("Credenciales inválidas.");
        }
        try (Connection conexion = ConexionRestaurante.desdeEntorno();
             PreparedStatement ps = conexion.prepareStatement(
                     "SELECT usuario, password_hash, rol, activo FROM usuarios WHERE usuario = ?")) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new UsernameNotFoundException("Credenciales inválidas.");
                }
                return User.withUsername(rs.getString("usuario"))
                        .password(rs.getString("password_hash"))
                        .roles(rs.getString("rol"))
                        .disabled(!rs.getBoolean("activo"))
                        .build();
            }
        } catch (SQLException ex) {
            throw new AuthenticationServiceException(
                    "No se pudo acceder al almacén de usuarios.", ex);
        }
    }
}
