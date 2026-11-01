package com.demo.restaurante.web;

import com.demo.restaurante.EstadoPedido;
import java.sql.SQLException;
import java.util.List;
import java.util.NoSuchElementException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** UI de demostración local (sin autenticación: no exponer a Internet). */
@Controller
public class PedidosWebController {
    private final ServicioRestauranteWeb servicio;

    public PedidosWebController(ServicioRestauranteWeb servicio) {
        this.servicio = servicio;
    }

    @GetMapping("/")
    public String raiz() {
        return "redirect:/pedidos";
    }

    @GetMapping("/pedidos")
    public String listado(Model modelo) {
        try {
            var platillos = servicio.listarPlatillos();
            var pedidos = servicio.listarPedidos();
            modelo.addAttribute("platillos", platillos);
            modelo.addAttribute("pedidos", pedidos);
            modelo.addAttribute("pendientes", pedidos.stream()
                    .filter(p -> p.getEstado() == EstadoPedido.PENDIENTE).count());
            modelo.addAttribute("entregados", pedidos.stream()
                    .filter(p -> p.getEstado() == EstadoPedido.ENTREGADO).count());
        } catch (SQLException | IllegalStateException ex) {
            modelo.addAttribute("platillos", List.of());
            modelo.addAttribute("pedidos", List.of());
            modelo.addAttribute("pendientes", 0L);
            modelo.addAttribute("entregados", 0L);
            modelo.addAttribute("errorConexion",
                    "No se pudo conectar a la base Restaurante. Revisa las variables RESTAURANTE_DB_* y el esquema MySQL.");
        }
        return "pedidos";
    }

    @PostMapping("/pedidos")
    public String crear(@RequestParam(required = false) Integer clienteId,
                        @RequestParam(required = false) Integer platilloId,
                        @RequestParam(required = false) Integer cantidad,
                        RedirectAttributes mensajes) {
        try {
            var pedido = servicio.registrarPedido(clienteId, platilloId, cantidad);
            mensajes.addFlashAttribute("mensaje", "Pedido #" + pedido.getId() + " registrado.");
        } catch (SQLException | RuntimeException ex) {
            mensajes.addFlashAttribute("error", mensajePublico(ex));
        }
        return "redirect:/pedidos";
    }

    @PostMapping("/pedidos/{id}/estado")
    public String estado(@PathVariable int id, @RequestParam(required = false) EstadoPedido estado,
                         RedirectAttributes mensajes) {
        try {
            if (estado == null) {
                throw new IllegalArgumentException("Selecciona un estado.");
            }
            var pedido = servicio.cambiarEstado(id, estado);
            mensajes.addFlashAttribute("mensaje", "Pedido #" + id + " → " + pedido.getEstado());
        } catch (SQLException | RuntimeException ex) {
            mensajes.addFlashAttribute("error", mensajePublico(ex));
        }
        return "redirect:/pedidos";
    }

    @PostMapping("/pedidos/siguiente")
    public String siguiente(RedirectAttributes mensajes) {
        try {
            var pedido = servicio.iniciarSiguiente();
            mensajes.addFlashAttribute("mensaje", pedido
                    .map(p -> "Pedido #" + p.getId() + " enviado a preparación.")
                    .orElse("No hay pedidos pendientes."));
        } catch (SQLException | RuntimeException ex) {
            mensajes.addFlashAttribute("error", mensajePublico(ex));
        }
        return "redirect:/pedidos";
    }

    private static String mensajePublico(Exception ex) {
        if (ex instanceof SQLException || (ex instanceof IllegalStateException
                && ex.getMessage() != null && ex.getMessage().contains("RESTAURANTE_DB_"))) {
            return "No fue posible completar la operación. Revisa la conexión MySQL.";
        }
        if (ex instanceof NoSuchElementException) {
            return "No se encontró el pedido o el platillo solicitado.";
        }
        if (ex instanceof IllegalArgumentException || ex instanceof IllegalStateException) {
            return ex.getMessage();
        }
        return "No se pudo completar la operación.";
    }
}
