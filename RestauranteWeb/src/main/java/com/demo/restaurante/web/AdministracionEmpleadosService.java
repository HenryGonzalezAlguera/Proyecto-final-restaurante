package com.demo.restaurante.web;

import com.demo.restaurante.ConexionRestaurante;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Fase 9A: alta y consulta de cuentas EMPLEADO.
 * No permite modificar, crear ni borrar cuentas ADMIN desde la interfaz.
 */
@Service
public class AdministracionEmpleadosService {
    public record EmpleadoResumen(int id, String usuario, boolean activo) {}

    private final PasswordEncoder encoder;

    public AdministracionEmpleadosService(PasswordEncoder encoder) {
        this.encoder = encoder;
    }

    public List<EmpleadoResumen> listarEmpleados() throws SQLException {
        String sql = "SELECT id, usuario, activo FROM usuarios "
                + "WHERE rol = 'EMPLEADO' ORDER BY id DESC";
        List<EmpleadoResumen> empleados = new ArrayList<>();
        try (Connection conexion = ConexionRestaurante.desdeEntorno();
             PreparedStatement ps = conexion.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                empleados.add(new EmpleadoResumen(
                        rs.getInt("id"), rs.getString("usuario"),
                        rs.getBoolean("activo")));
            }
        }
        return empleados;
    }

    public void crearEmpleado(String usuario, String password) throws SQLException {
        if (usuario == null || !usuario.trim().matches("[a-zA-Z0-9_.-]{3,64}")) {
            throw new IllegalArgumentException(
                    "El usuario debe tener entre 3 y 64 caracteres: letras, números, punto, guion o guion bajo.");
        }
        if (password == null || password.length() < 12
                || password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new IllegalArgumentException(
                    "La contraseña debe tener al menos 12 caracteres y no superar 72 bytes UTF-8.");
        }
        String sql = "INSERT INTO usuarios (usuario, password_hash, rol, activo) "
                + "VALUES (?, ?, 'EMPLEADO', TRUE)";
        try (Connection conexion = ConexionRestaurante.desdeEntorno();
             PreparedStatement ps = conexion.prepareStatement(sql)) {
            ps.setString(1, usuario.trim());
            ps.setString(2, encoder.encode(password));
            ps.executeUpdate();
        } catch (SQLException ex) {
            if (ex.getErrorCode() == 1062) {
                throw new IllegalArgumentException("Ese nombre de usuario ya está registrado.");
            }
            throw ex;
        }
    }
}
