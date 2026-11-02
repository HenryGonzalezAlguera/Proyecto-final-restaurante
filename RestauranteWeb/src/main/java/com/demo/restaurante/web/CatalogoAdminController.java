package com.demo.restaurante.web;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.NoSuchElementException;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Administración de catálogo exclusiva de la demostración local.
 * Antes de publicar la app, implementar autenticación, autorización y CSRF.
 */
@Controller
public class CatalogoAdminController {
    private final ServicioRestauranteWeb servicio;
    private final AuditoriaRestauranteService auditoria;

    public CatalogoAdminController(ServicioRestauranteWeb servicio,
                                   AuditoriaRestauranteService auditoria) {
        this.servicio = servicio;
        this.auditoria = auditoria;
    }

    @PostMapping("/catalogo/nuevo")
    public String crear(@RequestParam(required = false) String nombre,
                        @RequestParam(required = false) String costo,
                        @RequestParam(required = false) String precio,
                        RedirectAttributes mensajes) {
        try {
            var creado = servicio.crearPlatillo(nombre, monto(costo), monto(precio));
            mensajes.addFlashAttribute("mensaje",
                    "Platillo #" + creado.getId() + " agregado al catálogo.");
            registrarAuditoria(mensajes, AuditoriaRestauranteService.Accion.PLATILLO_CREADO,
                    creado.getId(), "ACTIVO");
        } catch (SQLException error) {
            mensajes.addFlashAttribute("error", errorSql());
        } catch (IllegalArgumentException | IllegalStateException error) {
            mensajes.addFlashAttribute("error", error.getMessage());
        }
        return "redirect:/catalogo";
    }

    @PostMapping("/catalogo/{id}/editar")
    public String editar(@PathVariable int id,
                         @RequestParam(required = false) String nombre,
                         @RequestParam(required = false) String costo,
                         @RequestParam(required = false) String precio,
                         RedirectAttributes mensajes) {
        try {
            servicio.actualizarPlatillo(id, nombre, monto(costo), monto(precio));
            mensajes.addFlashAttribute("mensaje", "Platillo #" + id + " actualizado.");
            registrarAuditoria(mensajes, AuditoriaRestauranteService.Accion.PLATILLO_EDITADO,
                    id, "");
        } catch (SQLException error) {
            mensajes.addFlashAttribute("error", errorSql());
        } catch (NoSuchElementException error) {
            mensajes.addFlashAttribute("error", "El platillo solicitado no existe.");
        } catch (IllegalArgumentException | IllegalStateException error) {
            mensajes.addFlashAttribute("error", error.getMessage());
        }
        return "redirect:/catalogo";
    }

    @PostMapping("/catalogo/{id}/disponibilidad")
    public String disponibilidad(@PathVariable int id,
                                @RequestParam(required = false) String accion,
                                RedirectAttributes mensajes) {
        if (!"activar".equals(accion) && !"desactivar".equals(accion)) {
            mensajes.addFlashAttribute("error", "Acción de disponibilidad inválida.");
            return "redirect:/catalogo";
        }
        boolean activo = "activar".equals(accion);
        try {
            servicio.cambiarDisponibilidadPlatillo(id, activo);
            mensajes.addFlashAttribute("mensaje",
                    "Platillo #" + id + (activo ? " activado." : " desactivado."));
            registrarAuditoria(mensajes,
                    AuditoriaRestauranteService.Accion.PLATILLO_DISPONIBILIDAD,
                    id, activo ? "ACTIVO" : "INACTIVO");
        } catch (SQLException error) {
            mensajes.addFlashAttribute("error", errorSql());
        } catch (NoSuchElementException error) {
            mensajes.addFlashAttribute("error", "El platillo solicitado no existe.");
        } catch (IllegalArgumentException | IllegalStateException error) {
            mensajes.addFlashAttribute("error", error.getMessage());
        }
        return "redirect:/catalogo";
    }

    private void registrarAuditoria(RedirectAttributes mensajes,
                                   AuditoriaRestauranteService.Accion accion,
                                   int id, String detalle) {
        if (!auditoria.registrar(accion, String.valueOf(id), detalle)) {
            mensajes.addFlashAttribute("advertenciaAuditoria",
                    "El cambio se realizó, pero no se pudo guardar la auditoría.");
        }
    }

    private static BigDecimal monto(String valor) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException("El costo y el precio son obligatorios.");
        }
        try {
            return new BigDecimal(valor.trim());
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Introduce montos numéricos válidos.");
        }
    }

    private static String errorSql() {
        return "No se pudo guardar el cambio en MySQL. Comprueba los permisos "
                + "y que ejecutaste la migración 03 del catálogo.";
    }
}
