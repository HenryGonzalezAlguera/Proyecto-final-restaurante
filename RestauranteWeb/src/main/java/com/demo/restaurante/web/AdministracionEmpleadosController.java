package com.demo.restaurante.web;

import java.sql.SQLException;
import java.util.List;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Administración local de cuentas de empleados, autorizada por Spring Security. */
@Controller
public class AdministracionEmpleadosController {
    private static final String ERROR_DB =
            "No se pudo acceder a los usuarios. Revisa la conexión MySQL y la migración 04.";
    private final AdministracionEmpleadosService servicio;
    private final AuditoriaRestauranteService auditoria;

    public AdministracionEmpleadosController(AdministracionEmpleadosService servicio,
                                             AuditoriaRestauranteService auditoria) {
        this.servicio = servicio;
        this.auditoria = auditoria;
    }

    @GetMapping("/usuarios")
    public String listado(Model modelo) {
        try {
            var registrados = servicio.listarEmpleados();
            modelo.addAttribute("empleados", registrados);
            modelo.addAttribute("empleadosActivos", registrados.stream()
                    .filter(AdministracionEmpleadosService.EmpleadoResumen::activo).count());
        } catch (SQLException | IllegalStateException ex) {
            modelo.addAttribute("empleados", List.of());
            modelo.addAttribute("empleadosActivos", 0L);
            modelo.addAttribute("errorConexion", ERROR_DB);
        }
        return "usuarios";
    }

    @PostMapping("/usuarios/empleados")
    public String crear(@RequestParam(required = false) String usuario,
                        @RequestParam(required = false) String password,
                        RedirectAttributes mensajes) {
        try {
            servicio.crearEmpleado(usuario, password);
            mensajes.addFlashAttribute("mensaje", "Cuenta de empleado creada correctamente.");
            registrarAuditoria(mensajes, AuditoriaRestauranteService.Accion.EMPLEADO_CREADO,
                    usuario.trim(), "ACTIVO");
        } catch (IllegalArgumentException ex) {
            mensajes.addFlashAttribute("error", ex.getMessage());
        } catch (SQLException | IllegalStateException ex) {
            mensajes.addFlashAttribute("error", ERROR_DB);
        }
        return "redirect:/usuarios";
    }
    @PostMapping("/usuarios/empleados/{id}/estado")
    public String estado(@PathVariable int id,
                         @RequestParam(required = false) String accion,
                         RedirectAttributes mensajes) {
        if (!"activar".equals(accion) && !"desactivar".equals(accion)) {
            mensajes.addFlashAttribute("error", "Acción de estado inválida.");
            return "redirect:/usuarios";
        }
        try {
            servicio.cambiarEstadoEmpleado(id, "activar".equals(accion));
            mensajes.addFlashAttribute("mensaje",
                    "activar".equals(accion) ? "Empleado activado." : "Empleado desactivado.");
            registrarAuditoria(mensajes, AuditoriaRestauranteService.Accion.EMPLEADO_ESTADO,
                    String.valueOf(id), "activar".equals(accion) ? "ACTIVO" : "INACTIVO");
        } catch (IllegalArgumentException ex) {
            mensajes.addFlashAttribute("error", ex.getMessage());
        } catch (SQLException | IllegalStateException ex) {
            mensajes.addFlashAttribute("error", ERROR_DB);
        }
        return "redirect:/usuarios";
    }

    @PostMapping("/usuarios/empleados/{id}/clave")
    public String clave(@PathVariable int id,
                        @RequestParam(required = false) String password,
                        @RequestParam(required = false) String confirmacion,
                        RedirectAttributes mensajes) {
        try {
            servicio.restablecerClaveEmpleado(id, password, confirmacion);
            mensajes.addFlashAttribute("mensaje",
                    "Contraseña actualizada. Las sesiones anteriores del empleado han sido revocadas.");
            registrarAuditoria(mensajes, AuditoriaRestauranteService.Accion.EMPLEADO_CLAVE,
                    String.valueOf(id), "");
        } catch (IllegalArgumentException ex) {
            mensajes.addFlashAttribute("error", ex.getMessage());
        } catch (SQLException | IllegalStateException ex) {
            mensajes.addFlashAttribute("error", ERROR_DB);
        }
        return "redirect:/usuarios";
    }

    private void registrarAuditoria(RedirectAttributes mensajes,
                                   AuditoriaRestauranteService.Accion accion,
                                   String referencia, String detalle) {
        if (!auditoria.registrar(accion, referencia, detalle)) {
            mensajes.addFlashAttribute("advertenciaAuditoria",
                    "El cambio se realizó, pero no se pudo guardar la auditoría.");
        }
    }

}
