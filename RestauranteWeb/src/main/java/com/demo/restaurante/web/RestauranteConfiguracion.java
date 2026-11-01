package com.demo.restaurante.web;

import com.demo.restaurante.ConexionRestaurante;
import com.demo.restaurante.RepositorioRestauranteJdbc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Inyección de dependencias sin replicar las reglas de dominio ni escribir secretos.
 */
@Configuration
public class RestauranteConfiguracion {
    @Bean
    RepositorioRestauranteJdbc repositorioRestauranteJdbc() {
        return new RepositorioRestauranteJdbc(ConexionRestaurante::desdeEntorno);
    }
}
