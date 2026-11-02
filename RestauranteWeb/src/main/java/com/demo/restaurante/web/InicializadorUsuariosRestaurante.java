package com.demo.restaurante.web;

import com.demo.restaurante.ConexionRestaurante;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Crea cuentas iniciales solo si se suministran las variables locales.
 * No modifica contraseñas ni roles de cuentas que ya existan en MySQL.
 * Nunca escribe los secretos en logs.
 */
@Component
public class InicializadorUsuariosRestaurante implements ApplicationRunner {
    private final PasswordEncoder encoder;

    public InicializadorUsuariosRestaurante(PasswordEncoder encoder) {
        this.encoder = encoder;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        crearSiSeConfiguro("RESTAURANTE_ADMIN_USUARIO",
                "RESTAURANTE_ADMIN_PASSWORD", "admin", "ADMIN");
        crearSiSeConfiguro("RESTAURANTE_EMPLEADO_USUARIO",
                "RESTAURANTE_EMPLEADO_PASSWORD", "empleado", "EMPLEADO");
    }

    private void crearSiSeConfiguro(String variableUsuario, String variablePassword,
                                    String predeterminado, String rol) throws SQLException {
        String password = System.getenv(variablePassword);
        if (password == null || password.isBlank()) {
            return;
        }
        if (password.length() < 12) {
            throw new IllegalStateException(variablePassword + " debe tener al menos 12 caracteres.");
        }
        String usuario = System.getenv().getOrDefault(variableUsuario, predeterminado);
        if (usuario == null || !usuario.matches("[a-zA-Z0-9_.-]{3,64}")) {
            throw new IllegalStateException(variableUsuario + " debe contener 3 a 64 caracteres permitidos.");
        }
        String sql = "INSERT INTO usuarios (usuario, password_hash, rol, activo) "
                + "VALUES (?, ?, ?, TRUE) ON DUPLICATE KEY UPDATE usuario=usuario";
        try (Connection conexion = ConexionRestaurante.desdeEntorno();
             PreparedStatement ps = conexion.prepareStatement(sql)) {
            ps.setString(1, usuario);
            ps.setString(2, encoder.encode(password));
            ps.setString(3, rol);
            ps.executeUpdate();
        }
    }
}
