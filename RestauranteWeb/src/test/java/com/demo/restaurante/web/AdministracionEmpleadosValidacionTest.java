package com.demo.restaurante.web;

import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

class AdministracionEmpleadosValidacionTest {
    private final AdministracionEmpleadosService servicio =
            new AdministracionEmpleadosService(new BCryptPasswordEncoder());

    @Test
    void rechazaUsuarioInvalidoAntesDeAbrirConexion() {
        assertThrows(IllegalArgumentException.class,
                () -> servicio.crearEmpleado("../invalido", "ClaveTemporalSegura123!"));
    }

    @Test
    void rechazaClaveDebilAntesDeAbrirConexion() {
        assertThrows(IllegalArgumentException.class,
                () -> servicio.crearEmpleado("empleado2", "123"));
    }

    @Test
    void rechazaClavesQueExcedenElLimiteDeBytesDeBcrypt() {
        assertThrows(IllegalArgumentException.class,
                () -> servicio.crearEmpleado("empleado2", "á".repeat(40)));
    }
    @Test
    void noPermiteIdsInvalidosEnCambiosDeEstado() {
        assertThrows(IllegalArgumentException.class,
                () -> servicio.cambiarEstadoEmpleado(0, false));
    }

    @Test
    void noPermiteClavesDistintasODebilesAlRestablecer() {
        assertThrows(IllegalArgumentException.class,
                () -> servicio.restablecerClaveEmpleado(5, "ClaveTemporalLarga2026!", "otra"));
        assertThrows(IllegalArgumentException.class,
                () -> servicio.restablecerClaveEmpleado(5, "corta", "corta"));
    }

}
