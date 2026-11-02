package com.demo.restaurante.web;

import com.demo.restaurante.EstadoPedido;
import com.demo.restaurante.Pedido;
import com.demo.restaurante.Platillo;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * API REST local con sesión y CSRF; no está preparada para despliegue público.
 */
@RestController
@RequestMapping("/api")
public class PedidosApiController {
    private final ServicioRestauranteWeb servicio;
    private final AuditoriaRestauranteService auditoria;

    public PedidosApiController(ServicioRestauranteWeb servicio,
                                AuditoriaRestauranteService auditoria) {
        this.servicio = servicio;
        this.auditoria = auditoria;
    }

    public record CrearPedido(Integer clienteId, Integer platilloId, Integer cantidad) { }
    public record CambioEstado(EstadoPedido estado) { }

    @GetMapping("/platillos")
    public List<Platillo> platillos() throws SQLException {
        return servicio.listarPlatillos();
    }

    @GetMapping("/pedidos")
    public List<Pedido> pedidos() throws SQLException {
        return servicio.listarPedidos();
    }

    @GetMapping("/pedidos/{id}")
    public Pedido pedido(@PathVariable int id) throws SQLException {
        return servicio.consultarPedido(id);
    }

    @PostMapping("/pedidos")
    public ResponseEntity<Pedido> crear(@RequestBody CrearPedido solicitud) throws SQLException {
        if (solicitud == null) {
            throw new IllegalArgumentException("El pedido es obligatorio.");
        }
        var pedido = servicio.registrarPedido(
                solicitud.clienteId(), solicitud.platilloId(), solicitud.cantidad());
        boolean guardado = auditoria.registrar(AuditoriaRestauranteService.Accion.PEDIDO_CREADO,
                String.valueOf(pedido.getId()), "PENDIENTE");
        return ResponseEntity.status(HttpStatus.CREATED)
                .header("X-Audit-Status", guardado ? "recorded" : "missing").body(pedido);
    }

    @PostMapping("/pedidos/{id}/estado")
    public Pedido estado(@PathVariable int id, @RequestBody CambioEstado solicitud)
            throws SQLException {
        if (solicitud == null || solicitud.estado() == null) {
            throw new IllegalArgumentException("El estado es obligatorio.");
        }
        Pedido pedido = servicio.cambiarEstado(id, solicitud.estado());
        auditoria.registrar(AuditoriaRestauranteService.Accion.PEDIDO_ESTADO,
                String.valueOf(id), pedido.getEstado().name());
        return pedido;
    }

    @PostMapping("/pedidos/siguiente")
    public ResponseEntity<Pedido> siguiente() throws SQLException {
        Optional<Pedido> siguiente = servicio.iniciarSiguiente();
        siguiente.ifPresent(p -> auditoria.registrar(
                AuditoriaRestauranteService.Accion.PEDIDO_PREPARACION,
                String.valueOf(p.getId()), "EN_PREPARACION"));
        return siguiente.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }
}
