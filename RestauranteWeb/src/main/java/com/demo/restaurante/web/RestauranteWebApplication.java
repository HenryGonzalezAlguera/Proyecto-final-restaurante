package com.demo.restaurante.web;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Interfaz Spring MVC: comparte el módulo de dominio y JDBC de Restaurante.
 */
@SpringBootApplication
public class RestauranteWebApplication {
    public static void main(String[] args) {
        SpringApplication.run(RestauranteWebApplication.class, args);
    }
}
