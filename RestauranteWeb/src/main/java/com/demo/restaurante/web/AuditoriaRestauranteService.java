package com.demo.restaurante.web;

import com.demo.restaurante.ConexionRestaurante;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

/**
 * Registro POST-operación: se escribe después del cambio confirmado.
 * No forma una transacción única con el repositorio de negocio existente.
 * En caso de indisponibilidad se avisa sin presentar la operación como fallida.
 */
@Service
public class AuditoriaRestauranteService {
    private static final Logger LOG = LoggerFactory.getLogger(AuditoriaRestauranteService.class);
    private static final int POR_PAGINA = 20;

    public enum Accion {
        PEDIDO_CREADO("PEDIDO"),
        PEDIDO_ESTADO("PEDIDO"),
        PEDIDO_PREPARACION("PEDIDO"),
        PLATILLO_CREADO("PLATILLO"),
        PLATILLO_EDITADO("PLATILLO"),
        PLATILLO_DISPONIBILIDAD("PLATILLO"),
        EMPLEADO_CREADO("EMPLEADO"),
        EMPLEADO_ESTADO("EMPLEADO"),
        EMPLEADO_CLAVE("EMPLEADO");

        private final String entidad;
        Accion(String entidad) { this.entidad = entidad; }
        public String entidad() { return entidad; }
    }

    public record Evento(long id, LocalDateTime fecha, String actor, String accion,
                         String entidad, String referencia, String detalle) { }
    public record Pagina(List<Evento> eventos, int numero, long total,
                         boolean anterior, boolean siguiente) { }

    /**
     * No expone datos de excepciones SQL ni los secretos al navegador.
     * Los únicos valores aceptados son enums y referencias cortas.
     * @return true cuando el registro fue persistido correctamente
     */
    public boolean registrar(Accion accion, String referencia, String detalle) {
        if (accion == null || referencia == null
                || !referencia.matches("[a-zA-Z0-9_.-]{1,64}")
                || detalle == null || !detalle.matches("[A-Z_]{0,48}")) {
            throw new IllegalArgumentException("Evento de auditoría inválido.");
        }
        Authentication autenticacion = SecurityContextHolder.getContext().getAuthentication();
        if (autenticacion == null || !autenticacion.isAuthenticated()
                || "anonymousUser".equals(autenticacion.getPrincipal())) {
            throw new IllegalStateException("No hay usuario autenticado para auditoría.");
        }
        String actor = autenticacion.getName();
        if (actor == null || !actor.matches("[a-zA-Z0-9_.-]{1,64}")) {
            throw new IllegalStateException("Identidad inválida para auditoría.");
        }
        String sql = "INSERT INTO auditoria_operaciones "
                + "(actor, accion, entidad, referencia, detalle) VALUES (?, ?, ?, ?, ?)";
        try (Connection conexion = ConexionRestaurante.desdeEntorno();
             PreparedStatement ps = conexion.prepareStatement(sql)) {
            ps.setString(1, actor);
            ps.setString(2, accion.name());
            ps.setString(3, accion.entidad());
            ps.setString(4, referencia);
            ps.setString(5, detalle);
            return ps.executeUpdate() == 1;
        } catch (SQLException | IllegalStateException ex) {
            LOG.warn("No se pudo registrar un evento de auditoría. Verificar migración 05 y conexión.");
            return false;
        }
    }

    public Pagina consultar(Integer solicitado) throws SQLException {
        int pagina = solicitado == null ? 0 : Math.max(0, Math.min(solicitado, 10000));
        String totalSql = "SELECT COUNT(*) FROM auditoria_operaciones";
        String listaSql = "SELECT id, fecha, actor, accion, entidad, referencia, detalle "
                + "FROM auditoria_operaciones ORDER BY id DESC LIMIT ? OFFSET ?";
        try (Connection conexion = ConexionRestaurante.desdeEntorno()) {
            long total;
            try (PreparedStatement ps = conexion.prepareStatement(totalSql);
                 ResultSet rs = ps.executeQuery()) {
                rs.next();
                total = rs.getLong(1);
            }
            List<Evento> eventos = new ArrayList<>();
            try (PreparedStatement ps = conexion.prepareStatement(listaSql)) {
                ps.setInt(1, POR_PAGINA);
                ps.setInt(2, pagina * POR_PAGINA);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        Timestamp fecha = rs.getTimestamp("fecha");
                        eventos.add(new Evento(rs.getLong("id"), fecha.toLocalDateTime(),
                                rs.getString("actor"), rs.getString("accion"),
                                rs.getString("entidad"), rs.getString("referencia"),
                                rs.getString("detalle")));
                    }
                }
            }
            return new Pagina(List.copyOf(eventos), pagina, total, pagina > 0,
                    (long) (pagina + 1) * POR_PAGINA < total);
        }
    }
}
