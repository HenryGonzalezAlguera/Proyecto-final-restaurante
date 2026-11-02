package com.demo.restaurante.web;

import java.sql.SQLException;
import java.util.List;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/** Historial de eventos visible únicamente para ADMIN por Spring Security. */
@Controller
public class AuditoriaRestauranteController {
    private final AuditoriaRestauranteService auditoria;

    public AuditoriaRestauranteController(AuditoriaRestauranteService auditoria) {
        this.auditoria = auditoria;
    }

    @GetMapping("/auditoria")
    public String listado(@RequestParam(required = false) Integer pagina, Model modelo) {
        try {
            modelo.addAttribute("registro", auditoria.consultar(pagina));
        } catch (SQLException | IllegalStateException ex) {
            modelo.addAttribute("registro",
                    new AuditoriaRestauranteService.Pagina(List.of(), 0, 0, false, false));
            modelo.addAttribute("errorConexion",
                    "No se pudo leer la auditoría. Comprueba la migración 05 y permisos MySQL.");
        }
        return "auditoria";
    }
}
