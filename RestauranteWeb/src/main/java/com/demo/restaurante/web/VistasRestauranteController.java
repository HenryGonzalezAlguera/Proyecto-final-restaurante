package com.demo.restaurante.web;

import com.demo.restaurante.Pedido;
import java.sql.SQLException;
import java.util.List;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Pantallas de consulta del prototipo local. No expone información nueva
 * ni modifica los pedidos. Las APIs JSON continúan disponibles por separado.
 */
@Controller
public class VistasRestauranteController {
    private static final String ERROR_CONEXION =
            "No se pudo consultar Restaurante. Comprueba la conexión y los permisos de MySQL.";

    private final ServicioRestauranteWeb servicio;
    private final AnaliticaRestauranteWeb analitica;

    public VistasRestauranteController(ServicioRestauranteWeb servicio,
                                      AnaliticaRestauranteWeb analitica) {
        this.servicio = servicio;
        this.analitica = analitica;
    }

    @GetMapping("/catalogo")
    public String catalogo(Model modelo) {
        try {
            var platillos = servicio.listarPlatillos();
            modelo.addAttribute("platillos", platillos);
        } catch (SQLException | IllegalStateException ex) {
            modelo.addAttribute("platillos", List.of());
            modelo.addAttribute("errorConexion", ERROR_CONEXION);
        }
        return "catalogo";
    }

    @GetMapping("/historial")
    public String historial(@RequestParam(required = false, defaultValue = "") String estado,
                            @RequestParam(required = false, defaultValue = "") String q,
                            Model modelo) {
        modelo.addAttribute("estadoFiltro", estado);
        modelo.addAttribute("busqueda", q);
        try {
            var pedidos = servicio.listarPedidos();
            modelo.addAttribute("totalPedidos", pedidos.size());
            modelo.addAttribute("pedidos", analitica.filtrar(pedidos, estado, q));
        } catch (IllegalArgumentException ex) {
            modelo.addAttribute("pedidos", List.of());
            modelo.addAttribute("totalPedidos", 0);
            modelo.addAttribute("errorFiltro", ex.getMessage());
        } catch (SQLException | IllegalStateException ex) {
            modelo.addAttribute("pedidos", List.of());
            modelo.addAttribute("totalPedidos", 0);
            modelo.addAttribute("errorConexion", ERROR_CONEXION);
        }
        return "historial";
    }

    @GetMapping("/dashboard")
    public String dashboard(Model modelo) {
        try {
            List<Pedido> pedidos = servicio.listarPedidos();
            modelo.addAttribute("resumen", analitica.resumir(pedidos));
        } catch (SQLException | IllegalStateException ex) {
            modelo.addAttribute("resumen", analitica.resumir(List.of()));
            modelo.addAttribute("errorConexion", ERROR_CONEXION);
        }
        return "dashboard";
    }
}
