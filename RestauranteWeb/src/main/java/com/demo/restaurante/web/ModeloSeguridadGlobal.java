package com.demo.restaurante.web;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/** Facilita la visualización condicional sin sustituir la autorización del servidor. */
@ControllerAdvice
public class ModeloSeguridadGlobal {
    @ModelAttribute("esAdmin")
    public boolean esAdmin(HttpServletRequest request) {
        return request.isUserInRole("ADMIN");
    }
}
