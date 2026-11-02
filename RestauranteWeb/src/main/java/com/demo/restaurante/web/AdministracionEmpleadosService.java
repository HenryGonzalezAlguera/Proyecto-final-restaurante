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
 * Fases 9A/9B: alta, consulta, activación y restablecimiento de EMPLEADO.
 * Todas las modificaciones filtran por rol EMPLEADO: nunca modifica ADMIN.
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
        validarClave(password);
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
    public void cambiarEstadoEmpleado(int id, boolean activo) throws SQLException {
        validarId(id);
        String sql = "UPDATE usuarios SET activo = ? "
                + "WHERE id = ? AND rol = 'EMPLEADO' AND activo <> ?";
        try (Connection conexion = ConexionRestaurante.desdeEntorno();
             PreparedStatement ps = conexion.prepareStatement(sql)) {
            ps.setBoolean(1, activo);
            ps.setInt(2, id);
            ps.setBoolean(3, activo);
            if (ps.executeUpdate() != 1) {
                throw new IllegalArgumentException(
                        "No se encontró el empleado o su estado ya estaba actualizado.");
            }
        }
    }

    /**
     * La contraseña reemplazada invalida sesiones previas gracias al filtro
     * de vigencia; la cuenta no se activa automáticamente.
     */
    public void restablecerClaveEmpleado(int id, String nueva, String confirmacion)
            throws SQLException {
        validarId(id);
        validarClave(nueva);
        if (!nueva.equals(confirmacion)) {
            throw new IllegalArgumentException("Las contraseñas no coinciden.");
        }
        String sql = "UPDATE usuarios SET password_hash = ? "
                + "WHERE id = ? AND rol = 'EMPLEADO'";
        try (Connection conexion = ConexionRestaurante.desdeEntorno();
             PreparedStatement ps = conexion.prepareStatement(sql)) {
            ps.setString(1, encoder.encode(nueva));
            ps.setInt(2, id);
            if (ps.executeUpdate() != 1) {
                throw new IllegalArgumentException("No se encontró la cuenta de empleado.");
            }
        }
    }

    private static void validarId(int id) {
        if (id <= 0) {
            throw new IllegalArgumentException("Identificador de empleado inválido.");
        }
    }

    private static void validarClave(String password) {
        if (password == null || password.length() < 12
                || password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new IllegalArgumentException(
                    "La contraseña debe tener al menos 12 caracteres y no superar 72 bytes UTF-8.");
        }
    }

}
