package com.demo.restaurante;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Configuración JDBC exclusiva por variables de entorno.
 * No almacena ni muestra credenciales.
 */
public final class ConexionRestaurante {

    private ConexionRestaurante() {
    }

    public static Connection desdeEntorno() throws SQLException {
        String url = requerida("RESTAURANTE_DB_URL");
        String usuario = requerida("RESTAURANTE_DB_USER");
        String clave = requerida("RESTAURANTE_DB_PASSWORD");
        if (!url.startsWith("jdbc:mysql://")) {
            throw new IllegalArgumentException("RESTAURANTE_DB_URL debe ser una URL JDBC de MySQL.");
        }
        return DriverManager.getConnection(url, usuario, clave);
    }

    private static String requerida(String nombre) {
        String valor = System.getenv(nombre);
        if (valor == null || valor.isBlank()) {
            throw new IllegalStateException("Falta la variable de entorno " + nombre + ".");
        }
        return valor;
    }
}
